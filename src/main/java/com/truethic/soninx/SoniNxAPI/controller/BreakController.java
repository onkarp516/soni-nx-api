package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.WorkBreakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class BreakController {
    @Autowired
    private WorkBreakService workBreakService;

    @PostMapping(path = "/createBreak")
    public Object createBreak(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return workBreakService.createBreak(requestParam, request);
    }

    @PostMapping(path = "/DTBreak")
    public Object DTBreak(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return workBreakService.DTBreak(request, httpServletRequest);
    }

    @PostMapping(path = "/findBreak")
    public Object findBreak(@RequestBody Map<String, String> request) {
        return workBreakService.findBreak(request);
    }

    @PostMapping(path = "/updateBreak")
    public Object updateBreak(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return workBreakService.updateBreak(requestParam, request);
    }

    @PostMapping(path = "/deleteBreak")
    public Object deleteBreak(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return workBreakService.deleteBreak(requestParam, request);
    }

    @GetMapping(path = "/workBreakListForSelection")
    public Object workBreakListForSelection(HttpServletRequest request) {
        JsonObject res = workBreakService.workBreakListForSelection(request);
        return res.toString();
    }

    /*mobile app url start*/
    @GetMapping(path = "/mobile/workBreak/listForSelection")
    public Object listForSelection(HttpServletRequest request) {
        JsonObject res = workBreakService.listForSelection(request);
        return res.toString();
    }
    /*mobile app url end*/
}
