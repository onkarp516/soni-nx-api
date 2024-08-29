package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.DesignationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class DesignationController {
    @Autowired
    private DesignationService designationService;

    @PostMapping(path = "/createDesignation")
    public ResponseEntity<?> createDesignation(HttpServletRequest request) {
        return ResponseEntity.ok(designationService.createDesignation(request));
    }

    @PostMapping(path = "/findDesignation")
    public Object findDesignation(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return designationService.findDesignation(requestParam, request);
    }

    @PostMapping(path = "/updateDesignation")
    public Object updateDesignation(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return designationService.updateDesignation(requestParam, request);
    }

    @PostMapping(path = "/deleteDesignation")
    public Object deleteDesignation(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return designationService.deleteDesignation(requestParam, request);
    }

    @PostMapping(path = "/designation-create-sp")
    public ResponseEntity<?> createDesignationSP(HttpServletRequest request) {
        return ResponseEntity.ok(designationService.createDesignationSP(request));
    }

    @GetMapping(path = "/listOfDesignation")
    public Object listOfDesignation(HttpServletRequest httpServletRequest) {
        JsonObject res = designationService.listOfDesignation(httpServletRequest);
        return res.toString();
    }

    @PostMapping(path = "/DTDesignation")
    public Object DTDesignation(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return designationService.DTDesignation(request, httpServletRequest);
    }

}

