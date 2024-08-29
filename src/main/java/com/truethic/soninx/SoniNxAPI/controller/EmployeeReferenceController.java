package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.EmployeeReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class EmployeeReferenceController {
    @Autowired
    EmployeeReferenceService employeeReferenceService;

    @PostMapping(path = "/create_emp_reference")
    public ResponseEntity<?> createEmployeeReference(HttpServletRequest request) {
        return ResponseEntity.ok(employeeReferenceService.createEmployeeReference(request));
    }
}
