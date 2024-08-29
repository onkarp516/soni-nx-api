package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.*;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageProperties;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageService;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.EmployeeDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.EmployeeDTO;
import com.truethic.soninx.SoniNxAPI.dto.EmployeeSalaryDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
public class EmployeeService implements UserDetailsService {
    @Autowired
    EmployeeRepository employeeRepository;
    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    EmployeeDocumentRepository employeeDocumentRepository;
    @Autowired
    EmployeeFamilyRepository employeeFamilyRepository;
    @Autowired
    EmployeeEducationRepository employeeEducationRepository;
    @Autowired
    EmployeeExperienceRepository employeeExperienceRepository;
    @Autowired
    EmployeeReferenceRepository employeeReferenceRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private EmployeeSalaryRepository employeeSalaryRepository;
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private DesignationRepository designationRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private OTPRepository otpRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private Utility utility;
    @Autowired
    private AttendanceRepository attendanceRepository;

    public Object createEmployee(MultipartHttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        try {
            Employee emp = employeeRepository.findByMobileNumber(Long.parseLong(request.getParameter("mobileNumber")));
            if (emp != null) {
                responseObject.setResponseStatus(HttpStatus.NOT_ACCEPTABLE.value());
                responseObject.setMessage("This mobile number is already registered");
                return responseObject;
            }
            Employee employee = new Employee();
            employee.setFirstName(request.getParameter("firstName"));
            employee.setMiddleName(request.getParameter("middleName"));
            employee.setLastName(request.getParameter("lastName"));
            employee.setFullName(request.getParameter("fullName"));
            employee.setAddress(request.getParameter("fullAddress"));
            employee.setMobileNumber(Long.parseLong(request.getParameter("mobileNumber")));
            employee.setDob(LocalDate.parse(request.getParameter("dob")));
            employee.setAge(Integer.parseInt(request.getParameter("age")));
            employee.setReligion(request.getParameter("religion"));
            employee.setCast(request.getParameter("cast"));
            employee.setReasonToJoin(request.getParameter("reasonToJoin"));
            employee.setMarriageStatus(request.getParameter("marriageStatus"));
            if (request.getParameter("height") != null && !request.getParameter("height").equalsIgnoreCase("NA"))
                employee.setHeight(Double.parseDouble(request.getParameter("height")));
            if (request.getParameter("weight") != null && !request.getParameter("weight").equalsIgnoreCase("NA"))
                employee.setWeight(Double.parseDouble(request.getParameter("weight")));
            employee.setBloodGroup(request.getParameter("bloodGroup"));
            employee.setIsSpecks(Boolean.parseBoolean(request.getParameter("isSpecks")));
            employee.setHasOwnMobileDevice(Boolean.parseBoolean(request.getParameter("hasOwnMobileDevice")));
            employee.setEmployeeType(request.getParameter("employeeType"));

            employee.setIsExperienceEmployee(request.getParameter("isExperienceEmployee").equalsIgnoreCase("true"));

            if (request.getParameter("isDisability").equalsIgnoreCase("true")) {
                employee.setIsDisability(true);
                employee.setDisabilityDetails(request.getParameter("disabilityDetails"));
            } else {
                employee.setIsDisability(false);
                employee.setDisabilityDetails("");
            }

            if (request.getParameter("isInjured").equalsIgnoreCase("true")) {
                employee.setIsInjured(true);
                employee.setInjureDetails(request.getParameter("injureDetails"));
            } else {
                employee.setIsInjured(false);
                employee.setInjureDetails("");
            }
            employee.setHobbies(request.getParameter("hobbies"));
            employee.setCanWeContactPreviousCompany(Boolean.parseBoolean(request.getParameter("canWeContactPreviousCompany")));
            employee.setExpectedSalary(request.getParameter("expectedSalary").equalsIgnoreCase("NA") ? 0 : Double.valueOf(request.getParameter("expectedSalary")));
            employee.setWagesPerDay(Double.valueOf(request.getParameter("wagesPerDay")));
            employee.setDoj(LocalDate.parse(request.getParameter("doj")));
            employee.setReadyToWorkInThreeShift(Boolean.parseBoolean(request.getParameter("readyToWorkInThreeShift")));
            employee.setReadyToWorkInMonths(request.getParameter("readyToWorkInMonths"));
            employee.setBankName(request.getParameter("bankName"));
            employee.setBranchName(request.getParameter("branchName"));
            employee.setAccountNo(request.getParameter("accountNo"));
            employee.setIfscCode(request.getParameter("ifscCode"));
            employee.setPfNumber(request.getParameter("pfNumber"));
            employee.setEsiNumber(request.getParameter("esiNumber"));
            employee.setPanNumber(request.getParameter("panNumber"));

            if(request.getParameterMap().containsKey("employeeHavePf")){
                employee.setEmployeeHavePf(Boolean.parseBoolean(request.getParameter("employeeHavePf")));
                if (Boolean.parseBoolean(request.getParameter("employeeHavePf"))) {
                    employee.setEmployeePf(Double.valueOf(request.getParameter("employeePf")));
                }
            }

            if(request.getParameterMap().containsKey("employeeHaveEsi")) {
                employee.setEmployeeHaveEsi(Boolean.parseBoolean(request.getParameter("employeeHaveEsi")));
                if (Boolean.parseBoolean(request.getParameter("employeeHaveEsi"))) {
                    employee.setEmployeeEsi(Double.valueOf(request.getParameter("employeeEsi")));
                }
            }

            if(request.getParameterMap().containsKey("employeeHaveProfTax"))
                employee.setEmployeeHaveProfTax(Boolean.parseBoolean(request.getParameter("employeeHaveProfTax")));
            if(request.getParameterMap().containsKey("showSalarySheet"))
                employee.setShowSalarySheet(Boolean.parseBoolean(request.getParameter("showSalarySheet")));

            employee.setGender(request.getParameter("gender"));
            employee.setPoliceCaseDetails(request.getParameter("policeCaseDetails"));
            employee.setStatus(true);

            employee.setTextPassword("1234");
            String encPassword = passwordEncoder.encode("1234");
            employee.setPassword(encPassword);

            employee.setWagesOptions(request.getParameter("wagesOptions"));
            employee.setEmployeeWagesType(request.getParameter("employeeWagesType"));
            employee.setWeeklyOffDay(request.getParameter("weeklyOffDay"));

            Designation designation = designationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("designationId")), true);
            if (designation != null) {
                employee.setDesignation(designation);
            }

