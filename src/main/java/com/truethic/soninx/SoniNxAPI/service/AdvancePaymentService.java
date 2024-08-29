package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.repository.AdvancePaymentRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.AdvancePaymentDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.model.AdvancePayment;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.views.AdvancePaymentView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AdvancePaymentService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AdvancePaymentRepository paymentRepository;

    public Object saveAdvancePayment(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        AdvancePayment advancePayment = new AdvancePayment();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            advancePayment.setEmployee(employee);
//            employee.getAdvancePaymentList().add(advancePayment);
            advancePayment.setDateOfRequest(LocalDate.parse(requestParam.get("dateOfRequest")));
            advancePayment.setRequestAmount(Integer.valueOf(requestParam.get("requestAmount")));
            advancePayment.setCreatedBy(employee.getId());
            advancePayment.setInstitute(employee.getInstitute());
            advancePayment.setPaymentStatus("Pending");
            advancePayment.setStatus(true);
            advancePayment.setInstitute(employee.getInstitute());
            try {
                paymentRepository.save(advancePayment);
                responseMessage.setMessage("Saved advance payment request");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed to save advance payment");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to save advance payment");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject listOfAdvancePayment(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            List<AdvancePayment> advancePaymentList =
                    paymentRepository.findByEmployeeIdAndStatusOrderByIdDesc(employee.getId(),
                            true);
            for (AdvancePayment advancePayment : advancePaymentList) {
                JsonObject object = new JsonObject();
                object.addProperty("id", advancePayment.getId());
                String dateOfRequest = advancePayment.getDateOfRequest() == null ? "" :
                        String.valueOf(advancePayment.getDateOfRequest());
                object.addProperty("dateOfRequest", dateOfRequest);
                object.addProperty("requestAmount", advancePayment.getRequestAmount());
                object.addProperty("reason", advancePayment.getReason());
                object.addProperty("paymentStatus", advancePayment.getPaymentStatus());
                String paidDate = advancePayment.getPaymentDate() == null ? "" :
                        String.valueOf(advancePayment.getPaymentDate());
                object.addProperty("paidDate", paidDate);
                object.addProperty("paidAmount", advancePayment.getPaidAmount());
                object.addProperty("remark", advancePayment.getRemark());
                object.addProperty("approvedBy", advancePayment.getApprovedBy());
                object.addProperty("createdAt", String.valueOf(advancePayment.getCreatedAt()));
                jsonArray.add(object);
            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object DTAdvancePayment(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        GenericDTData genericDTData = new GenericDTData();
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        List<AdvancePaymentView> advancePaymentList = new ArrayList<>();
        List<AdvancePaymentDTO> advancePaymentDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `advance_payment_view` WHERE advance_payment_view.status=1 AND advance_payment_view.institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (full_name LIKE '%" + searchText + "%' OR approved_by LIKE '%" + searchText +
                        "%' OR date_of_request LIKE '%" + searchText + "%' OR paid_amount LIKE '%" + searchText + "%'" +
                        " OR payment_date LIKE '%" + searchText + "%' OR payment_status LIKE '%" + searchText + "%' OR" +
                        " remark LIKE '%" + searchText + "%' OR request_amount LIKE '%" + searchText + "%' )";
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

            Query q = entityManager.createNativeQuery(query, AdvancePaymentView.class);
            Query q1 = entityManager.createNativeQuery(query1, AdvancePaymentView.class);

            advancePaymentList = q.getResultList();
            System.out.println("Limit total rows " + advancePaymentList.size());

            for (AdvancePaymentView advancePaymentView : advancePaymentList) {
                advancePaymentDTOList.add(convertViewToAdvancePaymentDTO(advancePaymentView));
            }

            List<AdvancePaymentView> advancePaymentArrayList = new ArrayList<>();
            advancePaymentArrayList = q1.getResultList();
            System.out.println("total rows " + advancePaymentArrayList.size());

            genericDTData.setRows(advancePaymentDTOList);
            genericDTData.setTotalRows(advancePaymentArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(advancePaymentDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private AdvancePaymentDTO convertViewToAdvancePaymentDTO(AdvancePaymentView advancePayment) {
        AdvancePaymentDTO advancePaymentDTO = new AdvancePaymentDTO();

        advancePaymentDTO.setId(advancePayment.getId());
        advancePaymentDTO.setEmployeeName(advancePayment.getFullName());
        if (advancePayment.getDateOfRequest() != null) {
            advancePaymentDTO.setDateOfRequest(advancePayment.getDateOfRequest().toString());
        }
        advancePaymentDTO.setRequestAmount(advancePayment.getRequestAmount());
        advancePaymentDTO.setReason(advancePayment.getReason());
        advancePaymentDTO.setPaymentStatus(advancePayment.getPaymentStatus());
        if (advancePayment.getPaymentDate() != null) {
            advancePaymentDTO.setPaymentDate(advancePayment.getPaymentDate().toString());
        }
        advancePaymentDTO.setPaidAmount(advancePayment.getPaidAmount());
        advancePaymentDTO.setRemark(advancePayment.getRemark());
        advancePaymentDTO.setApprovedBy(advancePayment.getApprovedBy());
        advancePaymentDTO.setCreatedAt(advancePayment.getCreatedAt().toString());

        return advancePaymentDTO;
    }

    public Object updateAdvancePayment(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long paymentId = Long.valueOf(jsonRequest.get("paymentId"));
            Integer paidAmount = Integer.valueOf(jsonRequest.get("paidAmount"));
            String remark = jsonRequest.get("remark");
            AdvancePayment advancePayment = paymentRepository.findByIdAndStatus(paymentId, true);
            if (advancePayment != null) {
                if (!remark.isEmpty()) {
                    advancePayment.setRemark(remark);
                }
                advancePayment.setPaymentDate(LocalDate.now());
                advancePayment.setUpdatedBy(users.getId());
                advancePayment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(advancePayment);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
        }
        return responseMessage;
    }

    public Object rejectAdvancePayment(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long paymentId = Long.valueOf(jsonRequest.get("paymentId"));
            Integer paymentAmount = Integer.valueOf(jsonRequest.get("paymentAmount"));
            String paymentStatus = jsonRequest.get("paymentStatus");
            String remark = jsonRequest.get("remark");
            AdvancePayment advancePayment = paymentRepository.findByIdAndStatus(paymentId, true);
            if (advancePayment != null) {
                if (!remark.isEmpty()) {
                    advancePayment.setRemark(remark);
                }
                advancePayment.setPaymentStatus(paymentStatus);
                advancePayment.setApprovedBy(users.getUsername());
                advancePayment.setUpdatedBy(users.getId());
                advancePayment.setUpdatedAt(LocalDateTime.now());
                advancePayment.setInstitute(users.getInstitute());
                try {
                    paymentRepository.save(advancePayment);
                    responseMessage.setMessage("Request rejected successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to reject request");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Payment request not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to reject request");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object approveAdvancePayment(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long paymentId = Long.valueOf(jsonRequest.get("paymentId"));
            Integer paymentAmount = Integer.valueOf(jsonRequest.get("paymentAmount"));
            String paymentStatus = jsonRequest.get("paymentStatus");
            String remark = jsonRequest.get("remark");
            AdvancePayment advancePayment = paymentRepository.findByIdAndStatus(paymentId, true);
            if (advancePayment != null) {
                if (!remark.isEmpty()) {
                    advancePayment.setRemark(remark);
                }
                advancePayment.setPaymentStatus(paymentStatus);
                advancePayment.setPaidAmount(paymentAmount);
                advancePayment.setApprovedBy(users.getUsername());
                advancePayment.setPaymentDate(LocalDate.now());
                advancePayment.setUpdatedBy(users.getId());
                advancePayment.setUpdatedAt(LocalDateTime.now());
                advancePayment.setInstitute(users.getInstitute());
                paymentRepository.save(advancePayment);
                responseMessage.setMessage("Request approved successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Payment request not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to approve request");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }
}
