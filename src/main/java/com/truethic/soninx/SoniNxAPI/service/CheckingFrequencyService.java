package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.CheckingFrequencyRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.CheckingFrequency;
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
public class CheckingFrequencyService {
    @Autowired
    private static final Logger freqLOGGER = LoggerFactory.getLogger(CheckingFrequencyService.class);
    @Autowired
    private CheckingFrequencyRepository frequencyRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private EntityManager entityManager;

    public JsonObject createCheckingFrequency(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            CheckingFrequency checkingFrequency = new CheckingFrequency();
            checkingFrequency.setCheckingFrequencyLabel(request.getParameter("checkingFrequencyLabel"));
            checkingFrequency.setStatus(true);
            checkingFrequency.setCreatedBy(users.getId());
            checkingFrequency.setCreatedAt(LocalDateTime.now());
            checkingFrequency.setInstitute(users.getInstitute());
            frequencyRepository.save(checkingFrequency);

            response.addProperty("message", "Checking frequency created successfully");
            response.addProperty("responseStatus", HttpStatus.OK.value());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            freqLOGGER.error("Exception -> createCheckingFrequency " + e);

            response.addProperty("message", "Failed to create checking frequency");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object DTCheckingFrequency(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<CheckingFrequency> checkingFrequencyList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `checking_frequency_tbl` WHERE status=1 AND checking_frequency_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND checking_frequency_label LIKE '%" + searchText + "%'";
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

            Query q = entityManager.createNativeQuery(query, CheckingFrequency.class);
            Query q1 = entityManager.createNativeQuery(query1, CheckingFrequency.class);

            checkingFrequencyList = q.getResultList();
            System.out.println("Limit total rows " + checkingFrequencyList.size());

            List<CheckingFrequency> checkingFrequencyArrayListc = new ArrayList<>();
            checkingFrequencyArrayListc = q1.getResultList();
            System.out.println("total rows " + checkingFrequencyArrayListc.size());

            genericDTData.setRows(checkingFrequencyList);
            genericDTData.setTotalRows(checkingFrequencyArrayListc.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(checkingFrequencyList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    public Object findCheckingFrequency(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        CheckingFrequency checkingFrequency = frequencyRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
        if (checkingFrequency != null) {
            responseMessage.setResponse(checkingFrequency);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object updateCheckingFrequency(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        CheckingFrequency checkingFrequency = frequencyRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (checkingFrequency != null) {
            checkingFrequency.setCheckingFrequencyLabel(requestParam.get("checkingFrequencyLabel"));
            checkingFrequency.setStatus(true);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            checkingFrequency.setUpdatedBy(user.getId());
            checkingFrequency.setUpdatedAt(LocalDateTime.now());
            checkingFrequency.setInstitute(user.getInstitute());
            try {
                frequencyRepository.save(checkingFrequency);
                responseObject.setMessage("Checking frequency updated successfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseObject.setMessage("Failed to update checking frequency");
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
            }
        } else {
            responseObject.setMessage("Data not found");
            responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseObject;
    }

    public Object deleteCheckingFrequency(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            CheckingFrequency checkingFrequency = frequencyRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (checkingFrequency != null) {
                checkingFrequency.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                checkingFrequency.setCreatedBy(user.getId());
                checkingFrequency.setInstitute(user.getInstitute());
                try {
                    frequencyRepository.save(checkingFrequency);
                    responseObject.setMessage("Checking frequency deleted successfully");
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
            responseObject.setMessage("Failed to delete checking frequency");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object getCheckingFrequency() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<CheckingFrequency> checkingFrequencyList = frequencyRepository.findAllByStatus(true);
            responseMessage.setResponse(checkingFrequencyList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }
}
