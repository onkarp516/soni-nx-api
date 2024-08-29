package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.PushMessageRepository;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.PushMessageDTO;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.PushMessage;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PushMessageService {
    @Autowired
    private PushMessageRepository pushMessageRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;
    private static final Logger pushLogger = LoggerFactory.getLogger(PushMessage.class);

    public Object createPushMessage(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            PushMessage pushMessage = new PushMessage();
            pushMessage.setFromDate(LocalDate.parse(jsonRequest.get("fromDate")));
            pushMessage.setToDate(LocalDate.parse(jsonRequest.get("toDate")));
            pushMessage.setMessage(jsonRequest.get("message"));
            pushMessage.setCreatedBy(users.getId());
            pushMessage.setInstitute(users.getInstitute());
            pushMessage.setStatus(true);

            try {
                pushMessageRepository.save(pushMessage);
                response.addProperty("message","Push message created successfully");
                response.addProperty("responseStatus", HttpStatus.OK.value());
            }catch (Exception e){
                pushLogger.error("Exception in createPushMessage " + e);
                e.printStackTrace();
                System.out.println("Exception "+e.getMessage());
                response.addProperty("message","Failed to create push message");
                response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        }catch (Exception e){
            pushLogger.error("Exception in createPushMessage " + e);
            e.printStackTrace();
            System.out.println("Exception  "+e.getMessage());
            response.addProperty("message","Failed to create push message");
            response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    public Object updatePushMessage(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            PushMessage pushMessage = pushMessageRepository.findById(Long.valueOf(jsonRequest.get("id"))).get();

            if(pushMessage != null) {
                pushMessage.setFromDate(LocalDate.parse(jsonRequest.get("fromDate")));
                pushMessage.setToDate(LocalDate.parse(jsonRequest.get("toDate")));
                pushMessage.setMessage(jsonRequest.get("message"));
                pushMessage.setUpdatedBy(users.getId());
                pushMessage.setInstitute(users.getInstitute());
                try {
                    pushMessageRepository.save(pushMessage);
                    response.addProperty("message", "Push message updated successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    pushLogger.error("Exception in updatePushMessage " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    response.addProperty("message", "Failed to update push message");
                    response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }else {
                response.addProperty("message", "Push message not found.");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        }catch (Exception e){
            pushLogger.error("Exception in updatePushMessage " + e);
            e.printStackTrace();
            System.out.println("Exception  "+e.getMessage());
            response.addProperty("message","Failed to update push message");
            response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    public Object findPushMessage(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try{
            PushMessage pushMessage = pushMessageRepository.findById(Long.valueOf(jsonRequest.get("id"))).get();
            if(pushMessage != null){
                JsonObject messageObj = new JsonObject();

                messageObj.addProperty("id",pushMessage.getId());
                messageObj.addProperty("fromDate",pushMessage.getFromDate().toString());
                messageObj.addProperty("toDate",pushMessage.getToDate().toString());
                messageObj.addProperty("message",pushMessage.getMessage());

                response.add("response", messageObj);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            }else{
                response.addProperty("message", "Push message not found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        }catch (Exception e){
            pushLogger.error("Exception in findPushMessage " + e);
            e.printStackTrace();
            System.out.printf("Exception "+e.getMessage());
            response.addProperty("message", "Push message not found");
            response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
        }
        return response;
    }

    public Object deletePushMessage(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            PushMessage pushMessage = pushMessageRepository.findById(Long.valueOf(requestParam.get("id"))).get();

            if(pushMessage != null) {
                pushMessage.setStatus(false);
                pushMessage.setUpdatedBy(users.getId());
                pushMessage.setInstitute(users.getInstitute());
                try {
                    pushMessageRepository.save(pushMessage);
                    response.addProperty("message", "Push message deleted successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    pushLogger.error("Exception in deletePushMessage " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    response.addProperty("message", "Failed to delete push message");
                    response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }else {
                response.addProperty("message", "Push message not found.");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        }catch (Exception e){
            pushLogger.error("Exception in deletePushMessage " + e);
            e.printStackTrace();
            System.out.println("Exception  "+e.getMessage());
            response.addProperty("message","Failed to delete push message");
            response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    public Object DTPushMessage(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<PushMessage> pushMessageList = new ArrayList<>();
        List<PushMessageDTO> pushMessageDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `push_message_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (message LIKE '%" + searchText + "%' OR from_date LIKE '%" + searchText + "%' OR" +
                        " to_date LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, PushMessage.class);
            Query q1 = entityManager.createNativeQuery(query1, PushMessage.class);

            pushMessageList = q.getResultList();
            System.out.println("Limit total rows " + pushMessageList.size());

            for(PushMessage pushMessage: pushMessageList){
                PushMessageDTO pushMessageDTO = new PushMessageDTO();
                pushMessageDTO.setId(pushMessage.getId());
                pushMessageDTO.setFromDate(pushMessage.getFromDate().toString());
                pushMessageDTO.setToDate(pushMessage.getToDate().toString());
                pushMessageDTO.setMessage(pushMessage.getMessage());
                pushMessageDTO.setCreatedAt(pushMessage.getCreatedAt().toString());

                pushMessageDTOList.add(pushMessageDTO);
            }

            List<PushMessage> pushMessageArrayList = new ArrayList<>();
            pushMessageArrayList = q1.getResultList();
            System.out.println("total rows " + pushMessageArrayList.size());

            genericDTData.setRows(pushMessageDTOList);
            genericDTData.setTotalRows(pushMessageArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(pushMessageDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    public Object getPushMessageForMobileApp(HttpServletRequest request) {
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try{
            LocalDate localDate = LocalDate.now();
            PushMessage pushMessage = pushMessageRepository.getMessageByDateAndInstituteId(localDate, true, employee.getInstitute().getId());
            if(pushMessage != null){
                response.addProperty("message", pushMessage.getMessage());
                response.addProperty("responseStatus", HttpStatus.OK.value());
            }else{
                response.addProperty("message", "Data not found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Exception in getPushMessageForMobileApp "+e.getMessage());
            response.addProperty("message", "Failed to get data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
