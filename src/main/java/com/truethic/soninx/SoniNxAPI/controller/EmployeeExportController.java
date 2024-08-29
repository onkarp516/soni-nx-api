package com.truethic.soninx.SoniNxAPI.controller;

import com.truethic.soninx.SoniNxAPI.model.EmployeeExport;
import com.truethic.soninx.SoniNxAPI.reporting.FileExporter;
import com.truethic.soninx.SoniNxAPI.service.EmployeeExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class EmployeeExportController {
    @Autowired
    private EmployeeExportService employeeExportService;

    @Autowired
    private FileExporter fileExporter;

    @PostMapping(path = "/create_employee")
    public ResponseEntity<?> createEmployee(HttpServletRequest request) {
        return ResponseEntity.ok(employeeExportService.createEmployee(request));
    }

    @GetMapping(path = "/export/csv")
    public void exportToCSV(HttpServletResponse response) throws IOException {
        List<EmployeeExport> listEmployee = employeeExportService.getAllEmployee();
        fileExporter.exportToCSV(listEmployee, response);
    }

    @GetMapping(path = "/export/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        List<EmployeeExport> listEmployee = employeeExportService.getAllEmployee();
        fileExporter.exportToExcel(listEmployee, response);
    }

    //    @GetMapping(path="/getAllEmployee")
//    public List<EmployeeExport> getAllEmployee()
//    {
//        return employeeExportService.getAllEmployee();
//    }
    @RequestMapping(path = "/getAllEmployee")
    public String getEmployeeList(Model model) {
        List<EmployeeExport> employeeList = new ArrayList<>();
        employeeList = employeeExportService.getAllEmployee();
        model.addAttribute("employeeList", employeeList);
        return "employeeView";
    }
}
