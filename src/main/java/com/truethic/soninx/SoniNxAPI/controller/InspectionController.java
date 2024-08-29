package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class InspectionController {
    @Autowired
    private InspectionService inspectionService;


    @PostMapping(path = "/createLineInspection")
    public Object createLineInspection(HttpServletRequest request) {
        return inspectionService.createLineInspection(request).toString();
    }


    @PostMapping(path = "/DTLineInspection")
    public Object DTLineInspection(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return inspectionService.DTLineInspection(request, httpServletRequest);
    }

    @PostMapping(path = "/deleteLineInspection")
    public Object deleteLineInspection(HttpServletRequest request) {
        return inspectionService.deleteLineInspection(request).toString();
    }

    @PostMapping(path = "/findLineInspection")
    public Object findLineInspection(HttpServletRequest request) {
        return inspectionService.findLineInspection(request).toString();
    }

    @PostMapping(path = "/updateLineInspection")
    public Object updateLineInspection(HttpServletRequest request) {
        return inspectionService.updateLineInspection(request).toString();
    }


   /* @PostMapping(path = "/mobile/getDrawingSizes")
    public Object getDrawingSizes(@RequestBody Map<String, String> request) {
        JsonObject res = inspectionService.getDrawingSizes(request);
        return res.toString();
    }*/
}
