package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.*;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeRepository;
import com.truethic.soninx.SoniNxAPI.repository.ShiftAssignRepository;
import com.truethic.soninx.SoniNxAPI.repository.ShiftRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.ShiftAssignDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.Shift;
import com.truethic.soninx.SoniNxAPI.model.ShiftAssign;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ShiftAssignService {
    private static final Logger shiftAssignLogger = LoggerFactory.getLogger(ShiftAssignService.class);
    @Autowired
    private ShiftAssignRepository shiftAssignRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private Utility utility;

    public Object employeeWiseShiftAssign(HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            String jsonToStr1 = request.getParameter("employeeList");
            JsonArray empArray = new JsonParser().parse(jsonToStr1).getAsJsonArray();
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            List<ShiftAssign> shiftAssigns = new ArrayList<>();
            for (JsonElement jsonElement : empArray) {
                JsonObject object = jsonElement.getAsJsonObject();
                if (object.get("id").getAsString() != null) {
                    Employee employee = employeeRepository.findByIdAndStatus(object.get("id").getAsLong(), true);
                    if (employee != null) {
                        ShiftAssign shiftAssign = new ShiftAssign();
                        Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(request.getParameter("shiftId")), true);
                        shiftAssign.setFromDate(LocalDate.parse(request.getParameter("fromDate")));
                        shiftAssign.setToDate(LocalDate.parse(request.getParameter("toDate")));
                        shiftAssign.setShift(shift);
                        shiftAssign.setEmployee(employee);
                        shiftAssign.setCreatedBy(users.getId());
                        shiftAssign.setInstitute(users.getInstitute());
                        shiftAssign.setCreatedAt(LocalDateTime.now());
                        shiftAssign.setStatus(true);
                        shiftAssigns.add(shiftAssign);
                    }
                }
            }
            try {
                shiftAssignRepository.saveAll(shiftAssigns);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
                responseMessage.setMessage("Shift Assign Successfully");
            } catch (Exception e) {
                System.out.println("Exception" + e.getMessage());
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.setMessage("Internal Server Error");
            }
        } catch (Exception e) {
            System.out.println("Error ---> " + e);
            e.printStackTrace();
        }
        return responseMessage;
    }

    public JsonObject getEmployeeWiseShiftAssign(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<ShiftAssign> shiftAssigns = shiftAssignRepository.findAllByStatus(true);
            if (shiftAssigns.size() > 0) {
                for (ShiftAssign shiftAssign : shiftAssigns) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("id", shiftAssign.getId());
                    jsonObject.addProperty("fromDate", String.valueOf(shiftAssign.getFromDate()));
                    jsonObject.addProperty("toDate", String.valueOf(shiftAssign.getToDate()));
                    jsonObject.addProperty("employeeName", utility.getEmployeeName(shiftAssign.getEmployee()));
                    jsonObject.addProperty("shiftName", shiftAssign.getShift().getName());
                    jsonArray.add(jsonObject);
                }
                response.add("response", jsonArray);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;

    }


    private ShiftAssignDTO convertToDTO(ShiftAssign shiftAssign) {
        ShiftAssignDTO shiftAssignDTO = new ShiftAssignDTO();
        shiftAssignDTO.setId(shiftAssign.getId());
        shiftAssignDTO.setEmployeeName(utility.getEmployeeName(shiftAssign.getEmployee()));
        shiftAssignDTO.setShiftName(shiftAssign.getShift().getName());
        shiftAssignDTO.setFromDate(shiftAssign.getFromDate().toString());
        shiftAssignDTO.setToDate(shiftAssign.getToDate().toString());
        shiftAssignDTO.setCreatedBy(shiftAssign.getCreatedBy());
        shiftAssignDTO.setUpdatedBy(shiftAssign.getUpdatedBy());
        return shiftAssignDTO;
    }

    public Object DTShiftAssign(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.valueOf((request.get("from")));
        Integer to = Integer.valueOf((request.get("to")));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));

        GenericDTData genericDTData = new GenericDTData();
        List<ShiftAssign> shiftAssignList = new ArrayList<>();
        List<ShiftAssignDTO> shiftAssignDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `shift_assign_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND from_date LIKE '%" + searchText + "%'";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") && jsonObject.get("colId").toString() != null) {
//                   System.out.println(" ORDER BY " + jsonObject.getString("colId"));
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, ShiftAssign.class);
            Query q1 = entityManager.createNativeQuery(query1, ShiftAssign.class);

            shiftAssignList = q.getResultList();
            System.out.println("Limit total rows " + shiftAssignList.size());

            for (ShiftAssign shiftAssign : shiftAssignList) {
                shiftAssignDTOList.add(convertToDTO(shiftAssign));
            }
            List<ShiftAssign> shiftAssignArrayList = new ArrayList<>();
            shiftAssignArrayList = q1.getResultList();
            System.out.println("total rows " + shiftAssignArrayList.size());

            genericDTData.setRows(shiftAssignDTOList);
            genericDTData.setTotalRows(shiftAssignArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(shiftAssignDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    public JsonObject getNonShiftEmployee(HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request .getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            String fromDate = request.getParameter("fromDate");
            String toDate = request.getParameter("toDate");
            List<Employee> employeeList = employeeRepository.findEmployeeIdAndStatus(fromDate, toDate, true, users.getInstitute().getId());

            System.out.println("employeeList.size() " + employeeList.size());

            if (employeeList.size() > 0) {
                for (Employee employee : employeeList) {
                    JsonObject empObject = new JsonObject();
                    empObject.addProperty("id", employee.getId());
                    empObject.addProperty("shiftName", employee.getShift().getName());
                    empObject.addProperty("firstName", employee.getFirstName());
                    empObject.addProperty("middleName", employee.getMiddleName());
                    empObject.addProperty("lastName", employee.getLastName());
                    jsonArray.add(empObject);
                }
                response.add("response", jsonArray);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;

    }

    public Object deleteEmployeeShiftAssign(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long id = Long.valueOf(request.getParameter("id"));

            ShiftAssign shiftAssign = shiftAssignRepository.findByIdAndStatus(id, true);
            if (shiftAssign != null) {
                shiftAssignRepository.deleteEmployeeShift(id);
                response.addProperty("message", "Employee Shift Deleted Successfully");
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());

            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());

        }
        return response;
    }

    public Object findEmployeeShift(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long id = Long.valueOf(request.getParameter("id"));
            ShiftAssign shiftAssign = shiftAssignRepository.findByIdAndStatus(id, true);
            if (shiftAssign != null) {
                JsonObject fObject = new JsonObject();
                fObject.addProperty("id", shiftAssign.getId());
                fObject.addProperty("fromDate", shiftAssign.getFromDate().toString());
                fObject.addProperty("toDate", shiftAssign.getToDate().toString());
                fObject.addProperty("shiftId", shiftAssign.getShift().getId());
                fObject.addProperty("employeeId", shiftAssign.getEmployee().getId());
                fObject.addProperty("employeeName", utility.getEmployeeName(shiftAssign.getEmployee()));
                response.add("response", fObject);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object updateEmployeeShift(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();

        Long id = Long.valueOf(request.getParameter("id"));
        ShiftAssign shiftAssign = shiftAssignRepository.findByIdAndStatus(id, true);
        if (shiftAssign != null) {
            Employee employee = employeeRepository.findByIdAndStatus(Long.parseLong(request.getParameter("employeeId")), true);
            Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(request.getParameter("shiftId")), true);
            shiftAssign.setShift(shift);
            shiftAssign.setEmployee(employee);
            shiftAssign.setFromDate(LocalDate.parse(request.getParameter("fromDate")));
            shiftAssign.setToDate(LocalDate.parse(request.getParameter("toDate")));

            try {
                shiftAssignRepository.save(shiftAssign);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                responseMessage.addProperty("message", "Shift Assign Updated Successfully");
            } catch (Exception e) {
                e.printStackTrace();
                responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.addProperty("responseStatus", "Internal Server Error");
            }
        } else {
            responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            responseMessage.addProperty("message", "Data Not Found");
        }
        return responseMessage;
    }

    public JsonObject getNextDayShiftOfEmployee(HttpServletRequest request) {
        JsonObject jsonObject = new JsonObject();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            if (employee != null) {
                LocalDate nextDay = LocalDate.now().plusDays(1);
                String shiftName = shiftAssignRepository.getEmployeeNextDayShiftName(employee.getId(), nextDay);
                System.out.println("nextDay =>" + nextDay + " shiftName=>" + shiftName);
                jsonObject.addProperty("response", shiftName != null ? shiftName : "NA");
                jsonObject.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                jsonObject.addProperty("message", "Employee not found");
                jsonObject.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            jsonObject.addProperty("message", " Failed to load data");
            jsonObject.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return jsonObject;
    }
}
