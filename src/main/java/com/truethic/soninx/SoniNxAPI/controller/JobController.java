package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@RestController
public class JobController {
    @Autowired
    JobService jobService;

    @PostMapping(path = "/createJob")
    public ResponseEntity<?> createJob(MultipartHttpServletRequest request) {
        return ResponseEntity.ok(jobService.createJob(request));
    }

    @PostMapping(path = "/DTJob")
    public Object DTJob(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return jobService.DTJob(request,httpServletRequest);
    }

    @GetMapping(path = "/listOfJobsForSelect")
    public Object listOfJobsForSelect(HttpServletRequest httpServletRequest) {
        JsonObject res = jobService.listOfJobsForSelect(httpServletRequest);
        return res.toString();
    }

    @GetMapping(path = "/listOfJobs")
    public Object listOfJobs() {
        return jobService.listOfJobs();
    }

    @PostMapping(path = "/findJob")
    public Object findJob(@RequestBody Map<String, String> requestParam) {
        return jobService.findJob(requestParam);
    }

    @PostMapping(path = "/updateJob")
    public Object updateJob(MultipartHttpServletRequest request) throws IOException {
        return jobService.updateJob(request);
    }

    @PostMapping(path = "/deleteJob")
    public Object deleteJob(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return jobService.deleteJob(requestParam, request);
    }

    /*mobile app url start*/
    @GetMapping(path = "/mobile/job/listForSelection")
    public Object listForSelection(HttpServletRequest request) {
        JsonObject res = jobService.listForSelection(request);
        return res.toString();
    }
    /*mobile app url end*/


    @PostMapping(path = "/uploadJob")
    public ResponseEntity<?> uploadJob(MultipartHttpServletRequest request) {
        return ResponseEntity.ok(jobService.uploadJob(request));
    }

    @GetMapping(path = "/demoUrl")
    public Object demoUrl() {
        System.out.println("demoUrl called");
        return false;
    }

    @PostMapping(path = "/getItemReport")
    public Object getItemReport(@RequestBody Map<String, String> request) {
        JsonObject jsonObject = jobService.getItemReport(request);
        return jsonObject.toString();
    }
}
