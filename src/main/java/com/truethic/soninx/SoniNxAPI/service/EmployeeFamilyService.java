package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.EmployeeFamilyRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.model.EmployeeFamily;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class EmployeeFamilyService {

    @Autowired
    EmployeeFamilyRepository employeeFamilyRepository;

    @Autowired
    JwtTokenUtil jwtTokenUtil;

    public Object createEmployeeFamily(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        EmployeeFamily employeeFamily = new EmployeeFamily();

        employeeFamily.setFullName(request.getParameter("fullName"));
        employeeFamily.setAge(request.getParameter("age"));
        employeeFamily.setRelation(request.getParameter("relation"));
        employeeFamily.setEducation(request.getParameter("education"));
        employeeFamily.setBusiness(request.getParameter("business"));
        employeeFamily.setIncomePerMonth(request.getParameter("incomePerMonth"));
        employeeFamily.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            employeeFamily.setCreatedBy(user.getId());
            employeeFamily.setInstitute(user.getInstitute());
        }
        try {
            employeeFamilyRepository.save(employeeFamily);
            responseObject.setMessage("Employee Family added successfully");
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
