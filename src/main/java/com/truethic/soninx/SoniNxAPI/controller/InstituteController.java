package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.InstituteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class InstituteController {
    @Autowired
    private InstituteService instituteService;
    @PostMapping(path = "/createInstitute")
    public Object createLineInspection(HttpServletRequest request) {
        return instituteService.createInstitute(request).toString();
    }
    @GetMapping(path = "/listOfInstitutes")
    public Object listOfDesignation() {
        JsonObject res = instituteService.listOfInstitutes();
        return res.toString();
    }
    @PostMapping(path = "/getInstitute")
    public Object getInstitute(@RequestBody Map<String, String> requestParam) {
        return instituteService.getInstitute(requestParam);
    }

    @PostMapping(path = "/DTInstitute")
    public Object DTCompany(@RequestBody Map<String, String> request) {
        return instituteService.DTInstitute(request);
    }

    @PostMapping(path = "/updateInstitute")
    public Object updateCompany(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return instituteService.updateInstitute(jsonRequest, request);
    }

    @PostMapping(path = "/deleteInstitute")
    public Object deleteCompany(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return instituteService.deleteInstitute(jsonRequest, request);
    }
}
