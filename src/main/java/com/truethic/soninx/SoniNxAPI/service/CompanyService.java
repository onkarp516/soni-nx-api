package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.CompanyRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.CompanyDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Company;
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
public class CompanyService {
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;

    public Object createCompany(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            Company company = new Company();
            company.setCompanyName(jsonRequest.get("companyName"));
            company.setDescription(jsonRequest.get("description"));
            company.setStatus(true);
            company.setCreatedBy(user.getId());
            company.setInstitute(user.getInstitute());

            Company company1 = companyRepository.save(company);
            responseObject.setResponse(company1);
            responseObject.setMessage("Company added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            responseObject.setMessage("Failed to create company");
            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object DTCompany(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));

        GenericDTData genericDTData = new GenericDTData();
        List<Company> companyList = new ArrayList<>();
        List<CompanyDTDTO> companyDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `company_tbl` WHERE company_tbl.status=1 AND company_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (companyName LIKE '%" + searchText + "%' OR designation LIKE '%" + searchText +
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

            Query q = entityManager.createNativeQuery(query, Company.class);
            Query q1 = entityManager.createNativeQuery(query1, Company.class);

            companyList = q.getResultList();
            System.out.println("Limit total rows " + companyList.size());

            for (Company company : companyList) {
                companyDTDTOList.add(convertToDTDTO(company));
            }

            List<Company> companyArrayList = new ArrayList<>();
            companyArrayList = q1.getResultList();
            System.out.println("total rows " + companyArrayList.size());

            genericDTData.setRows(companyDTDTOList);
            genericDTData.setTotalRows(companyArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(companyDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private CompanyDTDTO convertToDTDTO(Company company) {
        CompanyDTDTO companyDTDTO = new CompanyDTDTO();
        companyDTDTO.setId(company.getId());
        companyDTDTO.setCompanyName(company.getCompanyName());
        companyDTDTO.setStatus(company.getStatus());
        companyDTDTO.setDescription(company.getDescription());
        companyDTDTO.setCreatedBy(company.getCreatedBy());
        companyDTDTO.setCreatedAt(String.valueOf(company.getCreatedAt()));
        companyDTDTO.setUpdatedAt(String.valueOf(company.getUpdatedAt()));
        return companyDTDTO;
    }

    public JsonObject listOfCompany(HttpServletRequest httpServletRequest) {
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest .getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Company> companyList = companyRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(), true);
            for (Company company : companyList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", company.getId());
                object.addProperty("companyName", company.getCompanyName());

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

    public Object updateCompany(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Company company = companyRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("id")),
                    true);
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            company.setCompanyName(jsonRequest.get("companyName"));
            company.setDescription(jsonRequest.get("description"));
            company.setUpdatedBy(users.getId());
            company.setInstitute(users.getInstitute());
            companyRepository.save(company);
            responseMessage.setMessage("Company updated successfully");
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to update");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object deleteCompany(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Company company = companyRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("id")),
                    true);
            if (company != null) {
                company.setStatus(false);
                company.setUpdatedBy(users.getId());
                company.setUpdatedAt(LocalDateTime.now());
                company.setInstitute(users.getInstitute());
                try {
                    companyRepository.save(company);
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                    responseMessage.setMessage("Company deleted successfully");
                } catch (Exception e) {
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to delete company");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                responseMessage.setMessage("Data not found");
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to delete company");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object findCompany(Map<String, String> jsonRequest) {
        ResponseMessage responseMessage = new ResponseMessage();
        Long companyId = Long.parseLong(jsonRequest.get("id"));
        try {
            Company company = companyRepository.findByIdAndStatus(companyId, true);
            if (company != null) {
                responseMessage.setResponse(company);
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
}
