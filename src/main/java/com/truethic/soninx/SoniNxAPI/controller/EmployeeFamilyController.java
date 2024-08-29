package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.EmployeeFamilyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class EmployeeFamilyController {

    @Autowired
    EmployeeFamilyService employeeFamilyService;

    @PostMapping(path = "/create_empFamily")
    public ResponseEntity<?> createEmployeeFamily(HttpServletRequest request) {
        return ResponseEntity.ok(employeeFamilyService.createEmployeeFamily(request));
    }
}
