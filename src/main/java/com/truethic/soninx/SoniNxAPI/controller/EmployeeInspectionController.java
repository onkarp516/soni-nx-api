package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.service.EmployeeInspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

@RestController
public class EmployeeInspectionController {
    @Autowired
    EmployeeInspectionService employeeInspectionService;

    @PostMapping(path = "/mobile/createLineInspection")
    public Object createMobileLineInspection(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.createMobileLineInspection(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/getTaskLineInspectionList")
    public Object getTaskLineInspectionList(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.getTaskLineInspectionList(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/getTaskLineInspectionListForSupervisor")
    public Object getTaskLineInspectionListForSupervisor(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.getTaskLineInspectionListForSupervisor(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/manualCreateLineInspection")
    public Object manualCreateLineInspection(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.manualCreateLineInspection(jsonRequest, request).toString();
    }


    @PostMapping(path = "/mobile/getManualLineInspectionList")
    public Object getManualLineInspectionList(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.getManualLineInspectionList(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getLineInspectionListWithFilter")
    public Object getLineInspectionListWithFilter(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.getLineInspectionListWithFilter(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getFinalLineInspectionListWithFilter")
    public Object getFinalLineInspectionListWithFilter(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeInspectionService.getFinalLineInspectionListWithFilter(jsonRequest, request).toString();
    }

    @PostMapping("/bo/exportExcelEmployeeInspection")
    public Object exportExcelEmployeeInspection(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {

        String filename = "line_inspection_sheet.xlsx";
        InputStreamResource file = new InputStreamResource(employeeInspectionService.exportExcelEmployeeInspection(jsonRequest, request));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    @PostMapping("/bo/exportExcelFinalEmployeeInspection")
    public Object exportExcelFinalEmployeeInspection(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) throws IOException {

        String filename = "line_inspection_sheet.xlsx";
        InputStreamResource file = new InputStreamResource(employeeInspectionService.exportExcelFinalEmployeeInspection(jsonRequest, request));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);

    }

    @PostMapping(path = "/bo/getMachineListFromInspectionData")
    public Object getMachineListFromInspectionData(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return employeeInspectionService.getMachineListFromInspectionData(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getJobListFromInspectionData")
    public Object getJobListFromInspectionData(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return employeeInspectionService.getJobListFromInspectionData(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getJobOperationListFromInspectionData")
    public Object getJobOperationListFromInspectionData(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return employeeInspectionService.getJobOperationListFromInspectionData(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getEmployeeListFromInspectionData")
    public Object getEmployeeListFromInspectionData(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return employeeInspectionService.getEmployeeListFromInspectionData(jsonRequest, request).toString();
    }
}
