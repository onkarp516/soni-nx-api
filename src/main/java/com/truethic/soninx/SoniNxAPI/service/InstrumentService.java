package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.InstrumentRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Instrument;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InstrumentService {
    @Autowired
    InstrumentRepository instrumentRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;

    public Object createInstrument(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Instrument instrument = new Instrument();
        instrument.setName(request.getParameter("instrumentName"));
        instrument.setIsReadingApplicable(Boolean.parseBoolean(request.getParameter("isReadingApplicable")));
        instrument.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            instrument.setCreatedBy(user.getId());
            instrument.setInstitute(user.getInstitute());
        }
        try {
            instrumentRepository.save(instrument);
            responseObject.setMessage("Instrument added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {

            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }

        return responseObject;
    }

    public Object DTInstrument(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Instrument> instrumentList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `instrument_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND name LIKE '%" + searchText + "%'";
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

            Query q = entityManager.createNativeQuery(query, Instrument.class);
            Query q1 = entityManager.createNativeQuery(query1, Instrument.class);

            instrumentList = q.getResultList();
            System.out.println("Limit total rows " + instrumentList.size());

            List<Instrument> instrumentArrayList = new ArrayList<>();
            instrumentArrayList = q1.getResultList();
            System.out.println("total rows " + instrumentArrayList.size());

            genericDTData.setRows(instrumentList);
            genericDTData.setTotalRows(instrumentArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(instrumentList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }


    public Object findInstrument(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Instrument instrument = instrumentRepository.findByIdAndStatus(Long.parseLong(request.get("id")), true);
        if (instrument != null) {
            responseMessage.setResponse(instrument);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } else {
            responseMessage.setMessage("Data not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }


    public Object updateInstrument(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Instrument instrument = instrumentRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
        if (instrument != null) {
            instrument.setName(requestParam.get("instrumentName"));
            instrument.setIsReadingApplicable(Boolean.parseBoolean(requestParam.get("isReadingApplicable")));
            instrument.setStatus(true);
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            instrument.setUpdatedBy(user.getId());
            instrument.setUpdatedAt(LocalDateTime.now());
            instrument.setInstitute(user.getInstitute());
            try {
                instrumentRepository.save(instrument);
                responseObject.setMessage("Instrument updated successfully");
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


    public Object deleteInstrument(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Instrument instrument = instrumentRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")),
                    true);
            if (instrument != null) {
                instrument.setStatus(false);
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                instrument.setCreatedBy(user.getId());
                instrument.setInstitute(user.getInstitute());
                try {
                    instrumentRepository.save(instrument);
                    responseObject.setMessage("Instrument deleted successfully");
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
            responseObject.setMessage("Failed to add job operation");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }


    public Object getInstrument() {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            List<Instrument> instrumentList = instrumentRepository.findAllByStatus(true);
            responseMessage.setResponse(instrumentList);
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            responseMessage.setMessage("Exception occurred");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

}
