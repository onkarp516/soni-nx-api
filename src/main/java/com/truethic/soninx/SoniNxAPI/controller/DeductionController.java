package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.DeductionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class DeductionController {
    @Autowired
    private DeductionService deductionService;

    @PostMapping(path = "createDeduction")
    public Object createDeduction(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return deductionService.createDeduction(requestParam, request);
    }

    @PostMapping(path = "DTDeduction")
    public Object DTDeduction(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return deductionService.DTDeduction(request, httpServletRequest);
    }

    @PostMapping(path = "findDeduction")
    public Object findDeduction(@RequestBody Map<String, String> request) {
        return deductionService.findDeduction(request);
    }

    @PostMapping(path = "updateDeduction")
    public Object updateDeduction(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return deductionService.updateDeduction(requestParam, request);
    }

    @PostMapping(path = "deleteDeduction")
    public Object deleteDeduction(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return deductionService.deleteDeduction(requestParam, request);
    }
    @GetMapping(path="get_deduction_list")
    public Object deductionList(HttpServletRequest request){
        JsonObject object=deductionService.deductionList(request);
        return object.toString();
    }
}
