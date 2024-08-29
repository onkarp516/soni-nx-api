package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.model.*;
import com.truethic.soninx.SoniNxAPI.repository.InspectionRepository;
import com.truethic.soninx.SoniNxAPI.repository.JobOperationRepository;
import com.truethic.soninx.SoniNxAPI.repository.JobRepository;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.InspectionDTDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InspectionService {
    @Autowired
    JwtTokenUtil jwtTokenUtil;

    @Autowired
    private InspectionRepository inspectionRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobOperationRepository jobOperationRepository;

    @Autowired
    private EntityManager entityManager;

    public JsonObject  createLineInspection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            String dsrows = request.getParameter("rows");
            JsonArray jsonArray = new JsonParser().parse(dsrows).getAsJsonArray();

            List<Inspection> inspections = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject object = jsonArray.get(i).getAsJsonObject();
                if (object.get("drawingSize").getAsString() != "" && object.get("drawingSize").getAsString() != null) {
                    Inspection inspection1 = new Inspection();
                    Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
                    JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
                    inspection1.setDrawingSize(object.get("drawingSize").getAsString());
                    inspection1.setJob(job);
                    inspection1.setStatus(true);
                    inspection1.setJobOperation(jobOperation);
                    if (request.getHeader("Authorization") != null) {
                        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                        inspection1.setInstitute(user.getInstitute());
                    }
                    inspections.add(inspection1);
                }
            }
            try {
                inspectionRepository.saveAll(inspections);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                responseMessage.addProperty("message", "Line Inspection Successfully");
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

    public Object DTLineInspection(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Inspection> inspectionList = new ArrayList<>();
        List<InspectionDTDTO> inspectionDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT inspection_tbl.*, job_tbl.job_name as job_name, job_operation_tbl.operation_name as" +
                    " operation_name FROM inspection_tbl LEFT JOIN job_tbl ON inspection_tbl.job_id=job_tbl.id LEFT JOIN job_operation_tbl" +
                    " ON inspection_tbl.job_operation_id=job_operation_tbl.id WHERE inspection_tbl.status=1  AND inspection_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (drawing_size LIKE '%" + searchText + "%' job_name LIKE '%" + searchText
                        + "%' OR operation_name LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, Inspection.class);
            Query q1 = entityManager.createNativeQuery(query1, Inspection.class);

            inspectionList = q.getResultList();
            System.out.println("Limit total rows " + inspectionList.size());

            for (Inspection inspection : inspectionList) {
                inspectionDTDTOList.add(convertToDTDTO(inspection));
            }

            List<Company> companyArrayList = new ArrayList<>();
            companyArrayList = q1.getResultList();
            System.out.println("total rows " + companyArrayList.size());

            genericDTData.setRows(inspectionDTDTOList);
            genericDTData.setTotalRows(companyArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(inspectionDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private InspectionDTDTO convertToDTDTO(Inspection inspection) {
        InspectionDTDTO inspectionDTDTO = new InspectionDTDTO();
        inspectionDTDTO.setId(inspection.getId());
        inspectionDTDTO.setDrawingSize(inspection.getDrawingSize());
        inspectionDTDTO.setStatus(inspection.getStatus());
        inspectionDTDTO.setJobName(inspection.getJob().getJobName());
        inspectionDTDTO.setOperationName(inspection.getJobOperation().getOperationName());
        inspectionDTDTO.setCreatedAt(String.valueOf(inspection.getCreatedAt()));
        if (inspection.getUpdatedAt() != null) {
            inspectionDTDTO.setUpdatedAt(String.valueOf(inspection.getUpdatedAt()));
        }
        return inspectionDTDTO;
    }

    public Object deleteLineInspection(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long id = Long.valueOf(request.getParameter("id"));
            Inspection inspection = inspectionRepository.findByIdAndStatus(id, true);
            if (inspection != null) {
                inspectionRepository.deleteLineInspection(id);
                response.addProperty("message", "Line Inspection Deleted Successfully");
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());

        }
        return response;
    }

    public Object findLineInspection(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long id = Long.valueOf(request.getParameter("id"));
            Inspection inspection = inspectionRepository.findByIdAndStatus(id, true);
            if (inspection != null) {
                JsonObject fObject = new JsonObject();
                fObject.addProperty("id", inspection.getId());
                fObject.addProperty("drawingSize", inspection.getDrawingSize());
                fObject.addProperty("jobId", inspection.getJob().getId());
                fObject.addProperty("jobName", inspection.getJob().getJobName());
                fObject.addProperty("jobOperationId", inspection.getJobOperation().getId());

                response.add("response", fObject);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Data Not Found");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("message", "Failed To Load Data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object updateLineInspection(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();

        Long id = Long.valueOf(request.getParameter("id"));
        Inspection inspection = inspectionRepository.findByIdAndStatus(id, true);
        if (inspection != null) {
            Job job = jobRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobId")), true);
            JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(request.getParameter("jobOperationId")), true);
            inspection.setDrawingSize(request.getParameter("drawingSize"));
            inspection.setJob(job);
            inspection.setJobOperation(jobOperation);
            if (request.getHeader("Authorization") != null) {
                Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
                inspection.setInstitute(user.getInstitute());
            }
            try {
                inspectionRepository.save(inspection);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                responseMessage.addProperty("message", "Line Inspection updated Successfully");
            } catch (Exception e) {
                responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.addProperty("message", "Internal Server Error");
            }
        } else {
            responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            responseMessage.addProperty("message", "Data  Not Found.");
        }
        return responseMessage;
    }

    public JsonObject getDrawingSizes(Map<String, String> request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            List<Inspection> inspectionList = inspectionRepository.findByJobOperationIdAndStatus(Long.valueOf(request.get("jobOperationId")), true);
            for (Inspection mInspection : inspectionList) {
                JsonObject dObject = new JsonObject();
                dObject.addProperty("id", mInspection.getId());
                dObject.addProperty("drawingSize", mInspection.getDrawingSize());
                String[] arrOfStr = mInspection.getDrawingSize().split("/");
                for (int i = 0; i < arrOfStr.length; i++) {
                    System.out.println("str[" + i + "]:" + arrOfStr[i]);
                }
                dObject.addProperty("min", arrOfStr[0]);
                dObject.addProperty("max", arrOfStr[1]);
                jsonArray.add(dObject);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.addProperty("message", "Internal Server Error");
        }
        return response;
    }
}