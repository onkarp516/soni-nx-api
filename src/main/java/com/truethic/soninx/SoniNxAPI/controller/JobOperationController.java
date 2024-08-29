package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.JobOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class JobOperationController {
    @Autowired
    JobOperationService jobOperationService;

    @PostMapping(path = "/createJobOperation")
    public Object createJobOperation(MultipartHttpServletRequest request) {
        return jobOperationService.createJobOperation(request);
    }

    @PostMapping(path = "/createNewJobOperation")
    public Object createNewJobOperation(MultipartHttpServletRequest request) {
        return jobOperationService.createNewJobOperation(request);
    }

    @PostMapping(path = "/DTJobOperation")
    public Object DTJobOperation(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return jobOperationService.DTJobOperation(request, httpServletRequest);
    }

    @PostMapping(path = "/findJobOperation")
    public Object findJobOperation(@RequestBody Map<String, String> request) {
        return jobOperationService.findJobOperation(request).toString();
    }

    @PostMapping(path = "/updateJobOperation")
    public Object updateJobOperation(MultipartHttpServletRequest request) {
        return jobOperationService.updateJobOperation(request);
    }

    @PostMapping(path = "/deleteJobOperation")
    public Object deleteJobOperation(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return jobOperationService.deleteJobOperation(requestParam, request);
    }

    @GetMapping(path = "/jobOperation-list")
    public Object getJobOperation() {
        return jobOperationService.getJobOperation().toString();
    }

    @PostMapping(path = "/listJobOperation")
    public Object listJobOperation(@RequestBody Map<String, String> request) {
        JsonObject res = jobOperationService.listJobOperation(request);
        return res.toString();
    }

    @PostMapping(path = "/deleteProcedureSheet")
    public Object deleteProcedureSheet(@RequestBody Map<String, String> jsonRequest) {
        return jobOperationService.deleteProcedureSheet(jsonRequest).toString();
    }

    @PostMapping(path = "/deleteDrawingSheet")
    public Object deleteDrawingSheet(@RequestBody Map<String, String> jsonRequest) {
        return jobOperationService.deleteDrawingSheet(jsonRequest).toString();
    }


    /*mobile app url start*/
    @PostMapping(path = "/mobile/jobOperation/listForSelection")
    public Object listForSelection(@RequestBody Map<String, String> request, HttpServletRequest req) {
        JsonObject res = jobOperationService.listForSelection(request, req);
        return res.toString();
    }

    @PostMapping(path = "/mobile/jobOperation/viewDrawing")
    public Object viewDrawing(@RequestBody Map<String, String> request, HttpServletRequest req) {
        JsonObject res = jobOperationService.viewDrawing(request, req);
        return res.toString();
    }



    /*mobile app url end*/
}
