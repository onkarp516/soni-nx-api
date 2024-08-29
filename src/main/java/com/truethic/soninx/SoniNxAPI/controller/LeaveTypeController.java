package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.LeaveTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class LeaveTypeController {
    @Autowired
    private LeaveTypeService leaveTypeService;

    @PostMapping(path = "/createLeaveType")
    public Object createLeaveType(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return leaveTypeService.createLeaveType(requestParam, request);
    }

    @PostMapping(path = "/DTLeaveType")
    public Object DTLeaveType(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return leaveTypeService.DTLeaveType(request, httpServletRequest);
    }

    @PostMapping(path = "/findLeaveType")
    public Object findLeaveType(@RequestBody Map<String, String> requestParam) {
        return leaveTypeService.findLeaveType(requestParam);
    }

    @PostMapping(path = "/updateLeaveType")
    public Object updateLeaveType(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return leaveTypeService.updateLeaveType(requestParam, request);
    }

    @PostMapping(path = "/deleteLeaveType")
    public Object deleteLeaveType(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return leaveTypeService.deleteLeaveType(requestParam, request);
    }

    /*mobile app url start*/
    @GetMapping(path = "/mobile/leaveType/listForSelection")
    public Object listForSelection(HttpServletRequest request) {
        JsonObject res = leaveTypeService.listForSelection(request);
        return res.toString();
    }
    /*mobile app url end*/

    @GetMapping(path = "/mobile/leavesDashboard")
    public Object leavesDashboard(HttpServletRequest request) {
        JsonObject res = leaveTypeService.leavesDashboard(request);
        return res.toString();
    }
    /*mobile app url end*/
}
