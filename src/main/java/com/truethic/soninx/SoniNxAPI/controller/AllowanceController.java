package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.AllowanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AllowanceController {
    @Autowired
    private AllowanceService allowanceService;

    @PostMapping(path = "/createAllowance")
    public Object createAllowance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return allowanceService.createAllowance(requestParam, request);
    }

    @PostMapping(path = "/DTAllowance")
    public Object DTAllowance(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return allowanceService.DTAllowance(request,httpServletRequest);
    }

    @PostMapping(path = "/findAllowance")
    public Object findAllowance(@RequestBody Map<String, String> requestParam) {
        return allowanceService.findAllowance(requestParam);
    }

    @PostMapping(path = "/updateAllowance")
    public Object updateAllowance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return allowanceService.updateAllowance(requestParam, request);
    }

    @PostMapping(path = "/deleteAllowance")
    public Object deleteAllowance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return allowanceService.deleteAllowance(requestParam, request);
    }
}
