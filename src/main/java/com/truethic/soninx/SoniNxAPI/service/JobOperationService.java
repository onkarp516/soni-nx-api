package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.*;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageProperties;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageService;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.JobOperationDTDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class JobOperationService {
    private static final Logger operationLogger = LoggerFactory.getLogger(JobOperationService.class);
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    JobOperationRepository jobOperationRepository;
    @Autowired
    private TaskMasterHistoryRepository taskMasterHistoryRepository;
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private TaskMasterRepository taskMasterRepository;
    @Autowired
    JobRepository jobRepository;
    @Autowired
    private TaskService taskService;

    @Value("${spring.serversource.url}")
    private String serverUrl;
    @Autowired
    private FileStorageService fileStorageService;
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private OperationDetailsRepository operationDetailsRepository;

    public Object createJobOperation(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        JobOperation jobOperation = new JobOperation();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
            if (job != null) {
                jobOperation.setJob(job);
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Job not found");
                return responseObject;
            }
            if (request.getFile("document") != null) {
                MultipartFile image = request.getFile("document");
                fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations" + File.separator);
                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                if (imagePath != null) {
                    jobOperation.setOperationImagePath("/uploads" + File.separator + "jobOperations" + File.separator + imagePath);
                } else {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Failed to upload documents. Please try again!");
                    return responseObject;
                }
            }

            if (request.getFile("procedureSheet") != null) {
                MultipartFile image = request.getFile("procedureSheet");
                fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations_sheet" + File.separator);
                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                if (imagePath != null) {
                    jobOperation.setProcedureSheet("/uploads" + File.separator + "jobOperations_sheet" + File.separator + imagePath);
                } else {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Failed to upload documents. Please try again!");
                    return responseObject;
                }
            }

            jobOperation.setOperationName(request.getParameter("operationName"));
            jobOperation.setOperationNo(request.getParameter("operationNo"));
            jobOperation.setCycleTime(Double.parseDouble(request.getParameter("cycleTime")));
            jobOperation.setPcsRate(Double.parseDouble(request.getParameter("pcsRate")));
            jobOperation.setAveragePerShift(Double.parseDouble(request.getParameter("averagePerShift")));
            jobOperation.setPointPerJob(Double.parseDouble(request.getParameter("pointPerJob")));
            jobOperation.setOperationBreakInMin(Integer.valueOf(request.getParameter("operationBreakInMin")));
            jobOperation.setStatus(true);
            jobOperation.setCreatedBy(user.getId());
            jobOperation.setInstitute(user.getInstitute());
            try {
                job.getJobOperations().add(jobOperation);
                jobOperationRepository.save(jobOperation);
                responseObject.setMessage("Job Operation created successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Internal Server Error");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to add job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object createNewJobOperation(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            JobOperation jobOperation = new JobOperation();
            Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
            if (job != null) {
                jobOperation.setJob(job);
            } else {
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseObject.setMessage("Job not found");
                return responseObject;
            }
            jobOperation.setOperationName(request.getParameter("operationName"));
            jobOperation.setOperationNo(request.getParameter("operationNo"));
            if (request.getFile("document") != null) {
                MultipartFile image = request.getFile("document");
                fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations" + File.separator);
                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                if (imagePath != null) {
                    jobOperation.setOperationImagePath("/uploads" + File.separator + "jobOperations" + File.separator + imagePath);
                } else {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Failed to upload documents. Please try again!");
                    return responseObject;
                }
            }
            if (request.getFile("procedureSheet") != null) {
                MultipartFile image = request.getFile("procedureSheet");
                fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations_sheet" + File.separator);
                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);
                if (imagePath != null) {
                    jobOperation.setProcedureSheet("/uploads" + File.separator + "jobOperations_sheet" + File.separator + imagePath);
                } else {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Failed to upload documents. Please try again!");
                    return responseObject;
                }
            }

            jobOperation.setStatus(true);
            jobOperation.setInstitute(user.getInstitute());
            JobOperation jobOperation1 = jobOperationRepository.save(jobOperation);

            String jRows = request.getParameter("jobOperationList");
            JsonArray jsonArray = new JsonParser().parse(jRows).getAsJsonArray();
            List<OperationDetails> operationDetailsList = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject object = jsonArray.get(i).getAsJsonObject();

                OperationDetails operationDetails = new OperationDetails();
                operationDetails.setJobOperation(jobOperation1);
                operationDetails.setCycleTime(object.get("cycleTime").getAsDouble());
                operationDetails.setPcsRate(object.get("pcsRate").getAsDouble());
                operationDetails.setAveragePerShift(object.get("averagePerShift").getAsDouble());
                operationDetails.setPointPerJob(object.get("pointPerJob").getAsDouble());

                operationDetails.setOperationBreakInMin(object.get("operationBreakInMin").getAsInt());
                operationDetails.setEffectiveDate(LocalDate.parse(object.get("effectiveDate").getAsString()));
                operationDetails.setStatus(true);
                operationDetails.setCreatedBy(user.getId());
                operationDetails.setInstitute(user.getInstitute());
                operationDetailsList.add(operationDetails);
            }

            try {
                operationDetailsRepository.saveAll(operationDetailsList);
                responseObject.setMessage("Job Operation created successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Internal Server Error");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to add job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


    public JsonObject getJobOperation() {
        JsonObject responseMessage = new JsonObject();
        try {
//            List<JobOperation> operationList = jobOperationRepository.findAllByStatus(true);
            JsonArray opArray = new JsonArray();
            List<Object[]> operationList = jobOperationRepository.getDistinctOperationNamesData(true);
            for (int i = 0; i < operationList.size(); i++) {
                Object[] opObj = operationList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("operationName", opObj[0].toString());
                opArray.add(jsonObject);
            }

            responseMessage.add("response", opArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("message", "Exception occurred");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object DTJobOperation(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String selectedJobName = request.get("selectedJobName");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<JobOperation> jobOperationList = new ArrayList<>();
        List<JobOperationDTDTO> jobOperationDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT job_operation_tbl.*, job_tbl.job_name as job_name FROM `job_operation_tbl` LEFT " +
                    "JOIN job_tbl ON job_operation_tbl.job_id=job_tbl.id WHERE job_operation_tbl.status=1 AND job_operation_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND ( job_name LIKE '%" + searchText + "%' OR operation_name LIKE '%" + searchText + "%'" +
                        " OR operation_no LIKE '%" + searchText + "%' )";
            }

            if (!selectedJobName.equalsIgnoreCase("")) {
                query = query + " AND job_name= '" + selectedJobName + "' ";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") &&
                    jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, JobOperation.class);
            Query q1 = entityManager.createNativeQuery(query1, JobOperation.class);

            jobOperationList = q.getResultList();
            System.out.println("Limit total rows " + jobOperationList.size());

            if (jobOperationList.size() > 0) {
                for (JobOperation jobOperation : jobOperationList) {
                    jobOperationDTDTOList.add(convertToDTDTO(jobOperation));
                }
            }

            List<JobOperation> jobOperationArrayList = new ArrayList<>();
            jobOperationArrayList = q1.getResultList();
            System.out.println("total rows " + jobOperationArrayList.size());

            genericDTData.setRows(jobOperationDTDTOList);
            genericDTData.setTotalRows(jobOperationArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(jobOperationDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private JobOperationDTDTO convertToDTDTO(JobOperation jobOperation) {
        JobOperationDTDTO jobOperationDTDTO = new JobOperationDTDTO();
        jobOperationDTDTO.setJobOperationId(jobOperation.getId());
        jobOperationDTDTO.setOperationName(jobOperation.getOperationName());
        jobOperationDTDTO.setOperationNo(jobOperation.getOperationNo());
        jobOperationDTDTO.setCycleTime(jobOperation.getCycleTime());
        jobOperationDTDTO.setPcsRate(jobOperation.getPcsRate());
        jobOperationDTDTO.setAveragePerShift(jobOperation.getAveragePerShift());
        jobOperationDTDTO.setPointPerJob(jobOperation.getPointPerJob());
        jobOperationDTDTO.setOperationDiameterType(jobOperation.getOperationDiameterType());
        jobOperationDTDTO.setJobName(jobOperation.getJob().getJobName());
        jobOperationDTDTO.setCreatedAt(String.valueOf(jobOperation.getCreatedAt()));
        if (jobOperation.getOperationImagePath() != null)
            jobOperationDTDTO.setOperationImagePath(serverUrl + jobOperation.getOperationImagePath());
        if (jobOperation.getProcedureSheet() != null)
            jobOperationDTDTO.setProcedureSheet(serverUrl + jobOperation.getProcedureSheet());
        jobOperationDTDTO.setStatus(jobOperation.getStatus());

        return jobOperationDTDTO;
    }

    public JsonObject findJobOperation(Map<String, String> request) {
        JsonObject response = new JsonObject();
        JsonObject operationData = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.get("jobOperationId")), true);
        if (jobOperation != null) {
            List<OperationDetails> operationDetails = operationDetailsRepository.findByJobOperationIdAndStatus(jobOperation.getId(), true);
            for (OperationDetails operationDetails1 : operationDetails) {
                JsonObject object = new JsonObject();
                object.addProperty("id", operationDetails1.getId());
                object.addProperty("effectiveDate", String.valueOf(operationDetails1.getEffectiveDate()));
                object.addProperty("averagePerShift", operationDetails1.getAveragePerShift());
                object.addProperty("cycleTime", operationDetails1.getCycleTime());
                object.addProperty("operationBreakInMin", operationDetails1.getOperationBreakInMin());
                object.addProperty("pcsRate", operationDetails1.getPcsRate());
                object.addProperty("pointPerJob", operationDetails1.getPointPerJob());
                jsonArray.add(object);
            }
            operationData.addProperty("jobOperationId", jobOperation.getId());
            operationData.addProperty("jobOperationName", jobOperation.getOperationName());
            operationData.addProperty("jobOperationNo", jobOperation.getOperationNo());
            operationData.addProperty("jobOperationImagePath",
                    jobOperation.getOperationImagePath() != null ? serverUrl + jobOperation.getOperationImagePath() : "");
            operationData.addProperty("procedureSheet", jobOperation.getProcedureSheet() != null ?
                    serverUrl + jobOperation.getProcedureSheet() : "");
            operationData.addProperty("jobId", jobOperation.getJob().getId());
            operationData.add("operationDetailsList", jsonArray);
            response.add("response", operationData);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } else {
            response.addProperty("message", "Data Not Found");
            response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
        }
        return response;
    }


    public Object updateJobOperation(MultipartHttpServletRequest request) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")),
                    true);
            if (jobOperation != null) {
                Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
                if (job != null) {
                    if (jobOperation.getJob() == job) {
                        System.out.println("same object");
                        jobOperation.setJob(job);
                    } else {
                        Job job1 = jobOperation.getJob();
                        job1.getJobOperations().remove(jobOperation);
                        jobOperationRepository.save(jobOperation);
                        jobOperation.setJob(job);
                        job.getJobOperations().add(jobOperation);
                    }
                } else {
                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                    responseObject.setMessage("Job not found");
                    return responseObject;
                }

                if (request.getFile("jobOperationImagePath") != null) {
                    if (jobOperation.getOperationImagePath() != null) {
                        File oldFile = new File("." + jobOperation.getOperationImagePath());
                        if (oldFile.exists()) {
                            System.out.println("Document Deleted");
                            if (!oldFile.delete()) {
                                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                responseObject.setMessage("Failed to delete old documents. Please try again!");
                                return responseObject;
                            }
                        }
                    }

                    MultipartFile image = request.getFile("jobOperationImagePath");
                    fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations" + File.separator);
                    String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                    if (imagePath != null) {
                        jobOperation.setOperationImagePath("/uploads" + File.separator + "jobOperations" + File.separator + imagePath);
                    } else {
                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        responseObject.setMessage("Failed to upload documents. Please try again!");
                        return responseObject;
                    }
                }


                if (request.getFile("procedureSheet") != null) {
                    if (jobOperation.getProcedureSheet() != null) {
                        File oldFile = new File("." + jobOperation.getProcedureSheet());
                        if (oldFile.exists()) {
                            System.out.println("Document Deleted");
                            //remove file from local directory
                            if (!oldFile.delete()) {
                                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                                responseObject.setMessage("Failed to delete old documents. Please try again!");
                                return responseObject;
                            }
                        }
                    }
                    MultipartFile image = request.getFile("procedureSheet");
                    fileStorageProperties.setUploadDir("./uploads" + File.separator + "jobOperations_sheet" + File.separator);
                    String imagePath = fileStorageService.storeFile(image, fileStorageProperties);

                    if (imagePath != null) {
                        jobOperation.setProcedureSheet("/uploads" + File.separator + "jobOperations_sheet" + File.separator + imagePath);
                    } else {
                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        responseObject.setMessage("Failed to upload documents. Please try again!");
                        return responseObject;
                    }
                }

                jobOperation.setOperationNo(request.getParameter("jobOperationNo"));
                jobOperation.setOperationName(request.getParameter("jobOperationName"));
                jobOperation.setUpdatedAt(LocalDateTime.now());
                jobOperation.setUpdatedBy(user.getId());
                jobOperation.setInstitute(user.getInstitute());
                String oldOperationRemove = request.getParameter("oldoperationremovelist");
                JsonArray oldOperationRemoveArray = new JsonParser().parse(oldOperationRemove).getAsJsonArray();
                if (oldOperationRemoveArray.size() > 0) {
                    for (int i = 0; i < oldOperationRemoveArray.size(); i++) {
                        JsonObject object = oldOperationRemoveArray.get(i).getAsJsonObject();
                        OperationDetails operationDetails = operationDetailsRepository.findByIdAndStatus(object.get("operationDetailsId").getAsLong(), true);
                        if (operationDetails != null) {
                            jobOperation.getOperationDetails().remove(operationDetails);
                            operationDetailsRepository.deleteById(operationDetails.getId());
                            System.out.println("Operation Deleted" + operationDetails.getId());
                        }
                    }
                }

                try {
                    JobOperation jobOperation1 = jobOperationRepository.save(jobOperation);
                    List<OperationDetails> operationDetailsList = new ArrayList<>();
                    String jsonToEmpSalary = request.getParameter("jobOperationList");
                    JsonArray array = new JsonParser().parse(jsonToEmpSalary).getAsJsonArray();
                    for (JsonElement jsonElement : array) {
                        JsonObject object = jsonElement.getAsJsonObject();
                        if (object.has("effectiveDate") && object.get("effectiveDate").getAsString() != null) {

                            if(LocalDate.parse(object.get("effectiveDate").getAsString()).isBefore(LocalDate.now())){
                                if (!object.get("id").getAsString().equalsIgnoreCase("")) {
                                    OperationDetails opDtls = operationDetailsRepository.findByIdAndStatus(object.get("id").getAsLong(), true);

                                    //update the task master
                                    if (Double.parseDouble(object.get("cycleTime").getAsString()) != opDtls.getCycleTime() ||
                                            Double.parseDouble(object.get("averagePerShift").getAsString()) != opDtls.getAveragePerShift() ||
                                            Double.parseDouble(object.get("pointPerJob").getAsString()) != opDtls.getPointPerJob() ||
                                            Double.parseDouble(object.get("operationBreakInMin").getAsString()) != opDtls.getOperationBreakInMin() ||
                                            !LocalDate.parse(object.get("effectiveDate").getAsString()).isEqual(opDtls.getEffectiveDate()) ||
                                            Double.parseDouble(object.get("pcsRate").getAsString()) != opDtls.getPcsRate()) {
                                        System.out.println("effective date is less than current");
                                        OperationDetails operationDetails = this.getOperationDetails(object, user, jobOperation1, opDtls);
                                        this.updateTaskDetailsByJobOperationAndEffectiveDate(operationDetails.getJobOperation(), operationDetails, user);
                                        operationDetailsList.add(operationDetails);
                                    }
                                }else{

                                    OperationDetails operationDetails = this.getOperationDetails(object, user, jobOperation1, null);
                                    this.updateTaskDetailsByJobOperationAndEffectiveDate(operationDetails.getJobOperation(), operationDetails, user);
                                    operationDetailsList.add(operationDetails);
                                }
                            } else {
                                //Do nothing when entered details are same
                                System.out.println("effective date is exceeding the current date");
                                if (!object.get("id").getAsString().equalsIgnoreCase("")) {
                                    OperationDetails opDtls = operationDetailsRepository.findByIdAndStatus(object.get("id").getAsLong(), true);

                                    OperationDetails operationDetails = this.getOperationDetails(object, user, jobOperation1, opDtls);
                                    operationDetailsList.add(operationDetails);
                                } else {
                                    OperationDetails operationDetails = this.getOperationDetails(object, user, jobOperation1, null);
                                    operationDetailsList.add(operationDetails);
                                }
                            }
//                            System.out.println("Old effective date : "+opDtls.getEffectiveDate());
                        }
                    }
                    operationDetailsRepository.saveAll(operationDetailsList);

                    responseObject.setMessage("Job Operation updated successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }


            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to update job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    private void updateTaskDetailsByJobOperationAndEffectiveDate(JobOperation jobOperation, OperationDetails operationDetails, Users user){

        List<TaskMaster> taskMasterList = taskMasterRepository.findByJobOperationIdAndTaskDateGreaterThanEqualAndTaskStatusAndTaskType(
                jobOperation.getId(), operationDetails.getEffectiveDate(), "complete", 1);
        System.out.println("Effective date : "+operationDetails.getEffectiveDate()+" job operation id : "+ jobOperation.getId());
        System.out.println("task master list size : "+taskMasterList.size());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TaskMaster taskMaster : taskMasterList) {
            try {
                System.out.println("task master Id : "+taskMaster.getId());
                LocalDateTime l1 = taskMaster.getStartTime();
                LocalDateTime l2 = taskMaster.getEndTime();
//                taskMaster.setEndTime(l2);
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                double time = totalTime;
                if (taskMaster != null) {
                    TaskMasterHistory taskMasterHistory = convertToHistory(taskMaster, user);
                    if (taskMasterHistory != null) {
                        taskMasterHistoryRepository.save(taskMasterHistory);
                        try {
                            Employee employee = taskMaster.getEmployee();
                            if (operationDetails != null) {
                                taskMaster.setCycleTime(operationDetails.getCycleTime());
                                taskMaster.setPcsRate(operationDetails.getPcsRate());

                                double avgPerShift = operationDetails.getAveragePerShift();
                                double perJobPOint = (100.0 / avgPerShift);
                                taskMaster.setPerJobPoint(perJobPOint);

                                double totalBreakMinutes = taskMasterRepository.getSumOfBreakTime(taskMaster.getId());
                                double actualTaskTime = time - totalBreakMinutes;
                                taskMaster.setActualWorkTime(actualTaskTime);

                                Double okQty = taskMaster.getOkQty() != null ? taskMaster.getOkQty() : 0;

                                System.out.println("user time in minutes " + time);
                                System.out.println("user totalActualTime in minutes " + actualTaskTime);
                                double jobsPerHour = (60.0 / operationDetails.getCycleTime());
                                double workHours = (time / 60.0);
                                double requiredProduction = (actualTaskTime / operationDetails.getCycleTime());
                                System.out.println("requiredProduction " + requiredProduction);
                                double actualProduction = okQty;
                                double shortProduction = (actualProduction - requiredProduction);
                                double percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
                                System.out.println("percentageOfTask " + percentageOfTask);

                                taskMaster.setJobsPerHour(jobsPerHour);
                                taskMaster.setWorkingHour(workHours);
                                taskMaster.setRequiredProduction(requiredProduction);
                                taskMaster.setActualProduction(actualProduction);
                                taskMaster.setShortProduction(shortProduction);
                                taskMaster.setPercentageOfTask(percentageOfTask);

                                double productionPoint = (double) okQty * perJobPOint;
                                double productionWorkingHour = productionPoint / 12.5;
                                double settingTimeInMinutes = totalBreakMinutes;
                                double perMinutePoint = 100.0 / 60;
                                double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
                                double workingHourWithSetting = productionWorkingHour + settingTimeInHour;
                                double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
                                double totalPoint = productionPoint + settingTimePoint;

                                double wagesPerPoint = taskMaster.getWagesPerDay() / 100.0;
                                double wagesPointBasis = wagesPerPoint * totalPoint;
                                double wagesPerPcs = operationDetails.getPcsRate();
                                double wagesPcsBasis = okQty * wagesPerPcs;

                                taskMaster.setProdPoint(productionPoint);
                                taskMaster.setProdWorkingHour(productionWorkingHour);
                                taskMaster.setSettingTimeInMin(settingTimeInMinutes);
                                taskMaster.setSettingTimeInHour(settingTimeInHour);
                                taskMaster.setWorkingHourWithSetting(workingHourWithSetting);
                                taskMaster.setSettingTimePoint(settingTimePoint);
                                taskMaster.setTotalPoint(totalPoint);

                                taskMaster.setWagesPerPoint(wagesPerPoint);
                                taskMaster.setWagesPointBasis(wagesPointBasis);
                                taskMaster.setWagesPcsBasis(wagesPcsBasis);

                            }
                            taskMaster.setUpdatedAt(LocalDateTime.now());
                            taskMaster.setUpdatedBy(user.getId());
                            taskMaster.setInstitute(user.getInstitute());
                            try {
                                taskMasterRepository.save(taskMaster);
                                taskService.updateEmployeeTaskSummary(taskMaster.getAttendance());
                                System.out.println("Successfully task saved");
                            } catch (Exception e) {
                                operationLogger.error("Failed to update the the task " + e);
                                e.printStackTrace();
                                System.out.println("Exception " + e.getMessage());
                            }
                        } catch (Exception e) {
                            operationLogger.error("Failed to update operation " + e);
                            e.printStackTrace();
                            System.out.println("Exception " + e.getMessage());
                        }
                    } else {
                        System.out.println("Failed to save task history");
                        System.out.println(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                } else {
                    System.out.println("Failed to load task");
                    System.out.println(HttpStatus.NOT_FOUND.value());
                }
            } catch (Exception e) {
                operationLogger.error("Failed to load task " + e);
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                System.out.println("Failed to load task");
                System.out.println(HttpStatus.NOT_FOUND.value());
            }
        }
    }

    private TaskMasterHistory convertToHistory(TaskMaster taskMaster, Users users) {
        TaskMasterHistory taskMasterHistory = new TaskMasterHistory();

        taskMasterHistory.setTaskId(taskMaster.getId());
        taskMasterHistory.setEmployeeId(taskMaster.getEmployee().getId());
        taskMasterHistory.setAttendanceId(taskMaster.getAttendance().getId());
        taskMasterHistory.setUpdatingUserId(users.getId());

        if (taskMaster.getTaskMaster() != null) {
            taskMasterHistory.setTaskMasterId(taskMaster.getTaskMaster().getId());
        }
        if(taskMaster.getInstitute() != null)
            taskMasterHistory.setInstitute(taskMaster.getInstitute());
        taskMasterHistory.setTaskDate(taskMaster.getTaskDate());
        taskMasterHistory.setStartTime(taskMaster.getStartTime());

        if (taskMaster.getEndTime() != null) {
            taskMasterHistory.setEndTime(taskMaster.getEndTime());
        }
        taskMasterHistory.setTotalTime(taskMaster.getTotalTime());
        taskMasterHistory.setActualWorkTime(taskMaster.getActualWorkTime());
        taskMasterHistory.setTaskType(taskMaster.getTaskType());


        if (taskMaster.getMachine() != null) {
            taskMasterHistory.setMachineId(taskMaster.getMachine().getId());
        }
        if (taskMaster.getJob() != null) {
            taskMasterHistory.setJobId(taskMaster.getJob().getId());
        }
        if (taskMaster.getJobOperation() != null) {
            taskMasterHistory.setJobOperationId(taskMaster.getJobOperation().getId());
        }
        if (taskMaster.getWorkBreak() != null) {
            taskMasterHistory.setWorkBreakId(taskMaster.getWorkBreak().getId());
        }

        taskMasterHistory.setRemark(taskMaster.getRemark());
        taskMasterHistory.setAdminRemark(taskMaster.getAdminRemark());
        taskMasterHistory.setCreatedAt(taskMaster.getCreatedAt());
        taskMasterHistory.setCreatedBy(taskMaster.getCreatedBy());
        taskMasterHistory.setUpdatedAt(taskMaster.getUpdatedAt());
        taskMasterHistory.setUpdatedBy(taskMaster.getUpdatedBy());

        return taskMasterHistory;
    }

    private OperationDetails getOperationDetails(JsonObject object, Users user, JobOperation jobOperation, OperationDetails opDtls) {
        OperationDetails operationDetails = new OperationDetails();
        if(opDtls != null)
            operationDetails = opDtls;
        if (!object.get("id").getAsString().equalsIgnoreCase("")) {
            operationDetails = operationDetailsRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
            operationDetails.setUpdatedAt(LocalDateTime.now());
            operationDetails.setUpdatedBy(user.getId());
        }
        operationDetails.setInstitute(user.getInstitute());
        operationDetails.setJobOperation(jobOperation);
        operationDetails.setEffectiveDate(LocalDate.parse(object.get("effectiveDate").getAsString()));
        operationDetails.setOperationBreakInMin(object.get("operationBreakInMin").getAsInt());
        operationDetails.setPointPerJob(object.get("pointPerJob").getAsDouble());
        operationDetails.setAveragePerShift(object.get("averagePerShift").getAsDouble());
        operationDetails.setCycleTime(object.get("cycleTime").getAsDouble());
        operationDetails.setPcsRate(object.get("pcsRate").getAsDouble());
        operationDetails.setCreatedBy(user.getId());
        operationDetails.setCreatedAt(LocalDateTime.now());
        operationDetails.setStatus(true);

        return operationDetails;
    }

    public Object deleteJobOperation(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (jobOperation != null) {
                jobOperation.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                jobOperation.setCreatedBy(user.getId());
                jobOperation.setInstitute(user.getInstitute());
                try {
                    jobOperationRepository.save(jobOperation);
                    responseObject.setMessage("Job Operation deleted successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseObject.setMessage("Internal Server Error");
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                }
            }
        } catch (Exception e) {
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Failed to add job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public JsonObject listForSelection(Map<String, String> request, HttpServletRequest req) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(req.getHeader("Authorization").substring(7));
            Long jobId = Long.valueOf(request.get("jobId"));
            System.out.println("jobId =>" + jobId);
            List<JobOperation> jobOperationList = jobOperationRepository.findByJobIdAndStatus(jobId, true);
            for (JobOperation jobOperation : jobOperationList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", jobOperation.getId());
                object.addProperty("operationName", jobOperation.getOperationName());

                object.addProperty("dataExist", false);
                object.addProperty("cycleTime", 0);
                object.addProperty("averagePerShift", 0);
                object.addProperty("pointPerJob", 0);
                object.addProperty("jobsHourBasis", 0);
                String operationData = operationDetailsRepository.getOperationDetailsByOperationId(jobOperation.getId(), LocalDate.now());
                if (operationData != null) {
                    String[] opData = operationData.split(",");
                    object.addProperty("dataExist", true);
                    object.addProperty("cycleTime", opData[1]);
                    object.addProperty("averagePerShift", opData[2]);
                    object.addProperty("pointPerJob", opData[3]);
                    Double jobsHourBasis = (60 / (Double.parseDouble(opData[1])));
                    object.addProperty("jobsHourBasis", String.format("%.2f", jobsHourBasis));
                }
                jsonArray.add(object);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("message", "Exception Occurred");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }


    private String uploadJobOperationDocument(MultipartFile multipartFile) {
        String dir = "job_operation_document";
        String imagePath = null;
        // imagePath = awss3Service.uploadFile(multipartFile, dir);
        return imagePath;
    }

    public JsonObject listJobOperation(Map<String, String> request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Long jobId = Long.valueOf(request.get("jobId"));
            List<JobOperation> jobOperationList = jobOperationRepository.findByJobIdAndStatus(jobId, true);
            for (JobOperation jobOperation : jobOperationList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", jobOperation.getId());
                object.addProperty("operationName", jobOperation.getOperationName());

                object.addProperty("dataExist", false);
                object.addProperty("cycleTime", 0);
                object.addProperty("averagePerShift", 0);
                object.addProperty("pointPerJob", 0);
                object.addProperty("jobsHourBasis", 0);
                String operationData = operationDetailsRepository.getOperationDetailsByOperationId(jobOperation.getId(), LocalDate.now());
                if (operationData != null) {
                    String[] opData = operationData.split(",");
                    object.addProperty("dataExist", true);
                    object.addProperty("cycleTime", opData[1]);
                    object.addProperty("averagePerShift", opData[2]);
                    object.addProperty("pointPerJob", opData[3]);
                    Double jobsHourBasis = (60 / (Double.parseDouble(opData[1])));
                    object.addProperty("jobsHourBasis", String.format("%.2f", jobsHourBasis));
                }

                jsonArray.add(object);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("message", "Exception occurred");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }


    public JsonObject viewDrawing(Map<String, String> request, HttpServletRequest req) {
        JsonObject response = new JsonObject();
        try {
            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.get("jobOperationId")), true);
            if (jobOperation != null) {
                JsonObject dObject = new JsonObject();
                if (jobOperation.getOperationImagePath() != null) {
                    dObject.addProperty("operationDrawing", serverUrl + jobOperation.getOperationImagePath());
                } else {
                    dObject.addProperty("operationDrawing", "");
                }
                if (jobOperation.getProcedureSheet() != null) {
                    dObject.addProperty("procedureDrawing", serverUrl + jobOperation.getProcedureSheet());
                } else {
                    dObject.addProperty("procedureDrawing", "");
                }
                response.add("response", dObject);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                response.addProperty("message", "No Data Found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.addProperty("message", "Internal Server Error");
        }
        return response;
    }


    public Object deleteProcedureSheet(Map<String, String> jsonRequest) {
        JsonObject response = new JsonObject();
        try {
            Long operationId = Long.valueOf(jsonRequest.get("id"));
            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(operationId, true);
            if (jobOperation != null) {
                if (jobOperation.getProcedureSheet() != null) {
                    System.out.println("jobOperation.getProcedureSheet() " + jobOperation.getProcedureSheet());
                    File oldFile = new File("." + jobOperation.getProcedureSheet());
                    if (oldFile.exists()) {
                        System.out.println("Document Deleted");
                        //remove file from local directory
                        if (!oldFile.delete()) {
                            response.addProperty("message", "Failed to delete document. Please try again!");
                            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                            return response;
                        }
                    }
                    jobOperation.setProcedureSheet(null);
                    jobOperationRepository.save(jobOperation);
                    response.addProperty("message", "Procedure Sheet deleted successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                    return response;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            operationLogger.error("Exception deleteProcedureSheet ->" + e);

            response.addProperty("message", "Failed to delete procedure sheet");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
        return response;
    }

    public Object deleteDrawingSheet(Map<String, String> jsonRequest) {
        JsonObject response = new JsonObject();
        try {
            Long operationId = Long.valueOf(jsonRequest.get("id"));
            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(operationId, true);
            if (jobOperation != null) {
                if (jobOperation.getOperationImagePath() != null) {
                    System.out.println("jobOperation.getOperationImagePath()() " + jobOperation.getOperationImagePath());
                    File oldFile = new File("." + jobOperation.getOperationImagePath());
                    if (oldFile.exists()) {
                        System.out.println("Document Deleted");
                        //remove file from local directory
                        if (!oldFile.delete()) {
                            response.addProperty("message", "Failed to delete drawing. Please try again!");
                            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                            return response;
                        }
                    }
                    jobOperation.setOperationImagePath(null);
                    jobOperationRepository.save(jobOperation);
                    response.addProperty("message", "Drawing Sheet deleted successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                    return response;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            operationLogger.error("Exception deleteProcedureSheet ->" + e);

            response.addProperty("message", "Failed to delete drawing sheet");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return response;
        }
        return response;
    }
}
