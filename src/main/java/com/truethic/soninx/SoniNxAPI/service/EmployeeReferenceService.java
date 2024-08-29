package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.EmployeeReferenceRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.model.EmployeeReference;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class EmployeeReferenceService {
    @Autowired
    EmployeeReferenceRepository employeeReferenceRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    public Object createEmployeeReference(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        EmployeeReference employeeReference = new EmployeeReference();
        employeeReference.setName(request.getParameter("name"));
        employeeReference.setAddress(request.getParameter("address"));
        employeeReference.setBusiness(request.getParameter("business"));
        employeeReference.setMobileNumber(String.valueOf(request.getParameter("mobileNumber")));
        employeeReference.setKnownFromWhen(request.getParameter("knownFromWhen"));
        employeeReference.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employeeReference.setCreatedBy(user.getId());
            employeeReference.setInstitute(user.getInstitute());
        }
        try {
            employeeReferenceRepository.save(employeeReference);
            responseObject.setMessage("Employee Reference added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {

            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }
}
