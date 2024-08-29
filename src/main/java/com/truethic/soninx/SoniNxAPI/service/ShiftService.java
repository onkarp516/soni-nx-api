package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.ShiftRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.ShiftDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Shift;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ShiftService {
    @Autowired
    ShiftRepository shiftRepository;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @PersistenceContext
    private EntityManager entityManager;

    public Object createShift(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            LocalTime fromTime = LocalTime.parse(requestParam.get("fromTime"));
            LocalTime threshold = LocalTime.parse(requestParam.get("threshold"));
            Boolean isDayDeduction=Boolean.parseBoolean((requestParam.get("isDayDeduction")));
            if(threshold.compareTo(fromTime) < 0){
                responseObject.setMessage("Threshold value should be greater than Punch in time");
                responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            } else {
                Shift shift = new Shift();
                shift.setName(requestParam.get("shiftName"));
                shift.setFromTime(fromTime);
                shift.setThreshold(threshold);
                shift.setIsDayDeduction(isDayDeduction);
                shift.setToTime(LocalTime.parse(requestParam.get("toTime")));
                shift.setLunchTime(Integer.parseInt(requestParam.get("lunchTime")));
                shift.setIsNightShift(Boolean.parseBoolean(requestParam.get("isNightShift")));
                shift.setWorkingHours(LocalTime.parse(requestParam.get("workingHours")));
                shift.setConsiderationCount(Long.valueOf(requestParam.get("considerationCount")));
//                shift.setIsDaysAndHours(Boolean.parseBoolean(requestParam.get("isDaysAndHours")));
                if(isDayDeduction!=null && isDayDeduction==true){
                    shift.setDayValueOfDeduction(requestParam.get("dayValueOfDeduction"));
                }else {
                    shift.setHourValueOfDeduction(Double.parseDouble(requestParam.get("hourValueOfDeduction")));
                }
                shift.setStatus(true);
                shift.setCreatedBy(user.getId());
                shift.setInstitute(user.getInstitute());
                shift.setCreatedAt(LocalDateTime.now());
                try {
                    Shift shift1 = shiftRepository.save(shift);
                    responseObject.setMessage("Shift created successfully");
                    responseObject.setResponse(shift1);
                    responseObject.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception:" + e.getMessage());
                    responseObject.setMessage("Failed to create shift");
                    responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setMessage("Failed to create shift");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    public JsonObject listOfShifts(HttpServletRequest httpServletRequest) {
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Shift> shiftList = shiftRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(), true);
            for (Shift shift : shiftList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", shift.getId());
                object.addProperty("name", shift.getName());

                jsonArray.add(object);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
            /*if (shiftList.size() > 0) {
            } else {
                response.addProperty("message", "Data not found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }*/
        } catch (Exception e) {
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object findShift(Map<String, String> requestParam) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (shift != null) {
                ShiftDTDTO shiftDTDTO = convertToDTO(shift);
                responseMessage.setResponse(shiftDTDTO);
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

    public Object updateShift(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            LocalTime fromTime = LocalTime.parse(requestParam.get("fromTime"));
            LocalTime threshold = LocalTime.parse(requestParam.get("threshold"));
            Boolean isDayDeduction=Boolean.parseBoolean(requestParam.get("isDayDeduction"));
            if(threshold.compareTo(fromTime) < 0) {
                responseObject.setMessage("Threshold value should be greater than Punch in time");
                responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
            } else {
                if (shift != null) {
                    shift.setName(requestParam.get("shiftName"));
                    shift.setFromTime(fromTime);
                    shift.setThreshold(threshold);
                    shift.setIsDayDeduction(isDayDeduction);
                    shift.setToTime(LocalTime.parse(requestParam.get("toTime")));
                    shift.setLunchTime(Integer.parseInt(requestParam.get("lunchTime")));
                    shift.setIsNightShift(Boolean.parseBoolean(requestParam.get("isNightShift")));
                    shift.setWorkingHours(LocalTime.parse(requestParam.get("workingHours")));
                    shift.setConsiderationCount(Long.valueOf(requestParam.get("considerationCount")));
//                    shift.setIsDayDeduction(Boolean.parseBoolean(requestParam.get("isDayDeduction")));
                    if(isDayDeduction!=null && isDayDeduction==true){
                        shift.setDayValueOfDeduction(requestParam.get("dayValueOfDeduction"));
                    }else{
                        shift.setHourValueOfDeduction(Double.parseDouble(requestParam.get("hourValueOfDeduction")));
                    }
                    shift.setStatus(true);
                    shift.setUpdatedBy(user.getId());
                    shift.setInstitute(user.getInstitute());
                    shift.setUpdatedAt(LocalDateTime.now());
                    try {
                        shiftRepository.save(shift);
                        responseObject.setMessage("Shift updated successfully");
                        responseObject.setResponseStatus(HttpStatus.OK.value());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exception:" + e.getMessage());
                        responseObject.setMessage("Failed to update shift");
                        responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                } else {
                    responseObject.setMessage("Data not found");
                    responseObject.setResponseStatus(HttpStatus.NOT_FOUND.value());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseObject.setMessage("Failed to update shift");
            responseObject.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseObject;
    }

    public Object deleteShift(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Shift shift = shiftRepository.findByIdAndStatus(Long.parseLong(requestParam.get("id")), true);
            if (shift != null) {
                shift.setStatus(false);
                shift.setUpdatedAt(LocalDateTime.now());
                shift.setUpdatedBy(users.getId());
                shift.setInstitute(users.getInstitute());
                try {
                    shiftRepository.save(shift);
                    responseMessage.setMessage("Shift deleted successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to delete shift");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Data not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            responseMessage.setMessage("Failed to delete shift");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object DTShift(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Shift> shiftList = new ArrayList<>();
        List<ShiftDTDTO> shiftDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `shift_tbl` WHERE status=1 AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (name LIKE '%" + searchText + "%' OR from_time LIKE '%" + searchText + "%' OR " +
                        "to_time LIKE '%" + searchText + "%' OR working_hours LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, Shift.class);
            Query q1 = entityManager.createNativeQuery(query1, Shift.class);

            shiftList = q.getResultList();
            System.out.println("Limit total rows " + shiftList.size());

            for (Shift shift : shiftList) {
                shiftDTDTOList.add(convertToDTO(shift));
            }

            List<Shift> shiftArrayList = new ArrayList<>();
            shiftArrayList = q1.getResultList();
            System.out.println("total rows " + shiftArrayList.size());

            genericDTData.setRows(shiftDTDTOList);
            genericDTData.setTotalRows(shiftArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(shiftDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private ShiftDTDTO convertToDTO(Shift shift) {
        ShiftDTDTO shiftDTDTO = new ShiftDTDTO();
        shiftDTDTO.setId(shift.getId());
        shiftDTDTO.setName(shift.getName());
        shiftDTDTO.setFromTime(String.valueOf(shift.getFromTime()));
        shiftDTDTO.setToTime(String.valueOf(shift.getToTime()));
        shiftDTDTO.setThreshold(String.valueOf(shift.getThreshold()));
        shiftDTDTO.setLunchTime(shift.getLunchTime());
        shiftDTDTO.setWorkingHours(String.valueOf(shift.getWorkingHours()));
        shiftDTDTO.setIsNightShift(shift.getIsNightShift());
        shiftDTDTO.setConsiderationCount(shift.getConsiderationCount());
        shiftDTDTO.setIsDayDeduction(shift.getIsDayDeduction());
        shiftDTDTO.setDayValueOfDeduction(String.valueOf(shift.getDayValueOfDeduction()));
        shiftDTDTO.setHourValueOfDeduction(shift.getHourValueOfDeduction());
        shiftDTDTO.setCreatedBy(shift.getCreatedBy());
        shiftDTDTO.setCreatedAt(String.valueOf(shift.getCreatedAt()));
        shiftDTDTO.setUpdatedBy(shift.getUpdatedBy());
        if (shift.getUpdatedAt() != null)
            shiftDTDTO.setUpdatedAt(String.valueOf(shift.getUpdatedAt()));
        return shiftDTDTO;
    }
}
