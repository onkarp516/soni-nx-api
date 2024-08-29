package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.SiteRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.SiteDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Site;
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
public class SiteService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public Object createSite(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            Site site = new Site();
            site.setSiteName(jsonRequest.get("siteName"));
            site.setSiteHindiName(jsonRequest.get("siteHindiName"));
            site.setSiteCode(jsonRequest.get("siteCode"));
            site.setSiteLat(Double.valueOf(jsonRequest.get("siteLat")));
            site.setSiteLong(Double.valueOf(jsonRequest.get("siteLong")));
            site.setSiteRadius(Double.valueOf(jsonRequest.get("siteRadius")));
            site.setStatus(true);
            site.setCreatedBy(user.getId());
            site.setInstitute(user.getInstitute());
            site.setCreatedAt(LocalDateTime.now());
            try {
                Site site1 = siteRepository.save(site);
                responseObject.setMessage("Site created successfully");
                responseObject.setResponse(site1);
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
                responseObject.setMessage("Failed to create site");
                responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setMessage("Failed to create site");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    public Object DTSite(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Site> siteList = new ArrayList<>();
        List<SiteDTDTO> siteDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `site_tbl` WHERE institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (site_name LIKE '%" + searchText + "%' OR site_code LIKE '%" + searchText +
                        "%' OR site_lat LIKE '%" + searchText + "%' OR site_long LIKE '%" + searchText + "%' OR " +
                        "site_radius LIKE '%" + searchText + "%')";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);

            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") &&
                    jsonObject.get("colId").toString() != null) {
                //   System.out.println(" ORDER BY " + jsonObject.getString("colId"));
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

            Query q = entityManager.createNativeQuery(query, Site.class);
            Query q1 = entityManager.createNativeQuery(query1, Site.class);

            siteList = q.getResultList();
            System.out.println("Limit total rows " + siteList.size());

            List<Site> siteArrayList = new ArrayList<>();
            siteArrayList = q1.getResultList();
            System.out.println("total rows " + siteArrayList.size());

            for (Site site : siteList) {
                siteDTDTOList.add(convertToDTDTO(site));
            }
            genericDTData.setRows(siteDTDTOList);
            genericDTData.setTotalRows(siteArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(siteDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private SiteDTDTO convertToDTDTO(Site site) {
        SiteDTDTO siteDTDTO = new SiteDTDTO();
        siteDTDTO.setId(site.getId());
        siteDTDTO.setSiteName(site.getSiteName());
        siteDTDTO.setSiteHindiName(site.getSiteHindiName());
        siteDTDTO.setSiteCode(site.getSiteCode());
        siteDTDTO.setSiteLat(site.getSiteLat());
        siteDTDTO.setSiteLong(site.getSiteLong());
        siteDTDTO.setSiteRadius(site.getSiteRadius());
        siteDTDTO.setCreatedAt(String.valueOf(site.getCreatedAt()));
        siteDTDTO.setStatus(site.getStatus());
        return siteDTDTO;
    }

    public Object findSite(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Site site = siteRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (site != null) {
                responseMessage.setResponse(site);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseMessage.setMessage("Shift not found");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object updateSite(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Site site = siteRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (site != null) {
                site.setSiteName(requestParam.get("siteName"));
                site.setSiteHindiName(requestParam.get("siteHindiName"));
                site.setSiteCode(requestParam.get("siteCode"));
                site.setSiteLat(Double.valueOf(requestParam.get("siteLat")));
                site.setSiteLong(Double.valueOf(requestParam.get("siteLong")));
                site.setSiteRadius(Double.valueOf(requestParam.get("siteRadius")));
                site.setUpdatedBy(user.getId());
                site.setInstitute(user.getInstitute());
                site.setUpdatedAt(LocalDateTime.now());
                try {
                    siteRepository.save(site);
                    responseObject.setMessage("Site updated successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                    responseObject.setMessage("Failed to update site");
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseObject.setMessage("Data not found");
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setMessage("Failed to update site");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    public Object changeSiteStatus(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Site site = siteRepository.findById(Long.parseLong(requestParam.get("id"))).get();
            if (site != null) {
                site.setStatus(Boolean.valueOf(requestParam.get("status")));
                site.setUpdatedBy(user.getId());
                site.setInstitute(user.getInstitute());
                site.setUpdatedAt(LocalDateTime.now());
                try {
                    siteRepository.save(site);
                    responseObject.setMessage("Site status updated successfully");
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                    responseObject.setMessage("Failed to update site status");
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseObject.setMessage("Data not found");
                responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setMessage("Failed to update site status");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    public JsonObject listOfSites(HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request .getHeader("Authorization").substring(7));
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Site> siteList = siteRepository.findByInstituteIdAndStatus(users.getInstitute().getId(),true);
            for (Site site : siteList) {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("id", site.getId());
                jsonObject.addProperty("siteName", site.getSiteName());
                jsonArray.add(jsonObject);
            }

            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }
}
