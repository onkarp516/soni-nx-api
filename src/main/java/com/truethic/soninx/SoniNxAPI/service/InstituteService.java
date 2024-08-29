package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.model.Institute;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.repository.InstituteRepository;
import com.truethic.soninx.SoniNxAPI.repository.UserRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.InstituteDTDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class InstituteService {
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserRepository userRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private InstituteRepository instituteRepository;

    public JsonObject createInstitute(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            if(checkUserNameExists(request.getParameter("username"))){
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_ACCEPTABLE.value());
                responseMessage.addProperty("message", "Username already exists");
                return responseMessage;
            }
            Institute institute = new Institute();
            institute.setInstituteName(request.getParameter("institute_name"));
            institute.setMobile(request.getParameter("mobile"));
            institute.setEmail(request.getParameter("email"));
            institute.setAddress(request.getParameter("address"));
            institute.setUsername(request.getParameter("username"));
            institute.setStatus(true);
            institute.setPassword(passwordEncoder.encode(request.getParameter("password")));
            institute.setPlainPassword(request.getParameter("password"));
            if (request.getHeader("Authorization") != null) {
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                institute.setCreatedBy(user.getId());
                institute.setCreatedAt(LocalDateTime.now());
            }
            try {
                Institute institute1 = instituteRepository.save(institute);
                if(institute1 != null){
                    Users users = new Users();
                    users.setUsername(request.getParameter("username"));
                    users.setIsSuperadmin(false);
                    users.setStatus(true);
                    users.setPassword(passwordEncoder.encode(request.getParameter("password")));
                    users.setPlain_password(request.getParameter("password"));
                    users.setInstitute(institute1);
                    users.setIsAdmin(true);
                    userRepository.save(users);
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                    responseMessage.addProperty("message", "Institute created Successfully");
                } else {
                    responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    responseMessage.addProperty("message", "Failed to create institute");
                }
            } catch (Exception e) {
                e.printStackTrace();
                responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.addProperty("message", "Internal Server Error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.addProperty("message", "Internal Server Error");
        }
        return responseMessage;
    }

    public boolean checkUserNameExists(String username) {
        Users user = userRepository.findByUsernameAndStatus(username, true);
        if (user != null)
            return true;
        else
            return false;
    }

    public JsonObject listOfInstitutes() {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Institute> institutesList = instituteRepository.findAllByStatus(true);
            for (Institute institute : institutesList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", institute.getId());
                object.addProperty("institute_name", institute.getInstituteName());
                object.addProperty("mobile", institute.getMobile());
                object.addProperty("username", institute.getUsername());
                object.addProperty("address", institute.getAddress());
                jsonArray.add(object);
            }
            response.add("institutesList", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getInstitute(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Institute institute = instituteRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (institute != null) {
                responseMessage.setResponse(institute);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTInstitute(Map<String, String> request) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");

        GenericDTData genericDTData = new GenericDTData();
        List<Institute> instituteList = new ArrayList<>();
        List<InstituteDTDTO> instituteDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `institute_tbl` WHERE institute_tbl.status=1";

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (institute_name LIKE '%" + searchText + "%' OR username LIKE '%" + searchText +
                        "%')";
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

            Query q = entityManager.createNativeQuery(query, Institute.class);
            Query q1 = entityManager.createNativeQuery(query1, Institute.class);

            instituteList = q.getResultList();
            System.out.println("Limit total rows " + instituteList.size());

            for (Institute institute : instituteList) {
                instituteDTDTOList.add(convertToDTDTO(institute));
            }

            List<Institute> instituteArrayList = new ArrayList<>();
            instituteArrayList = q1.getResultList();
            System.out.println("total rows " + instituteArrayList.size());

            genericDTData.setRows(instituteDTDTOList);
            genericDTData.setTotalRows(instituteArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(instituteDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private InstituteDTDTO convertToDTDTO(Institute institute) {
        InstituteDTDTO instituteDTDTO = new InstituteDTDTO();
        instituteDTDTO.setId(institute.getId());
        instituteDTDTO.setInstituteName(institute.getInstituteName());
        instituteDTDTO.setStatus(institute.getStatus());
        instituteDTDTO.setMobile(institute.getMobile());
        instituteDTDTO.setEmail(institute.getEmail());
        instituteDTDTO.setAddress(institute.getAddress());
        instituteDTDTO.setUsername(institute.getUsername());
        instituteDTDTO.setPassword(institute.getPassword());
        instituteDTDTO.setPlainPassword(institute.getPlainPassword());
        instituteDTDTO.setCreatedAt(institute.getCreatedAt());
        instituteDTDTO.setUpdatedAt(institute.getUpdatedAt());
        return instituteDTDTO;
    }

    public Object updateInstitute(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Institute institute = instituteRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("id")),
                    true);
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            institute.setInstituteName(jsonRequest.get("institute_name"));
            System.out.println("Name:"+jsonRequest.get("institute_name"));
            institute.setMobile(jsonRequest.get("mobile"));
            institute.setEmail(jsonRequest.get("email"));
            institute.setAddress(jsonRequest.get("address"));
            institute.setUsername(jsonRequest.get("username"));
            institute.setStatus(true);
            System.out.println("Password:"+jsonRequest.get("password"));
            institute.setPassword(passwordEncoder.encode(jsonRequest.get("password")));
            institute.setPlainPassword(jsonRequest.get("password"));
            institute.setUpdatedAt(LocalDateTime.now());
            institute.setUpdatedBy(users.getId());
            instituteRepository.save(institute);
            responseMessage.setMessage("Institute updated successfully");
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to update");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }
    public Object deleteInstitute(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Institute institute = instituteRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("id")),
                    true);
            if (institute != null) {
                institute.setStatus(false);
                institute.setUpdatedBy(users.getId());
                institute.setUpdatedAt(LocalDateTime.now());
                try {
                    instituteRepository.save(institute);
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                    responseMessage.setMessage("Institute deleted successfully");
                } catch (Exception e) {
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to delete institute");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseMessage.setMessage("Data not found");
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to delete institute");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }
}
