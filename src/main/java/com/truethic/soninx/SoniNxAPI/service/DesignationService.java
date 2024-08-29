package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.DesignationRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.DesignationDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Designation;
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
public class DesignationService {
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DesignationRepository designationRepository;

    public Object createDesignation(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Designation designation = new Designation();

        designation.setName(request.getParameter("name"));
        designation.setCode(request.getParameter("code"));
        designation.setStatus(true);

        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            designation.setCreatedBy(user.getId());
            designation.setInstitute(user.getInstitute());
        }
        try {
            Designation designation1 = designationRepository.save(designation);
            responseObject.setResponse(designation1);
            responseObject.setMessage("Designation added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setMessage("Failed to create designation");
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    /*public Object DTDesignation(Integer pageNo, Integer pageSize) {
        ResponseMessage responseMessage = new ResponseMessage();
        Pageable pageable = PageRequest.of(pageNo - 1, pageSize);
        try {
            Page<Designation> designationPage = designationRepository.findByStatusOrderByIdDesc(pageable, true);
            List<Designation> designationList = designationPage.toList();
            GenericData<Designation> data = new GenericData<>(designationList, designationPage.getTotalElements(),
                    pageNo, pageSize,
                    designationPage.getTotalPages());
            responseMessage.setResponse(data);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }*/

    public Object createDesignationSP(HttpServletRequest request) {
        try {
            designationRepository.insertDesignation(request.getParameter("desigName"), request.getParameter("code"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }

    public Object findDesignation(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long designationId = Long.parseLong(requestParam.get("id"));
        try {
            Designation designation = designationRepository.findByIdAndStatus(designationId, true);
            if (designation != null) {
                responseMessage.setResponse(designation);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    public Object updateDesignation(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Designation designation = designationRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                true);
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        designation.setName(requestParam.get("name"));
        designation.setCode(requestParam.get("code"));
        designation.setUpdatedBy(users.getId());
        designation.setInstitute(users.getInstitute());
        try {
            designationRepository.save(designation);
            responseMessage.setMessage("Designation updated successfully");
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to update");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object deleteDesignation(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        Designation designation = designationRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                true);
        if (designation != null) {
            designation.setStatus(false);
            designation.setUpdatedBy(users.getId());
            designation.setUpdatedAt(LocalDateTime.now());
            designation.setInstitute(users.getInstitute());
            try {
                designationRepository.save(designation);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
                responseMessage.setMessage("Designation deleted successfully");
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to delete designation");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("Data not found");
        }
        return responseMessage;
    }

    public JsonObject listOfDesignation(HttpServletRequest httpServletRequest) {
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest .getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Designation> designationList = designationRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(), true);
            for (Designation designation : designationList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", designation.getId());
                object.addProperty("name", designation.getName());
                object.addProperty("code", designation.getCode());
                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object DTDesignation(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Designation> designationList = new ArrayList<>();
        List<DesignationDTDTO> designationDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT id, name, code, created_at, created_by, updated_at, updated_by, " +
                    "status, institute_id FROM `designation_tbl` WHERE designation_tbl.status=1 AND designation_tbl.institute_id="+users.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (name LIKE '%" + searchText + "%' OR code LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, Designation.class);
            Query q1 = entityManager.createNativeQuery(query1, Designation.class);

            designationList = q.getResultList();
            System.out.println("Limit total rows " + designationList.size());

            for (Designation designation : designationList) {
                designationDTDTOList.add(convertToDTO(designation));
            }

            List<Designation> designationArrayList = new ArrayList<>();
            designationArrayList = q1.getResultList();
            System.out.println("total rows " + designationArrayList.size());

            genericDTData.setRows(designationDTDTOList);
            genericDTData.setTotalRows(designationArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(designationDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private DesignationDTDTO convertToDTO(Designation designation) {
        DesignationDTDTO designationDTDTO = new DesignationDTDTO();
        designationDTDTO.setId(designation.getId());
        designationDTDTO.setName(designation.getName());
        designationDTDTO.setCode(designation.getCode());
        designationDTDTO.setCreatedAt(String.valueOf(designation.getCreatedAt()));
        designationDTDTO.setUpdatedAt(String.valueOf(designation.getUpdatedAt()));
        designationDTDTO.setCreatedBy(designation.getCreatedBy());
        designationDTDTO.setUpdatedBy(designation.getUpdatedBy());
        return designationDTDTO;
    }
}
