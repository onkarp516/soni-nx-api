package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.ShiftAssignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ShiftAssignController {
    @Autowired
    private ShiftAssignService shiftAssignService;

    @PostMapping(path = "/employeeWiseShiftAssign")
    public Object employeeWiseShiftAssign(HttpServletRequest request) {
        return shiftAssignService.employeeWiseShiftAssign(request);
    }

    @GetMapping(path = "/getEmployeeWiseShiftAssign")
    public Object getEmployeeWiseShiftAssign(HttpServletRequest request) {
        JsonObject jsonObject = shiftAssignService.getEmployeeWiseShiftAssign(request);
        return jsonObject.toString();
    }


    @PostMapping(path = "/DTShiftAssign")
    public Object DTShiftAssign(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return shiftAssignService.DTShiftAssign(request, httpServletRequest);
    }

    @PostMapping(path = "/getNonShiftEmployee")
    public Object getNonShiftEmployee(HttpServletRequest request) {
        JsonObject jsonObject = shiftAssignService.getNonShiftEmployee(request);
        return jsonObject.toString();
    }


//    @PostMapping(path = "/getOrderByName")
//    public Object getOrderByName(HttpServletRequest request) {
//        JsonObject result=shiftAssignService.getOrderByName(request);
//        return result.toString();
//    }


    @PostMapping(path = "/deleteEmployeeShiftAssign")
    public Object deleteEmployeeShiftAssign(HttpServletRequest request) {
        return shiftAssignService.deleteEmployeeShiftAssign(request).toString();
    }

    @PostMapping(path = "/findEmployeeShift")
    public Object findEmployeeShift(HttpServletRequest request) {
        return shiftAssignService.findEmployeeShift(request).toString();
    }

    @PostMapping(path = "/updateEmployeeShift")
    public Object updateEmployeeShift(HttpServletRequest request) {
        return shiftAssignService.updateEmployeeShift(request).toString();
    }

    /* Mobile Urls Start */
    @GetMapping(path = "/mobile/getNextDayShiftOfEmployee")
    public Object getNextDayShiftOfEmployee(HttpServletRequest request) {
        return shiftAssignService.getNextDayShiftOfEmployee(request).toString();
    }
    /* Mobile Urls End */

}
