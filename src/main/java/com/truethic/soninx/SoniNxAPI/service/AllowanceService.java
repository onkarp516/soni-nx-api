package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.AllowanceRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.AllowanceDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Allowance;
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
public class AllowanceService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AllowanceRepository allowanceRepository;

    public Object createAllowance(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Allowance allowance = new Allowance();
            allowance.setName(requestParam.get("name"));
            allowance.setAmount(Double.valueOf(requestParam.get("amount")));
            allowance.setStatus(true);
            allowance.setCreatedBy(users.getId());
            allowance.setCreatedAt(LocalDateTime.now());
            allowance.setInstitute(users.getInstitute());
            try {
                allowanceRepository.save(allowance);
                responseMessage.setMessage("Allowance created successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                responseMessage.setMessage("Failed to create allowance");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to create allowance");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTAllowance(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Allowance> allowanceList = new ArrayList<>();
        List<AllowanceDTDTO> allowanceDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `allowance_tbl` WHERE allowance_tbl.status=1 AND allowance_tbl.institute_id="+user.getInstitute().getId();

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

            Query q = entityManager.createNativeQuery(query, Allowance.class);
            Query q1 = entityManager.createNativeQuery(query1, Allowance.class);

            allowanceList = q.getResultList();
            System.out.println("Limit total rows " + allowanceList.size());

            for (Allowance allowance : allowanceList) {
                allowanceDTDTOList.add(convertToDTDTO(allowance));
            }

            List<Allowance> allowanceArrayList = new ArrayList<>();
            allowanceArrayList = q1.getResultList();
            System.out.println("total rows " + allowanceArrayList.size());

            genericDTData.setRows(allowanceDTDTOList);
            genericDTData.setTotalRows(allowanceArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            genericDTData.setRows(allowanceDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private AllowanceDTDTO convertToDTDTO(Allowance allowance) {
        AllowanceDTDTO allowanceDTDTO = new AllowanceDTDTO();
        allowanceDTDTO.setId(allowance.getId());
        allowanceDTDTO.setName(allowance.getName());
        allowanceDTDTO.setAmount(allowance.getAmount());
        allowanceDTDTO.setStatus(allowance.getStatus());
        allowanceDTDTO.setCreatedAt(String.valueOf(allowance.getCreatedAt()));
        allowanceDTDTO.setUpdatedAt(String.valueOf(allowance.getUpdatedAt()));
        return allowanceDTDTO;
    }

    public Object updateAllowance(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long id = Long.valueOf(requestParam.get("id"));
            Allowance allowance = allowanceRepository.findByIdAndStatus(id, true);
            if (allowance != null) {
                allowance.setName(requestParam.get("name"));
                allowance.setAmount(Double.valueOf(requestParam.get("amount")));
                allowance.setStatus(true);
                allowance.setUpdatedBy(users.getId());
                allowance.setUpdatedAt(LocalDateTime.now());
                allowance.setInstitute(users.getInstitute());
                try {
                    allowanceRepository.save(allowance);
                    responseMessage.setMessage("Allowance updated successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    System.out.println("Exception " + e.getMessage());
                    e.printStackTrace();
                    responseMessage.setMessage("Failed to update allowance");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to update allowance");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object deleteAllowance(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long id = Long.valueOf(requestParam.get("id"));
            Allowance allowance = allowanceRepository.findByIdAndStatus(id, true);
            if (allowance != null) {
                allowance.setStatus(false);
                allowance.setUpdatedBy(users.getId());
                allowance.setUpdatedAt(LocalDateTime.now());
                allowance.setInstitute(users.getInstitute());
                try {
                    allowanceRepository.save(allowance);
                    responseMessage.setMessage("Allowance deleted successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    System.out.println("Exception " + e.getMessage());
                    e.printStackTrace();
                    responseMessage.setMessage("Failed to delete allowance");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to delete allowance");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object findAllowance(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Long id = Long.valueOf(requestParam.get("id"));
            Allowance allowance = allowanceRepository.findByIdAndStatus(id, true);
            if (allowance != null) {
                responseMessage.setResponse(allowance);
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
}
