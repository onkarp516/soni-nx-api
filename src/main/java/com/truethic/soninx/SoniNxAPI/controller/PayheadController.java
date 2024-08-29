package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.PayheadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class PayheadController {
    @Autowired
    private PayheadService payheadService;

    @PostMapping(path = "/createPayhead")
    public Object createPayhead(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return payheadService.createPayhead(requestParam, request);
    }

    @PostMapping(path = "/DTPayhead")
    public Object DTPayhead(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return payheadService.DTPayhead(request, httpServletRequest);
    }

    @GetMapping(path = "/payheadList")
    public Object payheadList() {
        JsonObject result = payheadService.payheadList();
        return result.toString();
    }

    @PostMapping(path = "/findPayhead")
    public Object findPayhead(@RequestBody Map<String, String> request) {
        return payheadService.findPayhead(request);
    }

    @PostMapping(path = "/updatePayhead")
    public Object updatePayhead(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return payheadService.updatePayhead(requestParam, request);
    }

    @PostMapping(path = "/deletePayhead")
    public Object deletePayhead(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return payheadService.deletePayhead(requestParam, request);
    }

    @GetMapping(path="/get_payhead_list")
    public Object getpayheadList(HttpServletRequest request){
        JsonObject object= payheadService.getpayheadList(request);
        return object.toString();
    }
}
