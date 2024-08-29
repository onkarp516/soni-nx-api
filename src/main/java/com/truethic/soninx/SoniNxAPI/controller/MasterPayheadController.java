package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.MasterPayheadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class MasterPayheadController {
    @Autowired
    private MasterPayheadService masterPayheadService;

    @PostMapping(path = "/createMasterPayhead")
    public Object createMasterPayhead(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return masterPayheadService.createMasterPayhead(jsonRequest, request);
    }

    @PostMapping(path = "/DTMasterPayhead")
    public Object DTMasterPayhead(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return masterPayheadService.DTMasterPayhead(request, httpServletRequest);
    }

    @PostMapping(path = "/findMasterPayhead")
    public Object findMasterPayhead(@RequestBody Map<String, String> request) {
        return masterPayheadService.findMasterPayhead(request);
    }
}
