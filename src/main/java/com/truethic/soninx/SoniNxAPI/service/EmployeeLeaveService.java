package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeLeaveRepository;
import com.truethic.soninx.SoniNxAPI.repository.LeaveTypeRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.EmployeeLeaveDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.EmployeeLeave;
import com.truethic.soninx.SoniNxAPI.model.LeaveType;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.views.EmployeeLeaveView;
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
public class EmployeeLeaveService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EmployeeLeaveRepository employeeLeaveRepository;
    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    public Object applyLeave(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        EmployeeLeave employeeLeave = new EmployeeLeave();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            EmployeeLeave employeeLeave1 =
                    employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                            employee.getId(), LocalDate.parse(requestParam.get("fromDate")), LocalDate.parse(requestParam.get("toDate")));
            if (employeeLeave1 == null) {
                Long leaveTypeId = Long.valueOf(requestParam.get("leaveTypeId"));
                LeaveType leaveType = leaveTypeRepository.findByIdAndStatus(leaveTypeId, true);
                employeeLeave.setEmployee(employee);
                employeeLeave.setInstitute(employee.getInstitute());
                employeeLeave.setLeaveType(leaveType);
                employeeLeave.setCreatedBy(employee.getId());
                employeeLeave.setFromDate(LocalDate.parse(requestParam.get("fromDate")));
                employeeLeave.setToDate(LocalDate.parse(requestParam.get("toDate")));
                employeeLeave.setTotalDays(Integer.valueOf(requestParam.get("totalDays")));
                employeeLeave.setReason(requestParam.get("reason"));
                employeeLeave.setLeaveStatus("Pending");
                employeeLeave.setStatus(true);
                employeeLeave.setInstitute(employee.getInstitute());
                try {
                    employeeLeaveRepository.save(employeeLeave);
                    responseMessage.setMessage("leave applied successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to apply leave");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Invalid leave dates");
                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to apply leave");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject listOfLeaves(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            List<EmployeeLeave> employeeLeaveList =
                    employeeLeaveRepository.findByEmployeeIdAndStatusOrderByIdDesc(employee.getId(), true);
            for (EmployeeLeave employeeLeave : employeeLeaveList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", employeeLeave.getId());
                object.addProperty("leaveName", employeeLeave.getLeaveType().getName());
                object.addProperty("fromDate", String.valueOf(employeeLeave.getFromDate()));
                object.addProperty("toDate", String.valueOf(employeeLeave.getToDate()));
                object.addProperty("totalDays", employeeLeave.getTotalDays());
                object.addProperty("reason", employeeLeave.getReason());
                object.addProperty("leaveStatus", employeeLeave.getLeaveStatus());
                object.addProperty("requestDate", String.valueOf(employeeLeave.getCreatedAt()));
                object.addProperty("leaveApprovedBy", employeeLeave.getLeaveApprovedBy());
                object.addProperty("leaveRemark", employeeLeave.getLeaveRemark());
                jsonArray.add(object);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTEmployeeLeaves(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<EmployeeLeaveView> employeeLeaveList = new ArrayList<>();
        List<EmployeeLeaveDTO> employeeLeaveDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM employee_leave_view WHERE employee_leave_view.status=1 AND employee_leave_view.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (full_name LIKE '%" + searchText + "%' OR leave_name LIKE '%" + searchText +
                        "%' OR from_date LIKE '%" + searchText + "%' OR leave_approved_by LIKE '%" + searchText + "%'" +
                        " OR leave_remark LIKE '%" + searchText + "%' OR leave_status LIKE '%" + searchText + "%' OR" +
                        " reason LIKE '%" + searchText + "%' OR to_date LIKE '%" + searchText + "%' OR total_days LIKE" +
                        " '%" + searchText + "%' )";
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

            Query q = entityManager.createNativeQuery(query, EmployeeLeaveView.class);
            Query q1 = entityManager.createNativeQuery(query1, EmployeeLeaveView.class);

            employeeLeaveList = q.getResultList();
            System.out.println("Limit total rows " + employeeLeaveList.size());

            if (employeeLeaveList.size() > 0) {
                for (EmployeeLeaveView employeeLeave : employeeLeaveList) {
                    employeeLeaveDTOList.add(convertEmployeeLeaveToEmployeeLeaveDTO(employeeLeave));
                }
            }

            List<EmployeeLeaveView> leaveViewArrayList = new ArrayList<>();
            leaveViewArrayList = q1.getResultList();
            System.out.println("total rows " + leaveViewArrayList.size());

            genericDTData.setRows(employeeLeaveDTOList);
            genericDTData.setTotalRows(leaveViewArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(employeeLeaveDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private EmployeeLeaveDTO convertEmployeeLeaveToEmployeeLeaveDTO(EmployeeLeaveView employeeLeave) {
        EmployeeLeaveDTO employeeLeaveDTO = new EmployeeLeaveDTO();
        employeeLeaveDTO.setId(employeeLeave.getId());
        employeeLeaveDTO.setEmployeeName(employeeLeave.getFullName());
        employeeLeaveDTO.setLeaveName(employeeLeave.getLeaveName());
        employeeLeaveDTO.setReason(employeeLeave.getReason());
        employeeLeaveDTO.setFromDate(String.valueOf(employeeLeave.getFromDate()));
        employeeLeaveDTO.setToDate(String.valueOf(employeeLeave.getToDate()));
        employeeLeaveDTO.setTotalDays(employeeLeave.getTotalDays());
        employeeLeaveDTO.setLeaveStatus(employeeLeave.getLeaveStatus());
        employeeLeaveDTO.setLeaveApprovedBy(employeeLeave.getLeaveApprovedBy());
        employeeLeaveDTO.setLeaveRemark(employeeLeave.getLeaveRemark());
        employeeLeaveDTO.setStatus(employeeLeave.getStatus());
        employeeLeaveDTO.setCreatedAt(String.valueOf(employeeLeave.getCreatedAt()));
        employeeLeaveDTO.setCreatedBy(employeeLeave.getCreatedBy());
//        employeeLeaveDTO.setInstitute(employeeLeave.getInstituteId());
        return employeeLeaveDTO;
    }

    public Object updateEmployeeLeaveStatus(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long leaveId = Long.valueOf(jsonRequest.get("leaveId"));
            String leaveStatus = jsonRequest.get("leaveStatus");
            String remark = jsonRequest.get("remark");
            EmployeeLeave employeeLeave = employeeLeaveRepository.findByIdAndStatus(leaveId, true);
            if (employeeLeave != null) {
                if (!remark.isEmpty()) {
                    employeeLeave.setLeaveRemark(remark);
                }
                employeeLeave.setLeaveApprovedBy(users.getUsername());
                employeeLeave.setLeaveStatus(leaveStatus);
                employeeLeave.setUpdatedBy(users.getId());
                employeeLeave.setUpdatedAt(LocalDateTime.now());
                employeeLeave.setInstitute(users.getInstitute());
                employeeLeaveRepository.save(employeeLeave);
                responseMessage.setMessage("Request " + leaveStatus + " successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Leave request not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to updated request");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public JsonObject checkLeaveAvailability(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Long leavesAllowed = 0L;
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            Long categoryId = Long.parseLong(requestParam.get("categoryId"));
            Long noOfDays = Long.parseLong(requestParam.get("days"));
            Long usedLeaves = leaveTypeRepository.getLeavesAlreadyApplied(employee.getId(), categoryId);
            LeaveType leaveType = leaveTypeRepository.findByIdAndStatus(categoryId, true);
            if(leaveType != null)
                leavesAllowed = leaveType.getLeavesAllowed();
            if(usedLeaves < leavesAllowed){
                Long remainingLeaves = leavesAllowed - usedLeaves;
                if(noOfDays > remainingLeaves){
                    if(remainingLeaves > 1)
                        responseMessage.addProperty("message", "You can apply leaves for "+remainingLeaves+" days");
                    else
                        responseMessage.addProperty("message", "You can apply leave for "+remainingLeaves+" day");
                    responseMessage.addProperty("flag",false);
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } else {
                    responseMessage.addProperty("message", "Proceed to apply the leave");
                    responseMessage.addProperty("flag",true);
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                }
            } else {
                responseMessage.addProperty("message", "Leaves Exhausted");
                responseMessage.addProperty("flag",false);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }
}
