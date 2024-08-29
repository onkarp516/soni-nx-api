package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeRepository;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class PayrollController {
    @Autowired
    private PayrollService payrollService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping(path = "/getCurrentMonthPayslip")
    public Object getCurrentMonthPayslip(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = payrollService.getCurrentMonthPayslip(jsonRequest, request);
        return jsonObject.toString();
    }


    @PostMapping(path = "/getEmpSalaryslip")
    public Object getEmpSalarySlip(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = payrollService.getEmpSalaryslip(jsonRequest, request);
        return jsonObject.toString();
    }

    //    @GetMapping(path = "/exportPdfEmpSalaryslip/{fromMonth}/{employeeId}")
    @GetMapping(path = "/exportPdfEmpSalarySlip")
    public void exportPdfEmpSalarySlip(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Employee> employees = employeeRepository.findByStatus(true);
            String data = "admin";
            HtmlConverter.convertToPdf(new File(
                            "/media/rahul/87a9770f-a3bb-46aa-ad9e-a57f977410b8/truethic_git/renukaengg/renukaenggapi/uploads/input.html"),
                    new File("demo-html.pdf"), (ConverterProperties) employees);

        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PostMapping(path = "/getDashboardStatistics")
    public Object getDashboardStatistics(HttpServletRequest request) {
        JsonObject jsonObject = payrollService.getDashboardStatistics(request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/recalculateEmpSalaryForMonth")
    public Object recalculateEmpSalaryForMonth(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return payrollService.recalculateEmpSalaryForMonth(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/getEmployeePaymentSheet")
    public Object getEmployeePaymentSheet(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return payrollService.getEmployeePaymentSheet(jsonRequest, request).toString();
    }

    @GetMapping("/bo/getExcelEmployeePaymentSheet/{month}/{companyId}")
    public Object getExcelEmployeePaymentSheet(@PathVariable(value = "month") String month,
                                               @PathVariable(value = "companyId") String companyId,
                                               HttpServletRequest request) {

        String filename = "emp_payment_sheet.xlsx";
        InputStreamResource file = new InputStreamResource(payrollService.getExcelEmployeePaymentSheet(month, companyId, request));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

}
