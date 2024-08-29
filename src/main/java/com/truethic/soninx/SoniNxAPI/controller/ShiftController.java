package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.ShiftService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class ShiftController {
    @Autowired
    ShiftService shiftService;

    @PostMapping(path = "/createShift")
    public ResponseEntity<?> createShift(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return ResponseEntity.ok(shiftService.createShift(requestParam, request));
    }

    @GetMapping(path = "/listOfShifts")
    public Object listOfShifts(HttpServletRequest httpServletRequest) {
        JsonObject res = shiftService.listOfShifts(httpServletRequest);
        return res.toString();
    }

    @PostMapping(path = "/DTShift")
    public Object DTShift(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        System.out.println("request " + request);
        return shiftService.DTShift(request, httpServletRequest);
    }

    @PostMapping(path = "/findShift")
    public Object findShift(@RequestBody Map<String, String> requestParam) {
        return shiftService.findShift(requestParam);
    }

    @PostMapping(path = "/updateShift")
    public Object updateShift(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return shiftService.updateShift(requestParam, request);
    }

    @PostMapping(path = "/deleteShift")
    public Object deleteShift(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return shiftService.deleteShift(requestParam, request);
    }
}
