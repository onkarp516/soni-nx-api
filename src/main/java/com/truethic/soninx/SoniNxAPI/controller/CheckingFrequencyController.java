package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.CheckingFrequencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class CheckingFrequencyController {
    @Autowired
    private CheckingFrequencyService frequencyService;

    @PostMapping(path = "/createCheckingFrequency")
    public Object createCheckingFrequency(HttpServletRequest request) {
        return frequencyService.createCheckingFrequency(request).toString();
    }

    @PostMapping(path = "/DTCheckingFrequency")
    public Object DTCheckingFrequency(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return frequencyService.DTCheckingFrequency(request, httpServletRequest);
    }

    @PostMapping(path = "/findCheckingFrequency")
    public Object findCheckingFrequency(@RequestBody Map<String, String> request) {
        return frequencyService.findCheckingFrequency(request);
    }

    @PostMapping(path = "/updateCheckingFrequency")
    public Object updateCheckingFrequency(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return frequencyService.updateCheckingFrequency(requestParam, request);
    }

    @PostMapping(path = "/deleteCheckingFrequency")
    public Object deleteCheckingFrequency(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return frequencyService.deleteCheckingFrequency(requestParam, request);
    }

    @GetMapping(path = "/checkingfrequency-list")
    public Object getCheckingFrequency() {
        return frequencyService.getCheckingFrequency();
    }
}
