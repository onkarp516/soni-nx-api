package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class CompanyController {
    @Autowired
    private CompanyService companyService;

    @PostMapping(path = "/createCompany")
    public Object createCompany(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return companyService.createCompany(jsonRequest, request);
    }

    @PostMapping(path = "/findCompany")
    public Object findCompany(@RequestBody Map<String, String> jsonRequest) {
        return companyService.findCompany(jsonRequest);
    }

    @PostMapping(path = "/updateCompany")
    public Object updateCompany(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return companyService.updateCompany(jsonRequest, request);
    }

    @PostMapping(path = "/deleteCompany")
    public Object deleteCompany(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return companyService.deleteCompany(jsonRequest, request);
    }

    @PostMapping(path = "/DTCompany")
    public Object DTCompany(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return companyService.DTCompany(request, httpServletRequest);
    }

    @GetMapping(path = "/listOfCompany")
    public Object listOfCompany(HttpServletRequest httpServletRequest) {
        JsonObject res = companyService.listOfCompany(httpServletRequest);
        return res.toString();
    }
}
