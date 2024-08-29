package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.LeaveTypeRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.LeaveTypeDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.LeaveType;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LeaveTypeService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    public Object createLeaveType(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        LeaveType leaveType = new LeaveType();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            leaveType.setName(requestParam.get("name"));
            leaveType.setIsPaid(Boolean.valueOf(requestParam.get("isPaid")));
            leaveType.setLeavesAllowed(Long.valueOf(requestParam.get("leavesAllowed")));
            leaveType.setCreatedBy(users.getId());
            leaveType.setInstitute(users.getInstitute());
            leaveType.setStatus(true);
            try {
                leaveTypeRepository.save(leaveType);
                responseMessage.setMessage("Leave Type created successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to create leave type");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to create leave type");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object DTLeaveType(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<LeaveType> leaveTypeList = new ArrayList<>();
        List<LeaveTypeDTO> leaveTypeDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `leave_type_master_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND name LIKE '%" + searchText + "%'";
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

            Query q = entityManager.createNativeQuery(query, LeaveType.class);
            Query q1 = entityManager.createNativeQuery(query1, LeaveType.class);

            leaveTypeList = q.getResultList();
            System.out.println("Limit total rows " + leaveTypeList.size());

            for (LeaveType leaveType : leaveTypeList) {
                leaveTypeDTOList.add(convertToDTDTO(leaveType));
            }

            List<LeaveType> leaveTypeArrayList = new ArrayList<>();
            leaveTypeArrayList = q1.getResultList();
            System.out.println("total rows " + leaveTypeArrayList.size());

            genericDTData.setRows(leaveTypeDTOList);
            genericDTData.setTotalRows(leaveTypeArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(leaveTypeDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private LeaveTypeDTO convertToDTDTO(LeaveType leaveType) {
        LeaveTypeDTO leaveTypeDTO = new LeaveTypeDTO();
        leaveTypeDTO.setId(leaveType.getId());
        leaveTypeDTO.setName(leaveType.getName());
        leaveTypeDTO.setIsPaid(leaveType.getIsPaid());
        leaveTypeDTO.setLeavesAllowed(leaveType.getLeavesAllowed());
        leaveTypeDTO.setStatus(leaveType.getStatus());
        leaveTypeDTO.setCreatedAt(String.valueOf(leaveType.getCreatedAt()));
        if (leaveType.getUpdatedAt() != null)
            leaveTypeDTO.setUpdatedAt(String.valueOf(leaveType.getUpdatedAt()));
        return leaveTypeDTO;
    }

    public Object findLeaveType(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        LeaveType leaveType = leaveTypeRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (leaveType != null) {
            responseMessage.setResponse(leaveType);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object updateLeaveType(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long id = Long.valueOf(requestParam.get("id"));
        LeaveType leaveType = leaveTypeRepository.findByIdAndStatus(id, true);
        if (leaveType != null) {
            try {
                Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                leaveType.setName(requestParam.get("name"));
                leaveType.setIsPaid(Boolean.valueOf(requestParam.get("isPaid")));
                leaveType.setLeavesAllowed(Long.valueOf(requestParam.get("leavesAllowed")));
                leaveType.setUpdatedBy(users.getId());
                leaveType.setInstitute(users.getInstitute());
                leaveType.setUpdatedAt(LocalDateTime.now());
                leaveType.setStatus(true);
                try {
                    leaveTypeRepository.save(leaveType);
                    responseMessage.setMessage("Leave Type updated successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to update leave type");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to update leave type");
                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            }
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object deleteLeaveType(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        LeaveType leaveType = leaveTypeRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (leaveType != null) {
            leaveType.setStatus(false);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            leaveType.setUpdatedBy(user.getId());
            leaveType.setInstitute(user.getInstitute());
            leaveType.setUpdatedAt(LocalDateTime.now());
            try {
                leaveTypeRepository.save(leaveType);
                responseObject.setMessage("Leave Type deleted successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to delete leave type");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }

    public JsonObject listForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            List<LeaveType> leaveTypeList = leaveTypeRepository.findAllByInstituteIdAndStatus(employee.getInstitute().getId(),true);
            for (LeaveType leaveType : leaveTypeList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", leaveType.getId());
                object.addProperty("name", leaveType.getName());
                object.addProperty("isPaid", leaveType.getIsPaid());

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

    public JsonObject leavesDashboard(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            List<Object[]> leaveCountList = leaveTypeRepository.getEmployeeLeavesDashboardData(employee.getId());
            for (int i = 0; i < leaveCountList.size(); i++) {
                Object[] obj = leaveCountList.get(i);
                JsonObject leaveObject = new JsonObject();
                leaveObject.addProperty("name", obj[0].toString());
                leaveObject.addProperty("id", obj[1].toString());
                leaveObject.addProperty("leaves_allowed", obj[2].toString());
                leaveObject.addProperty("usedleaves", obj[3].toString());
                jsonArray.add(leaveObject);
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
}
