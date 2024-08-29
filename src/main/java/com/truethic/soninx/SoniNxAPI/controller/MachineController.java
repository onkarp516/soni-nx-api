package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.MachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class MachineController {
    @Autowired
    MachineService machineService;

    @PostMapping(path = "/createMachine")
    public ResponseEntity<?> createMachine(HttpServletRequest request) {
        return ResponseEntity.ok(machineService.createMachine(request));
    }

    @GetMapping(path = "/machine-list")
    public Object getMachine() {
        return machineService.getMachine();
    }

    @PostMapping(path = "/DTMachine")
    public Object DTMachine(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return machineService.DTMachine(request,httpServletRequest);
    }

    @PostMapping(path = "/deleteMachine")
    public Object deleteMachine(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return machineService.deleteMachine(requestParam, request);
    }

    @PostMapping(path = "/findMachine")
    public Object findMachine(@RequestBody Map<String, String> requestParam) {
        return machineService.findMachine(requestParam);
    }

    @PostMapping(path = "/updateMachine")
    public Object updateMachine(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return machineService.updateMachine(requestParam, request);
    }

    @GetMapping(path = "/machineListForSelection")
    public Object machineListForSelection(HttpServletRequest request) {
        JsonObject res = machineService.machineListForSelection(request);
        return res.toString();
    }


    /*mobile app url start*/

    @GetMapping(path = "/mobile/machine/listForSelection")
    public Object listForSelection(HttpServletRequest request) {
        JsonObject res = machineService.listForSelection(request);
        return res.toString();
    }

    /*mobile app url end*/

    @PostMapping(path = "/getMachineReport")
    public Object getMachineReport(@RequestBody Map<String, String> request) {
        JsonObject jsonObject = machineService.getMachineReport(request);
        return jsonObject.toString();
    }
}
