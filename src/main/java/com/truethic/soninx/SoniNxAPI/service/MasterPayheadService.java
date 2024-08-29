package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.CompanyRepository;
import com.truethic.soninx.SoniNxAPI.repository.MasterPayheadRepository;
import com.truethic.soninx.SoniNxAPI.repository.PayheadRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.MasterPayheadDTO;
import com.truethic.soninx.SoniNxAPI.model.Company;
import com.truethic.soninx.SoniNxAPI.model.MasterPayhead;
import com.truethic.soninx.SoniNxAPI.model.Payhead;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.views.AttendanceView;
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
public class MasterPayheadService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private MasterPayheadRepository masterPayheadRepository;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private PayheadRepository payheadRepository;

    public Object createMasterPayhead(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            MasterPayhead masterPayhead = new MasterPayhead();

            Long payheadId = Long.valueOf(jsonRequest.get("payheadId"));
            Payhead payhead = payheadRepository.findByIdAndStatus(payheadId, true);

            masterPayhead.setPayhead(payhead);
            masterPayhead.setName(jsonRequest.get("name"));
            masterPayhead.setEmployerPer(Double.valueOf(jsonRequest.get("employerPer")));
            masterPayhead.setEmployeePer(Double.valueOf(jsonRequest.get("employeePer")));
            masterPayhead.setCompanyId(Long.valueOf(jsonRequest.get("companyId")));
            masterPayhead.setCreatedBy(users.getId());
            masterPayhead.setInstitute(users.getInstitute());
            masterPayhead.setCreatedAt(LocalDateTime.now());
            masterPayhead.setStatus(true);

            try {
                masterPayheadRepository.save(masterPayhead);
                responseMessage.setMessage("Master Payhead saved successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                responseMessage.setMessage("Failed to save master payhead");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
        }
        return responseMessage;
    }

    public Object DTMasterPayhead(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<MasterPayhead> masterPayheads = new ArrayList<>();
        List<MasterPayheadDTO> masterPayheadDTOList = new ArrayList<>();
        try {
            String query = "SELECT master_payhead_tbl.*, company_tbl.company_name FROM `master_payhead_tbl` LEFT JOIN" +
                    " company_tbl ON master_payhead_tbl.company_id=company_tbl.id WHERE master_payhead_tbl.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (name LIKE '%" + searchText + "%' OR employee_per LIKE '%" + searchText +
                        "%' OR employer_per LIKE '%" + searchText + "%' OR company_name LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, MasterPayhead.class);
            Query q1 = entityManager.createNativeQuery(query1, MasterPayhead.class);

            masterPayheads = q.getResultList();
            System.out.println("Limit total rows " + masterPayheads.size());

            for (MasterPayhead masterPayhead : masterPayheads) {
                masterPayheadDTOList.add(convertToDTDTO(masterPayhead));
            }

            List<AttendanceView> attendanceViewArrayList = new ArrayList<>();
            attendanceViewArrayList = q1.getResultList();
            System.out.println("total rows " + attendanceViewArrayList.size());

            genericDTData.setRows(masterPayheadDTOList);
            genericDTData.setTotalRows(attendanceViewArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(masterPayheadDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private MasterPayheadDTO convertToDTDTO(MasterPayhead masterPayhead) {
        MasterPayheadDTO masterPayheadDTO = new MasterPayheadDTO();
        masterPayheadDTO.setId(masterPayhead.getId());
        masterPayheadDTO.setName(masterPayhead.getName());
        masterPayheadDTO.setCompanyId(masterPayhead.getCompanyId());
        if (masterPayhead.getCompanyId() != 0) {
            Company company = companyRepository.findByIdAndStatus(masterPayheadDTO.getCompanyId(), true);
            masterPayheadDTO.setCompanyName(company.getCompanyName());
        } else {
            masterPayheadDTO.setCompanyName("BOTH");
        }
        masterPayheadDTO.setEmployeePer(masterPayhead.getEmployeePer());
        masterPayheadDTO.setEmployerPer(masterPayhead.getEmployerPer());
        masterPayheadDTO.setCreatedAt(masterPayhead.getCreatedAt().toString());
        return masterPayheadDTO;
    }

    public Object findMasterPayhead(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Long id = Long.valueOf(request.get("id"));
            MasterPayhead masterPayhead = masterPayheadRepository.findByIdAndStatus(id, true);
            if (masterPayhead != null) {
                MasterPayheadDTO masterPayheadDTO = convertToDTDTO(masterPayhead);

                responseMessage.setResponse(masterPayheadDTO);
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
