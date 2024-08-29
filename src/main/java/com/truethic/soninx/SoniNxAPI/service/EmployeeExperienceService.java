package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.EmployeeExperienceRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.model.EmployeeExperienceDetails;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class EmployeeExperienceService {

    @Autowired
    EmployeeExperienceRepository employeeExperienceRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    public Object createEmployeeExperience(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        EmployeeExperienceDetails employeeExperience = new EmployeeExperienceDetails();
        employeeExperience.setCompanyName(request.getParameter("companyName"));
        employeeExperience.setDesignationName(request.getParameter("designationName"));
        employeeExperience.setDuration(request.getParameter("duration"));
        employeeExperience.setIncomePerMonth(request.getParameter("incomePerMonth"));
        employeeExperience.setReasonToResign(request.getParameter("reasonToResign"));
        employeeExperience.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employeeExperience.setCreatedBy(user.getId());
            employeeExperience.setInstitute(user.getInstitute());
        }
        try {
            employeeExperienceRepository.save(employeeExperience);
            responseObject.setMessage("Employee Experience added successfully");
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
