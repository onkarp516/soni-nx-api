package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.WorkBreakRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.WorkBreakDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.model.WorkBreak;
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
public class WorkBreakService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private WorkBreakRepository workBreakRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public Object createBreak(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        WorkBreak workBreak = new WorkBreak();
        workBreak.setBreakName(requestParam.get("breakName"));
//        workBreak.setIsBreakPaid(Boolean.parseBoolean(requestParam.get("isBreakPaid")));
        workBreak.setStatus(true);
        workBreak.setCreatedBy(users.getId());
        workBreak.setCreatedAt(LocalDateTime.now());
        workBreak.setInstitute(users.getInstitute());
        try {
            workBreakRepository.save(workBreak);
            responseMessage.setMessage("Work break created successfully");
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception e");
            responseMessage.setMessage("Failed to create work break");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTBreak(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<WorkBreak> workBreakList = new ArrayList<>();
        List<WorkBreakDTDTO> workBreakDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT id ,break_name,is_break_paid, created_at, created_by, updated_at, updated_by, institute_id, " +
                    "status FROM `break_tbl` WHERE break_tbl.status=1 AND break_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND break_name LIKE '%" + searchText + "%'";
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
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, WorkBreak.class);
            Query q1 = entityManager.createNativeQuery(query1, WorkBreak.class);

            workBreakList = q.getResultList();
            System.out.println("Limit total rows " + workBreakList.size());

            for (WorkBreak workBreak : workBreakList) {
                workBreakDTDTOList.add(convertToDTDTO(workBreak));
            }

            List<WorkBreak> workBreakArrayList = new ArrayList<>();
            workBreakArrayList = q1.getResultList();
            System.out.println("total rows " + workBreakArrayList.size());

            genericDTData.setRows(workBreakDTDTOList);
            genericDTData.setTotalRows(workBreakArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(workBreakDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private WorkBreakDTDTO convertToDTDTO(WorkBreak workBreak) {
        WorkBreakDTDTO workBreakDTDTO = new WorkBreakDTDTO();
        workBreakDTDTO.setId(workBreak.getId());
        workBreakDTDTO.setBreakName(workBreak.getBreakName());
        workBreakDTDTO.setIsBreakPaid(workBreak.getIsBreakPaid());
        workBreakDTDTO.setStatus(workBreak.getStatus());
        workBreakDTDTO.setCreatedAt(String.valueOf(workBreak.getCreatedAt()));
        if (workBreak.getUpdatedAt() != null)
            workBreakDTDTO.setUpdatedAt(String.valueOf(workBreak.getUpdatedAt()));
        return workBreakDTDTO;
    }

    public Object findBreak(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long id = Long.parseLong(request.get("id"));
        try {
            WorkBreak workBreak = workBreakRepository.findByIdAndStatus(id, true);
            if (workBreak != null) {
                responseMessage.setResponse(workBreak);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object updateBreak(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long id = Long.parseLong(requestParam.get("id"));
        try {
            WorkBreak workBreak = workBreakRepository.findByIdAndStatus(id, true);
            if (workBreak != null) {
                Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                workBreak.setBreakName(requestParam.get("breakName"));
//                workBreak.setIsBreakPaid(Boolean.parseBoolean(requestParam.get("isBreakPaid")));
                workBreak.setStatus(true);
                workBreak.setUpdatedBy(users.getId());
                workBreak.setUpdatedAt(LocalDateTime.now());
                workBreak.setInstitute(users.getInstitute());
                try {
                    workBreakRepository.save(workBreak);
                    responseMessage.setMessage("Work break updated successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception e");
                    responseMessage.setMessage("Failed to create work break");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object deleteBreak(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long id = Long.parseLong(requestParam.get("id"));
        try {
            WorkBreak workBreak = workBreakRepository.findByIdAndStatus(id, true);
            if (workBreak != null) {
                Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                workBreak.setStatus(false);
                workBreak.setUpdatedBy(users.getId());
                workBreak.setUpdatedAt(LocalDateTime.now());
                workBreak.setInstitute(users.getInstitute());
                try {
                    workBreakRepository.save(workBreak);
                    responseMessage.setMessage("Work break deleted successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception e");
                    responseMessage.setMessage("Failed to create work break");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public JsonObject listForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            List<WorkBreak> workBreakList = workBreakRepository.findAllByInstituteIdAndStatus(employee.getInstitute().getId(),true);
            for (WorkBreak workBreak : workBreakList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", workBreak.getId());
                object.addProperty("breakName", workBreak.getBreakName());
                object.addProperty("isPaid", workBreak.getIsBreakPaid());

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

    public JsonObject workBreakListForSelection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            List<WorkBreak> workBreakList = workBreakRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(),true);
            for (WorkBreak workBreak : workBreakList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", workBreak.getId());
                object.addProperty("breakName", workBreak.getBreakName());
                object.addProperty("isPaid", workBreak.getIsBreakPaid());

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
}
