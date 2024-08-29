package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.ControlMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ControlMetodController {
    @Autowired
    private ControlMethodService controlMethodService;

    @PostMapping(path = "/createControlMethod")
    public Object createControlMethod(HttpServletRequest request) {
        return controlMethodService.createControlMethod(request).toString();
    }

    @PostMapping(path = "/DTControlMethod")
    public Object DTControlMethod(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return controlMethodService.DTControlMethod(request, httpServletRequest);
    }

    @PostMapping(path = "/findControlMethod")
    public Object findControlMethod(@RequestBody Map<String, String> request) {
        return controlMethodService.findControlMethod(request);
    }

    @PostMapping(path = "/updateControlMethod")
    public Object updateControlMethod(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return controlMethodService.updateControlMethod(requestParam, request);
    }

    @PostMapping(path = "/deleteControlMethod")
    public Object deleteControlMethod(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return controlMethodService.deleteControlMethod(requestParam, request);
    }

    @GetMapping(path = "/controlmethod-list")
    public Object getControlMethod() {
        return controlMethodService.getControlMethod();
    }

}
