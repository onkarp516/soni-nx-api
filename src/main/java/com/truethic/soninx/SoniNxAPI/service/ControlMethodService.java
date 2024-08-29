package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.ControlMethodRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.ControlMethod;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ControlMethodService {
    private static final Logger controlLOGGER = LoggerFactory.getLogger(ControlMethodService.class);
    @Autowired
    private ControlMethodRepository controlMethodRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EntityManager entityManager;

    public JsonObject createControlMethod(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            ControlMethod controlMethod = new ControlMethod();
            controlMethod.setControlMethodLabel(request.getParameter("controlMethodLabel"));
            controlMethod.setStatus(true);
            controlMethod.setCreatedBy(users.getId());
            controlMethod.setCreatedAt(LocalDateTime.now());
            controlMethod.setInstitute(users.getInstitute());
            controlMethodRepository.save(controlMethod);

            response.addProperty("message", "Control method created successfully");
            response.addProperty("responseStatus", HttpStatus.OK.value());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            controlLOGGER.error("Exception -> createControlMethod " + e);

            response.addProperty("message", "Failed to create control method");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object DTControlMethod(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<ControlMethod> controlMethodList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `control_method_tbl` WHERE status=1  AND institute_id="+users.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND control_method_label LIKE '%" + searchText + "%'";
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

            Query q = entityManager.createNativeQuery(query, ControlMethod.class);
            Query q1 = entityManager.createNativeQuery(query1, ControlMethod.class);

            controlMethodList = q.getResultList();
            System.out.println("Limit total rows " + controlMethodList.size());

            List<ControlMethod> controlMethodArrayList = new ArrayList<>();
            controlMethodArrayList = q1.getResultList();
            System.out.println("total rows " + controlMethodArrayList.size());

            genericDTData.setRows(controlMethodList);
            genericDTData.setTotalRows(controlMethodArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(controlMethodList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    public Object findControlMethod(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        ControlMethod controlMethod = controlMethodRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
        if (controlMethod != null) {
            responseMessage.setResponse(controlMethod);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object updateControlMethod(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        ControlMethod controlMethod = controlMethodRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (controlMethod != null) {
            controlMethod.setControlMethodLabel(requestParam.get("controlMethodLabel"));
            controlMethod.setStatus(true);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            controlMethod.setUpdatedBy(user.getId());
            controlMethod.setUpdatedAt(LocalDateTime.now());
            controlMethod.setInstitute(user.getInstitute());
            try {
                controlMethodRepository.save(controlMethod);
                responseObject.setMessage("Control method updated successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to update job");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }

    public Object deleteControlMethod(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            ControlMethod controlMethod = controlMethodRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (controlMethod != null) {
                controlMethod.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                controlMethod.setCreatedBy(user.getId());
                controlMethod.setInstitute(user.getInstitute());
                try {
                    controlMethodRepository.save(controlMethod);
                    responseObject.setMessage("Control method deleted successfully");
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
            responseObject.setMessage("Failed to delete control method");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object getControlMethod() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<ControlMethod> controlMethodList = controlMethodRepository.findAllByStatus(true);
            responseMessage.setResponse(controlMethodList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }
}