            if (request.getParameter("shiftId") != null && !request.getParameter("shiftId").equalsIgnoreCase("undefined")) {
                Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(request.getParameter("shiftId")), true);
                if (shift != null) {
                    employee.setShift(shift);
                }
            }

            if (request.getParameter("companyId") != null) {
                Company company = companyRepository.findByIdAndStatus(Long.parseLong(request.getParameter("companyId")), true);
                if (company != null) {
                    employee.setCompany(company);
                }
            }

            if (request.getParameter("siteId") != null) {
                Site site = siteRepository.findByIdAndStatus(Long.parseLong(request.getParameter("siteId")), true);
                if (site != null) {
                    employee.setSite(site);
                }
            }

            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employee.setCreatedBy(user.getId());
            employee.setInstitute(user.getInstitute());
            try {
                List<EmployeeFamily> employeeFamilyList = new ArrayList<>();
                String jsonToStr = request.getParameter("family");
                JsonArray array = new JsonParser().parse(jsonToStr).getAsJsonArray();
                for (JsonElement jsonElement1 : array) {
                    JsonObject jsonObject = jsonElement1.getAsJsonObject();
                    if (jsonObject.get("fullName").getAsString() != null) {
                        EmployeeFamily employeeFamily = this.getEmployeeFamilyFromJsonObject(jsonObject, user);
                        employeeFamilyList.add(employeeFamily);
                    }
                }


                List<EmployeeEducation> employeeEducationList = new ArrayList<>();
                String jsonToStr1 = request.getParameter("education");
                JsonArray educationArray = new JsonParser().parse(jsonToStr1).getAsJsonArray();
                for (JsonElement jsonElement : educationArray) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    if (object.get("schoolName").getAsString() != null) {
                        EmployeeEducation employeeEducation = this.getEmployeeEducationFromJsonObject(object, user);
                        employeeEducationList.add(employeeEducation);
                    }
                }

                List<EmployeeDocument> employeeDocumentList = new ArrayList<>();
                String jsonToStr2 = request.getParameter("document");
                JsonArray documentArray = new JsonParser().parse(jsonToStr2).getAsJsonArray();
                for (int i = 0; i < documentArray.size(); i++) {
                    JsonObject object = documentArray.get(i).getAsJsonObject();
                    if (object.get("d_documentId").getAsJsonObject() != null) {
                        EmployeeDocument employeeDocument = new EmployeeDocument();
                        JsonObject docObject = object.get("d_documentId").getAsJsonObject();
                        Document document = documentRepository.findByIdAndStatus(Long.parseLong(docObject.get("value").getAsString()), true);
                        employeeDocument.setDocument(document);

                        /*if (request.getFile("document" + i) != null) {
                            String imagePath = uploadDocumentImage(request.getFile("document" + i));
                            String[] arr = imagePath.split("#");
                            if (imagePath != null) {
                                employeeDocument.setImagePath(awss3Service.getBASE_URL() + arr[0]);
                                employeeDocument.setImageKey(arr[1]);
                            } else {
                                responseObject.setMessage("Image uploading error");
                                responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                                return responseObject;
                            }
                        } else {
                            responseObject.setMessage("Please upload document");
                            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                            return responseObject;
                        }*/

                        if (request.getFile("document" + i) != null) {
                            MultipartFile image = request.getFile("document" + i);
                            fileStorageProperties.setUploadDir("./uploads" + File.separator + "emp_documents" + File.separator);
                            String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                            if (imagePath != null) {
                                employeeDocument.setImagePath("/uploads" + File.separator + "emp_documents" + File.separator + imagePath);
                            } else {
                                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                responseObject.setMessage("Failed to upload documents. Please try again!");
                                return responseObject;
                            }
                        } else {
                            responseObject.setMessage("Please upload document");
                            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                            return responseObject;
                        }

                        employeeDocument.setCreatedBy(user.getId());
                        employeeDocument.setStatus(true);
                        employeeDocument.setInstitute(user.getInstitute());
                        employeeDocumentList.add(employeeDocument);
                    }
                }

                List<EmployeeExperienceDetails> experienceDetailsList = new ArrayList<>();
                String jsonToStr3 = request.getParameter("experience");
                JsonArray experienceArray = new JsonParser().parse(jsonToStr3).getAsJsonArray();
                for (JsonElement jsonElement : experienceArray) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    if (object.get("companyName").getAsString() != null) {
                        EmployeeExperienceDetails employeeExperienceDetails = this.getEmployeeExperienceFromJsonObject(object, user);
                        experienceDetailsList.add(employeeExperienceDetails);
                    }
                }

                List<EmployeeReference> employeeReferenceList = new ArrayList<>();
                String jsonToStr4 = request.getParameter("reference");
                JsonArray jsonArray = new JsonParser().parse(jsonToStr4).getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    if (object.get("name").getAsString() != null) {
                        EmployeeReference employeeReference = this.getEmployeeReferenceFromJsonObject(object, user);
                        employeeReferenceList.add(employeeReference);
                    }
                }

                try {
                    employee.setEmployeeFamily(employeeFamilyList);
                    employee.setEmployeeEducation(employeeEducationList);
                    employee.setEmployeeDocuments(employeeDocumentList);
                    employee.setEmployeeReferences(employeeReferenceList);
                    employee.setEmployeeExperienceDetails(experienceDetailsList);

                    entityManager.persist(employee);
                    Employee employee1 = employeeRepository.save(employee);
                    List<EmployeeSalary> employeeSalaryList = new ArrayList<>();
                    String jsonToEmpSalary = request.getParameter("salaryList");
                    JsonArray array5 = new JsonParser().parse(jsonToEmpSalary).getAsJsonArray();
                    for (JsonElement jsonElement : array5) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("effectiveDate") && object.get("effectiveDate").getAsString() != null) {
                            EmployeeSalary employeeSalary = this.getEmployeeSalaryFromJsonObject(object, user, employee1);
                            employeeSalaryList.add(employeeSalary);
                        }
                    }
                    employee1.setEmployeeSalaries(employeeSalaryList);

                    responseObject.setMessage("Employee added successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(" e " + e.getMessage());
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                }

            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Internal Server Error");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    private EmployeeReference getEmployeeReferenceFromJsonObject(JsonObject object, Users user) {
        EmployeeReference employeeReference = new EmployeeReference();
        employeeReference.setName(object.get("name").getAsString());
        employeeReference.setAddress(object.get("address").getAsString());
        employeeReference.setBusiness(object.get("business").getAsString());
        employeeReference.setMobileNumber(object.get("mobileNumber").getAsString());
        employeeReference.setKnownFromWhen(object.get("knownFromWhen").getAsString());
        employeeReference.setCreatedBy(user.getId());
        employeeReference.setInstitute(user.getInstitute());
        employeeReference.setStatus(true);
        return employeeReference;
    }

    private EmployeeExperienceDetails getEmployeeExperienceFromJsonObject(JsonObject object, Users user) {
        EmployeeExperienceDetails employeeExperienceDetails = new EmployeeExperienceDetails();
        employeeExperienceDetails.setCompanyName(object.get("companyName").getAsString());
        employeeExperienceDetails.setDuration(object.get("duration").getAsString());
        employeeExperienceDetails.setDesignationName(object.get("designationName").getAsString());
        employeeExperienceDetails.setIncomePerMonth(object.get("incomePerMonth").getAsString());
        employeeExperienceDetails.setReasonToResign(object.get("reasonToResign").getAsString());
        employeeExperienceDetails.setCreatedBy(user.getId());
        employeeExperienceDetails.setInstitute(user.getInstitute());
        employeeExperienceDetails.setStatus(true);
        return employeeExperienceDetails;
    }

    private EmployeeEducation getEmployeeEducationFromJsonObject(JsonObject object, Users user) {
        EmployeeEducation employeeEducation = new EmployeeEducation();
        employeeEducation.setDesignationName(object.get("designationName").getAsString());
        employeeEducation.setSchoolName(object.get("schoolName").getAsString());
        employeeEducation.setYear(object.get("year").getAsString());
        employeeEducation.setGrade(object.get("grade").getAsString());
        employeeEducation.setPercentage(object.get("percentage").getAsString());
        employeeEducation.setMainSubject(object.get("mainSubject").getAsString());
        employeeEducation.setCreatedBy(user.getId());
        employeeEducation.setInstitute(user.getInstitute());
        employeeEducation.setStatus(true);
        return employeeEducation;
    }

    private EmployeeFamily getEmployeeFamilyFromJsonObject(JsonObject object, Users user) {
        EmployeeFamily employeeFamily = new EmployeeFamily();
        employeeFamily.setFullName(object.get("fullName").getAsString());
        employeeFamily.setAge(object.get("age").getAsString());
        employeeFamily.setRelation(object.get("relation").getAsString());
        employeeFamily.setEducation(object.get("education").getAsString());
        employeeFamily.setBusiness(object.get("business").getAsString());
        employeeFamily.setIncomePerMonth(object.get("incomePerMonth").getAsString());
        employeeFamily.setCreatedBy(user.getId());
        employeeFamily.setInstitute(user.getInstitute());
        employeeFamily.setStatus(true);
        return employeeFamily;
    }

    private String uploadDocumentImage(MultipartFile multipartFile) {
        String dir = null;
        String imagePath = null;
        dir = "emp_document";
        // imagePath = awss3Service.uploadFile(multipartFile, dir);
        return imagePath;
    }

    public Object DTEmployee(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String empStatus = request.get("empStatus");
        String selectedShift = request.get("selectedShift");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Employee> employees = new ArrayList<>();
        List<EmployeeDTDTO> employeeDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT employee_tbl.*, designation_tbl.name as desig_name, shift_tbl.name as shift_name "
                    + "FROM `employee_tbl` LEFT JOIN designation_tbl ON employee_tbl.designation_id=designation_tbl.id "
                    + "LEFT JOIN shift_tbl ON employee_tbl.shift_id=shift_tbl.id WHERE employee_tbl.institute_id="+user.getInstitute().getId();

            if (selectedShift.equalsIgnoreCase("") && !empStatus.equalsIgnoreCase("")) {
                query = query + " AND employee_tbl.status=" + Integer.parseInt(empStatus) + " ";
            }

            if (!selectedShift.equalsIgnoreCase("")) {
                query = "SELECT employee_tbl.*, designation_tbl.name as desig_name, shift_tbl.name as shift_name "
                        + "FROM `employee_tbl` LEFT JOIN designation_tbl ON employee_tbl.designation_id=designation_tbl.id "
                        + "LEFT JOIN shift_tbl ON employee_tbl.shift_id=shift_tbl.id "
                        + "WHERE employee_tbl.shift_id='" + selectedShift + "' AND employee_tbl.institute_id="+user.getInstitute().getId();
            }
            if (!selectedShift.equalsIgnoreCase("") && !empStatus.equalsIgnoreCase("")) {
                query = query + "  AND employee_tbl.status=" + Integer.parseInt(empStatus) + " ";
            }


            if (!searchText.equalsIgnoreCase("")) {
                if (!selectedShift.equalsIgnoreCase(""))
                    query = query + " AND";
                else if (!empStatus.equalsIgnoreCase(""))
                    query = query + " AND";
                else
//                    query = query + " WHERE employee_tbl.institute_id="+user.getInstitute().getId()+" AND";
                query = query + " AND (first_name LIKE '%" + searchText + "%' OR middle_name LIKE '%" + searchText
                        + "%' OR last_name LIKE '%" + searchText + "%' OR dob LIKE '%" + searchText + "%' OR gender LIKE '%"
                        + searchText + "%'  OR mobile_number LIKE '%" + searchText + "%' OR employee_tbl.created_at LIKE '%"
                        + searchText + "%'  OR designation_tbl.name LIKE '%" + searchText + "%' OR shift_tbl.name LIKE '%" + searchText + "%')";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") && jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY employee_tbl.id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, Employee.class);
            Query q1 = entityManager.createNativeQuery(query1, Employee.class);

            employees = q.getResultList();
            System.out.println("Limit total rows " + employees.size());
            if (employees.size() > 0) {
                for (Employee employee : employees) {
                    employeeDTDTOList.add(convertToDTDTO(employee));
                }
            }

            List<Employee> employeeArrayList = new ArrayList<>();
            employeeArrayList = q1.getResultList();
            System.out.println("total rows " + employeeArrayList.size());

            genericDTData.setRows(employeeDTDTOList);
            genericDTData.setTotalRows(employeeArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(employeeDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private EmployeeDTDTO convertToDTDTO(Employee employee) {
        EmployeeDTDTO employeeDTDTO = new EmployeeDTDTO();
        employeeDTDTO.setEmployeeId(employee.getId());
//        employeeDTDTO.setFirstName(employee.getFirstName());
//        employeeDTDTO.setMiddleName(employee.getMiddleName());
//        employeeDTDTO.setLastName(employee.getLastName());
        employeeDTDTO.setFullName(utility.getEmployeeName(employee));
        employeeDTDTO.setDob(String.valueOf(employee.getDob()));
        employeeDTDTO.setGender(employee.getGender());
        employeeDTDTO.setMobileNumber(employee.getMobileNumber());
        employeeDTDTO.setEmployeeType(employee.getEmployeeType());
        employeeDTDTO.setCreatedAt(String.valueOf(employee.getCreatedAt()));
        employeeDTDTO.setStatus(employee.getStatus());

        Double empPerDaySal = utility.getEmployeeWages(employee.getId());
        double perDaySal = 0;
        if (empPerDaySal != null) {
            perDaySal = empPerDaySal;
        }
        employeeDTDTO.setWagesPerDay(perDaySal);
        employeeDTDTO.setDesigName(employee.getDesignation().getName());
        employeeDTDTO.setShiftName(employee.getShift() != null ? employee.getShift().getName() : "");
        employeeDTDTO.setCompanyName(employee.getCompany() != null ? employee.getCompany().getCompanyName() : "");
        return employeeDTDTO;
    }

    public Object findEmployee(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Employee employee = employeeRepository.findById(Long.parseLong(request.get("id"))).get();
            EmployeeDTO employeeDTO = new EmployeeDTO();
            if (employee != null) {
                employeeDTO = convertEmployeeToEmployeeDTO(employee);
                responseMessage.setResponse(employeeDTO);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    private EmployeeDTO convertEmployeeToEmployeeDTO(Employee employee) {
        EmployeeDTO employeeDTO = new EmployeeDTO();
        employeeDTO.setId(employee.getId());
        employeeDTO.setFirstName(employee.getFirstName());
        employeeDTO.setMiddleName(employee.getMiddleName());
        employeeDTO.setLastName(employee.getLastName());
        employeeDTO.setHasOwnMobileDevice(employee.getHasOwnMobileDevice());
        employeeDTO.setFullName(utility.getEmployeeName(employee));

        if (employee.getDob() != null) employeeDTO.setDob(String.valueOf(employee.getDob()));
        employeeDTO.setGender(employee.getGender());
        employeeDTO.setMobileNumber(employee.getMobileNumber());
        employeeDTO.setEmployeeType(employee.getEmployeeType());
        employeeDTO.setCreatedAt(String.valueOf(employee.getCreatedAt()));
        employeeDTO.setStatus(employee.getStatus());
        employeeDTO.setDesignation(employee.getDesignation());
        employeeDTO.setShift(employee.getShift() != null ? employee.getShift() : null);
        employeeDTO.setCompany(employee.getCompany() != null ? employee.getCompany() : null);
        employeeDTO.setSite(employee.getSite() != null ? employee.getSite() : null);

        employeeDTO.setWagesOptions(employee.getWagesOptions());
        employeeDTO.setEmployeeWagesType(employee.getEmployeeWagesType());
        employeeDTO.setWeeklyOffDay(employee.getWeeklyOffDay());
        employeeDTO.setAddress(employee.getAddress());
        employeeDTO.setCast(employee.getCast());
        employeeDTO.setReasonToJoin(employee.getReasonToJoin());
        employeeDTO.setAge(employee.getAge());
        employeeDTO.setReligion(employee.getReligion());
        employeeDTO.setMarriageStatus(employee.getMarriageStatus());
        employeeDTO.setHeight(employee.getHeight());
        employeeDTO.setWeight(employee.getWeight());
        employeeDTO.setBloodGroup(employee.getBloodGroup());
        employeeDTO.setIsSpecks(employee.getIsSpecks());
        employeeDTO.setIsDisability(employee.getIsDisability());
        employeeDTO.setDisabilityDetails(employee.getDisabilityDetails());
        employeeDTO.setIsInjured(employee.getIsInjured());
        employeeDTO.setInjureDetails(employee.getInjureDetails());

        Double empPerDaySal = utility.getEmployeeWages(employee.getId());
        double perDaySal = 0;
        if (empPerDaySal != null) {
            perDaySal = empPerDaySal;
        }
        employeeDTO.setWagesPerDay(perDaySal);
        employeeDTO.setEmployeeHavePf(employee.getEmployeeHavePf());
        employeeDTO.setEmployerPf(employee.getEmployerPf());
        employeeDTO.setEmployeePf(employee.getEmployeePf());
        employeeDTO.setEmployeeHaveEsi(employee.getEmployeeHaveEsi());
        employeeDTO.setEmployerEsi(employee.getEmployerEsi());
        employeeDTO.setEmployeeEsi(employee.getEmployeeEsi());
        employeeDTO.setEmployeeHaveProfTax(employee.getEmployeeHaveProfTax());
        employeeDTO.setShowSalarySheet(employee.getShowSalarySheet());

        employeeDTO.setEmployeeFamily(employee.getEmployeeFamily());
        employeeDTO.setEmployeeEducation(employee.getEmployeeEducation());
        employeeDTO.setEmployeeExperienceDetails(employee.getEmployeeExperienceDetails());
        employeeDTO.setEmployeeDocuments(employee.getEmployeeDocuments());
        employeeDTO.setEmployeeReferences(employee.getEmployeeReferences());

        List<EmployeeSalaryDTO> employeeSalaryDTOS = new ArrayList<>();
        for (EmployeeSalary employeeSalary : employee.getEmployeeSalaries()) {
            employeeSalaryDTOS.add(convertEmpSalToSalDTO(employeeSalary));
        }
        employeeDTO.setEmployeeSalaryList(employeeSalaryDTOS);

        employeeDTO.setPoliceCaseDetails(employee.getPoliceCaseDetails());
        employeeDTO.setIsExperienceEmployee(employee.getIsExperienceEmployee());
        employeeDTO.setCanWeContactPreviousCompany(employee.getCanWeContactPreviousCompany());
        employeeDTO.setHobbies(employee.getHobbies());
        employeeDTO.setExpectedSalary(employee.getExpectedSalary());
        if (employee.getDoj() != null) employeeDTO.setDoj(String.valueOf(employee.getDoj()));
        employeeDTO.setReadyToWorkInThreeShift(employee.getReadyToWorkInThreeShift());
        employeeDTO.setReadyToWorkInMonths(employee.getReadyToWorkInMonths());
        employeeDTO.setBankName(employee.getBankName());
        employeeDTO.setBranchName(employee.getBranchName());
        employeeDTO.setAccountNo(employee.getAccountNo());
        employeeDTO.setIfscCode(employee.getIfscCode());
        employeeDTO.setPfNumber(employee.getPfNumber());
        employeeDTO.setEsiNumber(employee.getEsiNumber());
        employeeDTO.setPanNumber(employee.getPanNumber());

        return employeeDTO;
    }

    private EmployeeSalaryDTO convertEmpSalToSalDTO(EmployeeSalary employeeSalary) {
        EmployeeSalaryDTO employeeSalaryDTO = new EmployeeSalaryDTO();
        employeeSalaryDTO.setId(employeeSalary.getId());
        employeeSalaryDTO.setEmployeeId(employeeSalary.getEmployeeId());
        employeeSalaryDTO.setEffectiveDate(employeeSalary.getEffectiveDate().toString());
        employeeSalaryDTO.setSalary(employeeSalary.getSalary());
        employeeSalaryDTO.setCreatedBy(employeeSalary.getCreatedBy());
        employeeSalaryDTO.setCreatedAt(employeeSalary.getCreatedAt().toString());
        employeeSalaryDTO.setUpdatedBy(employeeSalary.getUpdatedBy());
        employeeSalaryDTO.setUpdatedAt(employeeSalary.getUpdatedAt() != null ? employeeSalary.getUpdatedAt().toString() : "");
        return employeeSalaryDTO;
    }

    public Object updateEmployee(MultipartHttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        try {
//            Employee employee = employeeRepository.findByIdAndStatus(Long.parseLong(request.getParameter("id")), true);
            Employee employee = employeeRepository.findById(Long.parseLong(request.getParameter("id"))).get();
            if (employee != null) {
                employee.setFirstName(request.getParameter("firstName"));
                employee.setMiddleName(request.getParameter("middleName"));
                employee.setLastName(request.getParameter("lastName"));
                employee.setFullName(request.getParameter("fullName"));
                employee.setAddress(request.getParameter("fullAddress"));
                employee.setMobileNumber(Long.parseLong(request.getParameter("mobileNumber")));
                employee.setDob(LocalDate.parse(request.getParameter("dob")));
                employee.setAge(Integer.parseInt(request.getParameter("age")));
                employee.setReligion(request.getParameter("religion"));
                employee.setCast(request.getParameter("cast"));
                employee.setReasonToJoin(request.getParameter("reasonToJoin"));
                employee.setMarriageStatus(request.getParameter("marriageStatus"));
                if (!request.getParameter("height").equalsIgnoreCase("null") && !request.getParameter("height").equalsIgnoreCase("NA"))
                    employee.setHeight(Double.parseDouble(request.getParameter("height")));
                if (!request.getParameter("weight").equalsIgnoreCase("null") && !request.getParameter("weight").equalsIgnoreCase("NA"))
                    employee.setWeight(Double.parseDouble(request.getParameter("weight")));
                employee.setBloodGroup(request.getParameter("bloodGroup"));
                employee.setIsSpecks(Boolean.parseBoolean(request.getParameter("isSpecks")));
                employee.setHasOwnMobileDevice(Boolean.parseBoolean(request.getParameter("hasOwnMobileDevice")));
                employee.setEmployeeType(request.getParameter("employeeType"));

                employee.setIsExperienceEmployee(request.getParameter("isExperienceEmployee").equalsIgnoreCase("true"));

                if (request.getParameter("isDisability").equalsIgnoreCase("true")) {
                    employee.setIsDisability(true);
                    employee.setDisabilityDetails(request.getParameter("disabilityDetails"));
                } else {
                    employee.setIsDisability(false);
                    employee.setDisabilityDetails("");
                }

                if (request.getParameter("isInjured").equalsIgnoreCase("true")) {
                    employee.setIsInjured(true);
                    employee.setInjureDetails(request.getParameter("injureDetails"));
                } else {
                    employee.setIsInjured(false);
                    employee.setInjureDetails("");
                }
                employee.setWagesOptions(request.getParameter("wagesOptions"));
                employee.setEmployeeWagesType(request.getParameter("employeeWagesType"));
                employee.setWeeklyOffDay(request.getParameter("weeklyOffDay"));
                employee.setHobbies(request.getParameter("hobbies"));
                employee.setCanWeContactPreviousCompany(Boolean.parseBoolean(request.getParameter("canWeContactPreviousCompany")));
                employee.setExpectedSalary(request.getParameterMap().containsKey("expectedSalary") ?
                        request.getParameter("expectedSalary").equalsIgnoreCase("NA") ? 0 : Double.valueOf(request.getParameter("expectedSalary"))
                        : 0);
                if (request.getParameterMap().containsKey("wagesPerDay") && !request.getParameter("wagesPerDay").equalsIgnoreCase("null")) {
                    employee.setWagesPerDay(Double.valueOf(request.getParameter("wagesPerDay")));
                }
                employee.setDoj(LocalDate.parse(request.getParameter("doj")));
                employee.setReadyToWorkInThreeShift(Boolean.parseBoolean(request.getParameter("readyToWorkInThreeShift")));
                employee.setReadyToWorkInMonths(request.getParameter("readyToWorkInMonths"));
                employee.setBankName(request.getParameter("bankName"));
                employee.setBranchName(request.getParameter("branchName"));
                employee.setAccountNo(request.getParameter("accountNo"));
                employee.setIfscCode(request.getParameter("ifscCode"));
                employee.setPfNumber(request.getParameter("pfNumber"));
                employee.setEsiNumber(request.getParameter("esiNumber"));
                employee.setPanNumber(request.getParameter("panNumber"));

                if (Boolean.parseBoolean(request.getParameter("employeeHavePf"))) {
//            employee.setEmployerPf(Double.valueOf(request.getParameter("employerPf")));
                    employee.setEmployeePf(Double.valueOf(request.getParameter("employeePf")));
                }

                employee.setEmployeeHaveEsi(Boolean.parseBoolean(request.getParameter("employeeHaveEsi")));
                if (Boolean.parseBoolean(request.getParameter("employeeHaveEsi"))) {
//            employee.setEmployerEsi(Double.valueOf(request.getParameter("employerEsi")));
                    employee.setEmployeeEsi(Double.valueOf(request.getParameter("employeeEsi")));
                }
                employee.setEmployeeHaveProfTax(Boolean.parseBoolean(request.getParameter("employeeHaveProfTax")));
                employee.setShowSalarySheet(Boolean.parseBoolean(request.getParameter("showSalarySheet")));

                employee.setGender(request.getParameter("gender"));
                employee.setPoliceCaseDetails(request.getParameter("policeCaseDetails"));
                employee.setStatus(true);

                Designation designation = designationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("designationId")), true);
                if (designation != null) {
                    employee.setDesignation(designation);
                }

                if (!request.getParameter("shiftId").equalsIgnoreCase("") && !request.getParameter("shiftId").equalsIgnoreCase("undefined")) {
                    Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(request.getParameter("shiftId")), true);
                    if (shift != null) {
                        employee.setShift(shift);
                    }
                } else {
                    employee.setShift(null);
                }

                if (request.getParameter("companyId") != null) {
                    Company company = companyRepository.findByIdAndStatus(Long.parseLong(request.getParameter("companyId")), true);
                    if (company != null) {
                        employee.setCompany(company);
                    }
                }

                if (request.getParameter("siteId") != null) {
                    Site site = siteRepository.findByIdAndStatus(Long.parseLong(request.getParameter("siteId")), true);
                    if (site != null) {
                        employee.setSite(site);
                    }
                }

                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                employee.setUpdatedBy(user.getId());
                employee.setUpdatedAt(LocalDateTime.now());
                employee.setInstitute(user.getInstitute());
                try {
                    List<EmployeeFamily> employeeFamilyList = new ArrayList<>();
                    String jsonToStr = request.getParameter("family");
                    JsonArray jsonArray = new JsonParser().parse(jsonToStr).getAsJsonArray();
                    for (JsonElement jsonElement : jsonArray) {
                        JsonObject object = jsonElement.getAsJsonObject();

                        if (object.has("id")) {
                            System.out.println("Old family " + object.get("id").getAsLong());
                            EmployeeFamily employeeFamily = employeeFamilyRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employee.getEmployeeFamily().remove(employeeFamily);
                            employeeFamilyRepository.deleteFamilyFromEmployee(object.get("id").getAsLong());
                            System.out.println("Removed family " + object.get("id").getAsLong());
                        }

                        if (object.has("fullName") && object.get("fullName").getAsString() != null) {
                            EmployeeFamily employeeFamily = this.getEmployeeFamilyFromJsonObject(object, user);
                            employeeFamilyList.add(employeeFamily);
                        }
                    }

                    List<EmployeeEducation> employeeEducationList = new ArrayList<>();
                    String jsonToStr1 = request.getParameter("education");
                    JsonArray array1 = new JsonParser().parse(jsonToStr1).getAsJsonArray();
                    for (JsonElement jsonElement : array1) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("id")) {
                            System.out.println("Old education " + object.get("id").getAsLong());
                            EmployeeEducation employeeEducation = employeeEducationRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employee.getEmployeeEducation().remove(employeeEducation);
                            employeeEducationRepository.deleteEducationFromEmployee(object.get("id").getAsLong());
                            System.out.println("Removed education " + object.get("id").getAsLong());
                        }
                        if (object.has("schoolName") && object.get("schoolName").getAsString() != null) {
                            EmployeeEducation employeeEducation = this.getEmployeeEducationFromJsonObject(object, user);
                            employeeEducationList.add(employeeEducation);
                        }
                    }

                    String oldDocRemove = request.getParameter("oldDocRemoveList");
                    JsonArray oldDocRemoveArray = new JsonParser().parse(oldDocRemove).getAsJsonArray();
                    if (oldDocRemoveArray.size() > 0) {
                        for (int i = 0; i < oldDocRemoveArray.size(); i++) {
                            JsonObject object = oldDocRemoveArray.get(i).getAsJsonObject();
                            EmployeeDocument employeeDocument = employeeDocumentRepository.findByIdAndStatus(object.get("empDocumentId").getAsLong(), true);
                            if (employeeDocument != null) {
                               /* Boolean result = awss3Service.deleteFileFromS3Bucket(employeeDocument.getImageKey());
                                if (result) {
                                    try {
                                        employee.getEmployeeDocuments().remove(employeeDocument);
                                        employeeDocumentRepository.deleteDocumentFromEmployee(employeeDocument.getId());
                                        System.out.println("Document Deleted" + employeeDocument.getId());
                                    } catch (Exception e) {
                                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                        responseObject.setMessage("Failed to delete old documents. Please try again!");
                                        return responseObject;
                                    }
                                }*/

                                if (employeeDocument.getImagePath() != null) {
                                    File oldFile = new File("." + employeeDocument.getImagePath());

                                    if (oldFile.exists()) {
                                        System.out.println("Document Deleted");
                                        //remove file from local directory
                                        if (!oldFile.delete()) {
                                            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                            responseObject.setMessage("Failed to delete old documents. Please try again!");
                                            return responseObject;
                                        } else {
                                            employee.getEmployeeDocuments().remove(employeeDocument);
                                            employeeDocumentRepository.deleteDocumentFromEmployee(employeeDocument.getId());
                                            System.out.println("Document Deleted" + employeeDocument.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    List<EmployeeDocument> employeeDocumentList = new ArrayList<>();
                    String jsonToStr2 = request.getParameter("document");
                    JsonArray array2 = new JsonParser().parse(jsonToStr2).getAsJsonArray();
                    for (int i = 0; i < array2.size(); i++) {
                        JsonObject object = array2.get(i).getAsJsonObject();

                        if (object.has("id")) {
                            System.out.println("Old document " + object.get("id").getAsLong());
                            EmployeeDocument employeeDocument = employeeDocumentRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employeeDocumentList.add(employeeDocument);
                        } else {
                            if (object.get("d_documentId").getAsJsonObject() != null) {
                                EmployeeDocument employeeDocument = new EmployeeDocument();
                                JsonObject docObject = object.get("d_documentId").getAsJsonObject();
                                Document document = documentRepository.findByIdAndStatus(Long.parseLong(docObject.get("value").getAsString()), true);
                                employeeDocument.setDocument(document);

                                /*if (request.getFile("document" + i) != null) {
                                    String imagePath = uploadDocumentImage(request.getFile("document" + i));
                                    String[] arr = imagePath.split("#");
                                    if (imagePath != null) {
                                        employeeDocument.setImagePath(awss3Service.getBASE_URL() + arr[0]);
                                        employeeDocument.setImageKey(arr[1]);
                                    } else {
                                        responseObject.setMessage("Image uploading error");
                                        responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                                        return responseObject;
                                    }
                                } else {
                                    responseObject.setMessage("Please upload document");
                                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                                    return responseObject;
                                }*/
                                if (request.getFile("document" + i) != null) {
                                    MultipartFile image = request.getFile("document" + i);
                                    fileStorageProperties.setUploadDir("./uploads" + File.separator + "emp_documents" + File.separator);
                                    String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                                    if (imagePath != null) {
                                        employeeDocument.setImagePath("/uploads" + File.separator + "emp_documents" + File.separator + imagePath);
                                    } else {
                                        responseObject.setMessage("Failed to upload documents. Please try again!");
                                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                        return responseObject;
                                    }
                                }

                                employeeDocument.setCreatedBy(user.getId());
                                employeeDocument.setStatus(true);
                                employeeDocument.setInstitute(user.getInstitute());
                                employeeDocumentList.add(employeeDocument);
                            }
                        }
                    }

                    String jsonToStrOldDocs = request.getParameter("oldDocumentList");
                    JsonArray arrayOldDocs = new JsonParser().parse(jsonToStrOldDocs).getAsJsonArray();
                    for (JsonElement jsonElement : arrayOldDocs) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("id")) {
                            System.out.println("Old document " + object.get("id").getAsLong());
                            EmployeeDocument employeeDocument = employeeDocumentRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employeeDocumentList.add(employeeDocument);
                        }
                    }

                    List<EmployeeExperienceDetails> experienceDetailsList = new ArrayList<>();
                    String jsonToStr3 = request.getParameter("experience");
                    JsonArray array3 = new JsonParser().parse(jsonToStr3).getAsJsonArray();
                    for (JsonElement jsonElement : array3) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("id")) {
                            System.out.println("Old experience " + object.get("id").getAsLong());
                            EmployeeExperienceDetails employeeExperienceDetails = employeeExperienceRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employee.getEmployeeExperienceDetails().remove(employeeExperienceDetails);
                            employeeExperienceRepository.deleteExperienceFromEmployee(object.get("id").getAsLong());
                            System.out.println("Removed experience " + object.get("id").getAsLong());
                        }
                        if (object.has("companyName") && object.get("companyName").getAsString() != null) {
                            EmployeeExperienceDetails employeeExperienceDetails = this.getEmployeeExperienceFromJsonObject(object, user);
                            experienceDetailsList.add(employeeExperienceDetails);
                        }
                    }

                    List<EmployeeReference> employeeReferenceList = new ArrayList<>();
                    String jsonToStr4 = request.getParameter("reference");
                    JsonArray array4 = new JsonParser().parse(jsonToStr4).getAsJsonArray();
                    for (JsonElement jsonElement : array4) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("id")) {
                            System.out.println("Old reference " + object.get("id").getAsLong());
                            EmployeeReference employeeReference = employeeReferenceRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                            employee.getEmployeeReferences().remove(employeeReference);
                            employeeReferenceRepository.deleteReferenceFromEmployee(object.get("id").getAsLong());
                            System.out.println("Removed reference " + object.get("id").getAsLong());
                        }
                        if (object.has("name") && object.get("name").getAsString() != null) {
                            EmployeeReference employeeReference = this.getEmployeeReferenceFromJsonObject(object, user);
                            employeeReferenceList.add(employeeReference);
                        }
                    }

                    String oldSalRemove = request.getParameter("oldsalremoveList");
                    JsonArray oldSalRemoveArray = new JsonParser().parse(oldSalRemove).getAsJsonArray();
                    if (oldSalRemoveArray.size() > 0) {
                        for (int i = 0; i < oldSalRemoveArray.size(); i++) {
                            JsonObject object = oldSalRemoveArray.get(i).getAsJsonObject();
                            EmployeeSalary employeeSalary = employeeSalaryRepository.findByIdAndStatus(object.get("empSalId").getAsLong(), true);
                            if (employeeSalary != null) {
                                employee.getEmployeeSalaries().remove(employeeSalary);
                                employeeSalaryRepository.deleteSalaryFromEmployee(employeeSalary.getId());
                                System.out.println("Salary Deleted" + employeeSalary.getId());
                            }
                        }
                    }

                    List<EmployeeSalary> employeeSalaryList = new ArrayList<>();
                    String jsonToEmpSalary = request.getParameter("salaryList");
                    JsonArray array5 = new JsonParser().parse(jsonToEmpSalary).getAsJsonArray();
                    for (JsonElement jsonElement : array5) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("effectiveDate") && object.get("effectiveDate").getAsString() != null) {
                            EmployeeSalary employeeSalary = this.getEmployeeSalaryFromJsonObject(object, user, employee);
                            employeeSalaryList.add(employeeSalary);
                        }
                    }

                    try {
                        employee.setEmployeeFamily(employeeFamilyList);
                        employee.setEmployeeEducation(employeeEducationList);
                        if (employeeDocumentList.size() > 0) {
                            employee.setEmployeeDocuments(employeeDocumentList);
                        }
                        if (employeeSalaryList.size() > 0) {
                            employee.setEmployeeSalaries(employeeSalaryList);
                        }
                        employee.setEmployeeReferences(employeeReferenceList);
                        employee.setEmployeeExperienceDetails(experienceDetailsList);

                        entityManager.persist(employee);
                        responseObject.setMessage("Employee updated successfully");
                        responseObject.setResponseStatus(HttpStatus.OK.value());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exception " + e.getMessage());
                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        responseObject.setMessage("Internal Server Error");
                    }

                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Data not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseObject.setMessage("Failed to update employee");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    private EmployeeSalary getEmployeeSalaryFromJsonObject(JsonObject object, Users user, Employee employee) {
        EmployeeSalary employeeSalary = new EmployeeSalary();
        if (!object.get("id").getAsString().equalsIgnoreCase("")) {
            employeeSalary = employeeSalaryRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
            employeeSalary.setUpdatedAt(LocalDateTime.now());
            employeeSalary.setUpdatedBy(user.getId());
        }
        employeeSalary.setEmployeeId(employee.getId());
        employeeSalary.setEffectiveDate(LocalDate.parse(object.get("effectiveDate").getAsString()));
        employeeSalary.setSalary(object.get("salary").getAsDouble());
        employeeSalary.setCreatedBy(user.getId());
        employeeSalary.setCreatedAt(LocalDateTime.now());
        employeeSalary.setStatus(true);

        return employeeSalary;
    }

    public Object addEmployeeDeviceId(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Employee employee = employeeRepository.findByMobileNumber(Long.parseLong(requestParam.get("contact")));
            if (employee != null) {
                String deviceId = employee.getDeviceId() != null ? employee.getDeviceId() : null;
                if(deviceId != null){
                    Employee emp = employeeRepository.findByMobileNumberAndDeviceId(String.valueOf(employee.getMobileNumber()),deviceId);
                    if(emp!= null){
                        responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                        responseMessage.setMessage("Employee Already Registered, Please Login");
                        return responseMessage;
                    }
                } else if(!employee.getHasOwnMobileDevice()) {
                    employee.setDeviceId(requestParam.get("deviceId"));
                    if(employee.getMobileNumber().toString().equals("1234567890"))
                        employee.setStatus(true);
                    else
                        employee.setStatus(false);
                    employee.setTextPassword(requestParam.get("password"));
                    String encPassword = passwordEncoder.encode(requestParam.get("password"));
                    employee.setPassword(encPassword);
                    employeeRepository.save(employee);
                    if(employee.getMobileNumber().toString().equals("1234567890"))
                        responseMessage.setMessage("Registration Successful..Please login");
                    else
                        responseMessage.setMessage("Registration Successful.. Contact Admin for Approval");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                    return responseMessage;
                } else {
                    List<Employee> employees = employeeRepository.getEmployeesByDeviceId(requestParam.get("deviceId"));
                    if(employees.size() > 0){
                        String mobile = requestParam.get("contact");
                        Employee found = null;
                        for(Employee emp : employees){
                            if(emp.getMobileNumber().equals(mobile)){
                                found = emp;
                            }
                        }
                        if(found == null){
                            employee.setDeviceId(requestParam.get("deviceId"));
                            if(employee.getMobileNumber().toString().equals("1234567890"))
                                employee.setStatus(true);
                            else
                                employee.setStatus(false);
                            employee.setTextPassword(requestParam.get("password"));
                            String encPassword = passwordEncoder.encode(requestParam.get("password"));
                            employee.setPassword(encPassword);
                            employeeRepository.save(employee);
                            if(employee.getMobileNumber().toString().equals("1234567890"))
                                responseMessage.setMessage("Registration Successful..Please login");
                            else
                                responseMessage.setMessage("Registration Successful.. Contact Admin for Approval");
                            responseMessage.setResponseStatus(HttpStatus.OK.value());
                            return responseMessage;
                        }
                    } else {
                        Employee emp = employeeRepository.findByDeviceId(requestParam.get("deviceId"));
                        if(emp != null && emp.getHasOwnMobileDevice()){
                            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                            responseMessage.setMessage("Device Already Registered with Other Mobile Number");
                            return responseMessage;
                        }
                        employee.setDeviceId(requestParam.get("deviceId"));
                        if(employee.getMobileNumber().toString().equals("1234567890"))
                            employee.setStatus(true);
                        else
                            employee.setStatus(false);
                        employee.setTextPassword(requestParam.get("password"));
                        String encPassword = passwordEncoder.encode(requestParam.get("password"));
                        employee.setPassword(encPassword);
                        employeeRepository.save(employee);
                        if(employee.getMobileNumber().toString().equals("1234567890"))
                            responseMessage.setMessage("Registration Successful..Please login");
                        else
                            responseMessage.setMessage("Registration Successful.. Contact Admin for Approval");
                        responseMessage.setResponseStatus(HttpStatus.OK.value());
                        return responseMessage;
                    }
                }
            } else {
                Employee emp = employeeRepository.findByDeviceId(requestParam.get("deviceId"));
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                if(emp != null && emp.getHasOwnMobileDevice()){
                    responseMessage.setMessage("Device Already Registered with Other Mobile Number");
                } else {
                    responseMessage.setMessage("Please Register, Contact Admin");
                }
                return responseMessage;
//                employee.setFirstName(requestParam.get("firstName"));
//                employee.setLastName(requestParam.get("lastName"));
//                employee.setDeviceId(requestParam.get("deviceId"));
//                employee.setMobileNumber(Long.parseLong(requestParam.get("contact")));
//                employee.setStatus(false);
//                employee.setTextPassword(requestParam.get("password"));
//                String encPassword = passwordEncoder.encode(requestParam.get("password"));
//                employee.setPassword(encPassword);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.setMessage("Failed to change status");
        }
        return responseMessage;
    }

    public Object changeEmployeeStatus(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Employee employee = employeeRepository.findById(Long.parseLong(requestParam.get("id"))).get();
        if (employee != null) {
            String status = "";
            if (Boolean.parseBoolean(requestParam.get("status"))) {
                status = "activated";
            } else {
                status = "de-activated";
            }
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employee.setStatus(Boolean.parseBoolean(requestParam.get("status")));
            employee.setUpdatedBy(user.getId());
            employee.setUpdatedAt(LocalDateTime.now());
            employee.setInstitute(user.getInstitute());
            try {
                employeeRepository.save(employee);
                responseMessage.setMessage("Employee " + status + " successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("Failed to change status");
            }
        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("Data not found");
        }
        return responseMessage;
    }

    @Override
    public UserDetails loadUserByUsername(String mobileNo) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByMobileNumber(Long.parseLong(mobileNo));
        if (employee == null) {
            throw new UsernameNotFoundException("User not found with username: " + mobileNo);
        }
        String username = employee.getMobileNumber().toString();
        return new org.springframework.security.core.userdetails.User(username, employee.getPassword(), new ArrayList<>());
    }

    public Employee findUserByMobile(String username, String password) {
        Employee employee = employeeRepository.findByMobileNumber(Long.parseLong(username));
        if (passwordEncoder.matches(password, employee.getPassword())) {
            return employee;
        }
        return null;
    }

    public  Employee findUserByMobileAndDeviceId(String username, String password, String deviceId){
        Employee employee = employeeRepository.findByMobileNumberAndDeviceId(Long.parseLong(username), deviceId);
        if (employee != null && passwordEncoder.matches(password, employee.getPassword())) {
            return employee;
        }
        return null;
    }

    public Object findEmp(Long mobileNumber) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByMobileNumber(mobileNumber);
        if (employee == null) {
            throw new UsernameNotFoundException("User not found with username: " + mobileNumber);
        }
        return employee;
    }

    public Object sendOtp(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        String username = request.get("username");
        Employee employee = employeeRepository.findByMobileNumber(Long.parseLong(username));
        if (employee != null) {
            OTP otp = new OTP();
            Random random = new Random();
            String uniqueOtp = String.valueOf(random.nextInt((9999 - 100) + 1) + 10);
            otp.setOtp(uniqueOtp);
            otp.setStatus(true);
            otp.setUsername(username);
            otp.setMobileNo(employee.getMobileNumber());
            try {
                otpRepository.save(otp);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
                responseMessage.setMessage("OTP sent to mobile no.");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("Failed to send otp");
            }
        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("User not exist with mobile number, Try again.");
        }
        return responseMessage;
    }

    public Object verifyOtp(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        String username = request.get("username");
        String userOtp = request.get("userOtp");
        System.out.println("USer Otp " + userOtp);
        OTP otp = otpRepository.findTop1ByUsernameAndStatusOrderByIdDesc(username, true);
        if (otp != null) {
            String savedOtp = String.valueOf(otp.getOtp());
            System.out.println("Stored Otp " + savedOtp);
            if (savedOtp.equals(userOtp)) {
                otpRepository.updateOtpStatus(username);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
                responseMessage.setMessage("OTP verified successfully");
            } else {
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("OTP mismatched!");
            }

        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("OTP mismatched! Try again.");
        }
        return responseMessage;
    }

    public Object forgetPassword(Map<String, String> request, HttpServletRequest httpServletRequest) {
        ResponseMessage responseMessage = new ResponseMessage();
        String mobileNumber = request.get("mobile_number");
        Employee employee = employeeRepository.findByMobileNumber(Long.parseLong(mobileNumber));
        if (employee != null) {
            employee.setTextPassword(request.get("password"));
            String encPassword = passwordEncoder.encode(request.get("password"));
            employee.setPassword(encPassword);
            try {
                employeeRepository.save(employee);
                responseMessage.setMessage("Password changed successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed change password");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("User not exist with mobile number, Try again.");
        }
        return responseMessage;
    }

    public Object checkMobileNumberExists(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        String mobileNumber = request.get("mobile_number");
        String deviceId = request.get("device_id");
        Employee employee = employeeRepository.findByMobileNumberAndDeviceId(Long.parseLong(mobileNumber), deviceId);
        if (employee != null) {
            responseMessage.setResponseStatus(HttpStatus.OK.value());
            responseMessage.setMessage("Entered mobile number is valid");
        } else {
            Employee empObj = employeeRepository.findByMobileNumber(Long.parseLong(mobileNumber));
            if(empObj != null){
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseMessage.setMessage("Please use authorized mobile");
            } else {
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseMessage.setMessage("Please Enter Registered Mobile number");
            }
        }
        return responseMessage;
    }

    public Object fileUpload(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        MultipartFile image = request.getFile("file");
        fileStorageProperties.setUploadDir("./uploads" + File.separator + "demo" + File.separator);

        String filename = fileStorageService.storeFile(image, fileStorageProperties);
        return filename;
    }

    public JsonObject listOfEmployee(HttpServletRequest httpServletRequest) {
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Employee> employeeList = employeeRepository.findAllByInstituteIdAndStatus(user.getInstitute().getId(), true);
            for (Employee employee : employeeList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", employee.getId());
                object.addProperty("firstName", employee.getFirstName());
                object.addProperty("middleName", employee.getMiddleName());
                object.addProperty("lastName", employee.getLastName());
                object.addProperty("employeeName", utility.getEmployeeName(employee));
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
            /*if (employeeList.size() > 0) {
            } else {
                response.addProperty("message", "Data not found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }*/
        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject getEmployeeList(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            JsonArray jsonArray = new JsonArray();
            LocalDate today = LocalDate.now();
            if (!jsonRequest.get("attendanceDate").equalsIgnoreCase(""))
                today = LocalDate.parse(jsonRequest.get("attendanceDate"));
            List<Attendance> attendanceList = attendanceRepository.findByInstituteIdAndAttendanceDateAndStatus(today, true, "L3", employee.getInstitute().getId());
            System.out.println("attendanceList.size() : " + attendanceList.size());
            for (Attendance attendance : attendanceList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", attendance.getEmployee().getId());
                object.addProperty("attendanceId", attendance.getId());
                object.addProperty("employeeName", utility.getEmployeeName(attendance.getEmployee()));
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject orderByEmployee(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Employee> employeeList = new ArrayList<>();
            int orderById = Integer.parseInt(request.getParameter("orderById"));
            if (orderById == 1) {
                employeeList = employeeRepository.findAllByOrderByFirstName();
            } else {
                employeeList = employeeRepository.findAllByOrderByLastName();
            }
            for (Employee employee : employeeList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", employee.getId());
                object.addProperty("firstName", employee.getFirstName());
                object.addProperty("middleName", employee.getMiddleName());
                object.addProperty("lastName", employee.getLastName());
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
            /*if (employeeList.size() > 0) {
            } else {
                response.addProperty("message", "Data not found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }*/
        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject employeeList() {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Employee> employeeList = employeeRepository.findByStatus(true);

            for (Employee employee : employeeList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", employee.getId());
                object.addProperty("employeeName", utility.getEmployeeName(employee));
                object.addProperty("mobileNumber", employee.getMobileNumber().toString());
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());

        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
