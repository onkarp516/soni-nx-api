package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.AdvancePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AdvancePaymentController {
    @Autowired
    private AdvancePaymentService advancePaymentService;

    /*Mobile app urls start*/
    @PostMapping(path = "/mobile/saveAdvancePayment")
    public Object saveAdvancePayment(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return advancePaymentService.saveAdvancePayment(requestParam, request);
    }

    @GetMapping(path = "/mobile/listOfAdvancePayment")
    public Object listOfAdvancePayment(HttpServletRequest request) {
        JsonObject res = advancePaymentService.listOfAdvancePayment(request);
        return res.toString();
    }

    /*Mobile app urls end*/

    @PostMapping(path = "/DTAdvancePayment")
    public Object DTAdvancePayment(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return advancePaymentService.DTAdvancePayment(request, httpServletRequest);
    }

    @PostMapping(path = "/rejectAdvancePayment")
    public Object rejectAdvancePayment(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return advancePaymentService.rejectAdvancePayment(jsonRequest, request);
    }

    @PostMapping(path = "/approveAdvancePayment")
    public Object approveAdvancePayment(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return advancePaymentService.approveAdvancePayment(jsonRequest, request);
    }
}
