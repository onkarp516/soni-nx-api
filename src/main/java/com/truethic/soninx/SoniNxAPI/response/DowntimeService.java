package com.truethic.soninx.SoniNxAPI.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.DowntimeRepository;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeRepository;
import com.truethic.soninx.SoniNxAPI.repository.WorkBreakRepository;
import com.truethic.soninx.SoniNxAPI.viewRepository.DowntimeViewRepository;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.Downtime;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.WorkBreak;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.views.DowntimeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class DowntimeService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private DowntimeRepository downtimeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private WorkBreakRepository workBreakRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private DowntimeViewRepository downtimeViewRepository;

    public Object saveDowntime(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Downtime downtime = new Downtime();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            Long breakId = Long.valueOf(requestParam.get("breakId"));
            WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);

            downtime.setEmployee(employee);
            downtime.setInstitute(employee.getInstitute());
            downtime.setWorkBreak(workBreak);
            downtime.setDowntimeDate(LocalDate.now());
            downtime.setWorkDone(Boolean.valueOf(requestParam.get("workDone")));

            LocalTime l1 = LocalTime.parse(requestParam.get("startTime"));
            LocalTime l2 = LocalTime.parse(requestParam.get("endTime"));
            downtime.setStartTime(l1);
            downtime.setEndTime(l2);
            System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
            double totalTime = SECONDS.between(l1, l2) / 60.0;
            double time = Double.parseDouble(String.format("%.2f", totalTime));
            downtime.setTotalTime(time);

            String note = requestParam.get("note");
            if (!note.equalsIgnoreCase("")) {
                downtime.setNote(note);
            }
            downtime.setStatus(true);
            downtime.setCreatedBy(employee.getId());
            downtime.setInstitute(employee.getInstitute());
            try {
                downtimeRepository.save(downtime);
                responseMessage.setMessage("Downtime saved successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to save downtime");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            return responseMessage;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to save downtime");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public Object getDowntimes(Map<String, String> request) {
//        List<DowntimeView> downtimeViewList = downtimeViewRepository.findAll();
//        return downtimeViewList;

        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");

        GenericDTData genericDTData = new GenericDTData();
        List<DowntimeView> downtimeViewList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `downtime_view`";

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND downtime_date LIKE '%" + searchText + "%'";
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
                query = query + " ORDER BY downtime_id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, DowntimeView.class);
            Query q1 = entityManager.createNativeQuery(query1, DowntimeView.class);

            downtimeViewList = q.getResultList();
            System.out.println("Limit total rows " + downtimeViewList.size());

            List<DowntimeView> downtimeViewArrayList = new ArrayList<>();
            downtimeViewArrayList = q1.getResultList();
            System.out.println("total rows " + downtimeViewArrayList.size());

            genericDTData.setRows(downtimeViewList);
            genericDTData.setTotalRows(downtimeViewArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(downtimeViewList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;

    }
}
