package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.model.Payhead;
import com.truethic.soninx.SoniNxAPI.repository.DeductionRepository;
import com.truethic.soninx.SoniNxAPI.repository.PayheadRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.DeductionDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Deduction;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
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
public class DeductionService {
    @Autowired
    private DeductionRepository deductionRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    Utility utility;
    @Autowired
    private PayheadRepository payheadRepository;

    public Object createDeduction(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Deduction deduction = new Deduction();
            deduction.setName(requestParam.get("deductionName"));
            if(requestParam.containsKey("percentage") && !requestParam.get("percentage").equalsIgnoreCase(""))
                deduction.setPercentage(Double.parseDouble(requestParam.get("percentage")));
            deduction.setDeductionStatus(Boolean.parseBoolean(requestParam.get("deductionStatus")));
            if (requestParam.containsKey("payheadId") && requestParam.get("payheadId") != null) {
                Long payheadId = Long.valueOf(requestParam.get("payheadId"));
                Payhead payhead1 = payheadRepository.findByIdAndStatus(payheadId, true);
                deduction.setPercentageOf(payhead1);
            }
//            deduction.setAmount(Double.valueOf(requestParam.get("amount")));
                deduction.setDeductionSlug(utility.getKeyName(requestParam.get("deductionName"),false));
            deduction.setStatus(true);
            deduction.setCreatedAt(LocalDateTime.now());
            deduction.setCreatedBy(users.getId());
            deduction.setInstitute(users.getInstitute());
            try {
                deductionRepository.save(deduction);
                responseMessage.setMessage("Deduction created successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to create deduction");
                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to create deduction");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTDeduction(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Deduction> deductionList = new ArrayList<>();
        List<DeductionDTDTO> deductionDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `deduction_tbl` WHERE deduction_tbl.status=1 AND deduction_tbl.institute_id="+users.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (name LIKE '%" + searchText + "%' OR amount LIKE '%" +
                        searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, Deduction.class);
            Query q1 = entityManager.createNativeQuery(query1, Deduction.class);

            deductionList = q.getResultList();
            System.out.println("Limit total rows " + deductionList.size());

            for (Deduction deduction : deductionList) {
                deductionDTDTOList.add(convertToDTDTO(deduction));
            }

            List<Deduction> deductionArrayList = new ArrayList<>();
            deductionArrayList = q1.getResultList();
            System.out.println("total rows " + deductionArrayList.size());

            genericDTData.setRows(deductionDTDTOList);
            genericDTData.setTotalRows(deductionArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            genericDTData.setRows(deductionDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private DeductionDTDTO convertToDTDTO(Deduction deduction) {
        DeductionDTDTO deductionDTDTO = new DeductionDTDTO();
        deductionDTDTO.setId(deduction.getId());
        deductionDTDTO.setName(deduction.getName());
        deductionDTDTO.setAmount(deduction.getAmount());
        deductionDTDTO.setDeductionStatus(deduction.getDeductionStatus());
        deductionDTDTO.setStatus(deduction.getStatus());
        deductionDTDTO.setCreatedAt(String.valueOf(deduction.getCreatedAt()));
        deductionDTDTO.setUpdatedAt(String.valueOf(deduction.getUpdatedAt()));
        deductionDTDTO.setPercentage(deduction.getPercentage());
        deductionDTDTO.setPayheadParentId(deduction.getPercentageOf() != null ? deduction.getPercentageOf().getId() : null);
        deductionDTDTO.setPayheadParentName(deduction.getPercentageOf() != null ? deduction.getPercentageOf().getName() : "");
        return deductionDTDTO;
    }

    public Object updateDeduction(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long id = Long.valueOf(requestParam.get("id"));
            Deduction deduction = deductionRepository.findByIdAndStatus(id, true);
            deduction.setName(requestParam.get("deductionName"));
            if(requestParam.containsKey("percentage") && !requestParam.get("percentage").equalsIgnoreCase(""))
                deduction.setPercentage(Double.parseDouble(requestParam.get("percentage")));
            deduction.setDeductionStatus(Boolean.parseBoolean(requestParam.get("deductionStatus")));
//            deduction.setAmount(Double.valueOf(requestParam.get("amount")));
            if (requestParam.containsKey("payheadId") && !requestParam.get("payheadId").equalsIgnoreCase("")) {
                Long payheadId = Long.valueOf(requestParam.get("payheadId"));
                Payhead payhead1 = payheadRepository.findByIdAndStatus(payheadId, true);
                deduction.setPercentageOf(payhead1);
            }
//            deduction.setAmount(Double.valueOf(requestParam.get("amount")));
            deduction.setStatus(true);
            deduction.setUpdatedAt(LocalDateTime.now());
            deduction.setUpdatedBy(users.getId());
            deduction.setInstitute(users.getInstitute());
            try {
                deductionRepository.save(deduction);
                responseMessage.setMessage("Deduction updated successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to update deduction");
                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to update deduction");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object findDeduction(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Long id = Long.valueOf(request.get("id"));
            Deduction deduction = deductionRepository.findByIdAndStatus(id, true);
            if (deduction != null) {
                responseMessage.setResponse(deduction);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to update deduction");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object deleteDeduction(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long id = Long.valueOf(requestParam.get("id"));
            Deduction deduction = deductionRepository.findByIdAndStatus(id, true);
            deduction.setStatus(false);
            deduction.setUpdatedAt(LocalDateTime.now());
            deduction.setUpdatedBy(users.getId());
            deduction.setInstitute(users.getInstitute());
            try {
                deductionRepository.save(deduction);
                responseMessage.setMessage("Deduction updated successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to update deduction");
                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public JsonObject deductionList(HttpServletRequest request){
        JsonArray result=new JsonArray();
        Users users=jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        List<Deduction> deductionsList=new ArrayList<>();
        deductionsList=deductionRepository.findAllByStatus();
        for(Deduction deduction:deductionsList){
            try{
                JsonObject response=new JsonObject();
                response.addProperty("id",deduction.getId());
                response.addProperty("name",deduction.getName());
                response.addProperty("deductionStatus",deduction.getDeductionStatus());
                result.add(response);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        JsonObject output=new JsonObject();
        output.addProperty("message","success");
        output.addProperty("responseStatus",HttpStatus.OK.value());
        output.add("data",result);
        return output;
    }
}
