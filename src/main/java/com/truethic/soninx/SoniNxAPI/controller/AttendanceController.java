package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.AttendanceService;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

@RestController
public class AttendanceController {
    @Autowired
    private AttendanceService attendanceService;

    /*Mobile app urls start*/
    @PostMapping(path = "/mobile/saveAttendance")
    public Object saveAttendance( MultipartHttpServletRequest request) {
        JsonObject jsonObject = attendanceService.saveAttendance(request);
        return jsonObject.toString();
    }

    @GetMapping(path = "/mobile/checkAttendanceStatus")
    public Object checkAttendanceStatus(HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.checkAttendanceStatus(request);
        return jsonObject.toString();
    }

    @GetMapping(path = "/mobile/getPaymentUptoDate")
    public Object getPaymentUptoDate(HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getPaymentUptoDate(request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/mobile/getAttendanceList")
    public Object getAttendanceList(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getAttendanceList(jsonRequest, request);
        return jsonObject.toString();
    }
    /*Mobile app urls end*/


    @PostMapping(path = "/getEmpMonthlyPresenty")
    public Object getEmpMonthlyPresenty(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getEmpMonthlyPresenty(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/getSalaryReportMonthWise")
    public Object getSalaryReportMonthWise(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getSalaryReportMonthWise(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/getManualAttendanceReport")
    public Object getManualAttendanceReport(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getManualAttendanceReport(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/getLateAttendanceReport")
    public Object getLateAttendanceReport(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getLateAttendanceReport(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/mobile/getSingleDayAttendanceDetails")
    public Object getSingleDayAttendanceDetails(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject jsonObject = attendanceService.getSingleDayAttendanceDetails(jsonRequest, request);
        return jsonObject.toString();
    }


    @PostMapping(path = "/DTAttendance")
    public Object DTAttendance(@RequestBody Map<String, String> request) {
        return attendanceService.DTAttendance(request);
    }

    @PostMapping(path = "/DTAbsent")
    public Object DTAbsent(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return attendanceService.DTAbsent(request, httpServletRequest);
    }

    @PostMapping(path = "/submitEmployeeTodayWages")
    public Object submitEmployeeTodayWages(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return attendanceService.submitEmployeeTodayWages(jsonRequest, request);
    }

    @PostMapping(path = "/getAttendanceHistory")
    public Object getAttendanceHistory(@RequestBody Map<String, String> request) {
        return attendanceService.getAttendanceHistory(request);
    }


    @PostMapping(path = "/updateAttendance")
    public Object updateAttendance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return attendanceService.updateAttendance(requestParam, request);
    }


    @PostMapping(path = "/getEmployeeAttendanceHistory")
    public Object getEmployeeAttendanceHistory(@RequestBody Map<String, String> request) {
        JsonObject jsonObject = attendanceService.getEmployeeAttendanceHistory(request);
        return jsonObject.toString();
    }

    @GetMapping("/exportEmployeeAttendanceReport/{fromDate}/{toDate}/{employeeId}")
    public ResponseEntity<?> getAllCategoryOrdersReport(@PathVariable(value = "fromDate") String fromDate,
                                                        @PathVariable(value = "toDate") String toDate,
                                                        @PathVariable(value = "employeeId") String employeeId,
                                                        HttpServletRequest req) throws ParseException {
//        return ResponseEntity.ok(orderService.getAllCategoryOrdersReport(request));

        String filename = "emp_att.xlsx";
        InputStreamResource file = new InputStreamResource(attendanceService.getEmployeeAttendanceReport(fromDate,
                toDate, employeeId, req));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    @GetMapping("/getEmployeeSalaryReportInExcel/{employeeId}/{currentMonth}")
    public ResponseEntity<?> getEmployeeSalaryReportInExcel(@PathVariable(value = "employeeId") String employeeId,
                                                            @PathVariable(value = "currentMonth") String currentMonth,
                                                            HttpServletRequest req) throws ParseException {

        String filename = "emp_salary_report.xlsx";
        InputStreamResource file = new InputStreamResource(attendanceService.getEmployeeSalaryReportInExcel(employeeId, currentMonth, req));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

    @PostMapping(path = "/getEmployeeYearlyPresent")
    public Object getEmployeeYearlyPresent(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) throws Exception {
        JsonObject jsonObject = attendanceService.getEmployeeYearlyPresent(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/getEmployeeYearlyAbsent")
    public Object getEmployeeYearlyAbsent(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) throws Exception {
        JsonObject jsonObject = attendanceService.getEmployeeYearlyAbsent(jsonRequest, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/todayEmployeeAttendance")
    public Object todayEmployeeAttendance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return attendanceService.todayEmployeeAttendance(requestParam, request).toString();
    }

    @GetMapping(path = "/bo/exportExcelTodayEmployeeAttendance/{fromDate}/{attendanceDate}/{employeeId}/{attStatus}")
    public Object exportExcelTodayEmployeeAttendance(@PathVariable(value = "fromDate") String fromDate,
                                                     @PathVariable(value = "attendanceDate") String attendanceDate,
                                                     @PathVariable(value = "employeeId") String employeeId,
                                                     @PathVariable(value = "attStatus") String attStatus,
                                                     HttpServletRequest request) {

        String filename = "emp_payment_sheet.xlsx";
        InputStreamResource file = new InputStreamResource(attendanceService.exportExcelTodayEmployeeAttendance(
                fromDate, attendanceDate, employeeId, attStatus, request));

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

 /*   @PostMapping(path = "/todayEmployeeTaskData")
    public Object todayEmployeeTaskData(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return attendanceService.todayEmployeeTaskData(requestParam, request).toString();
    }*/

    @PostMapping(path = "/bo/addManualAttendance")
    public Object addManualAttendance(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return attendanceService.addManualAttendance(requestParam, request).toString();
    }

    @PostMapping(path = "/bo/recalculateEmployeeTasksAttendance")
    public Object recalculateEmployeeTasksAttendance(HttpServletRequest request) throws ParseException {
        return attendanceService.recalculateEmployeeTasksAttendance(request).toString();
    }

    @PostMapping(path = "/bo/recalculateAllEmployeeTasksAttendance")
    public Object recalculateAllEmployeeTasksAttendance(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) throws ParseException {
        return attendanceService.recalculateAllEmployeeTasksAttendance(jsonRequest, request).toString();
    }

    @PostMapping(path = "/bo/approveSalaryAttendance")
    public Object approveSalaryAttendance(HttpServletRequest request) {
        return attendanceService.approveSalaryAttendance(request).toString();
    }

    @PostMapping(path = "/bo/deleteAttendance")
    public Object deleteAttendance(HttpServletRequest request) {
        return attendanceService.deleteAttendance(request).toString();
    }

    @PostMapping(path = "/getEmployeeSalaryReport")
    public Object getEmployeeSalaryReport(HttpServletRequest request) {
        return attendanceService.getEmployeeSalaryReport(request).toString();
    }

    @PostMapping(path = "/getEmployeeEarningReport")
    public Object getEmployeeEarningReport(HttpServletRequest request) {
        return attendanceService.getEmployeeEarningReport(request).toString();
    }

    /* PRACTICE URLS */

    @PostMapping(path = "/calculateTime")
    public Object calculateTime(@RequestBody Map<String, String> requestParam, HttpServletRequest request) throws ParseException {
//        return attendanceService.calculateTime(requestParam, request).toString();
        LocalDateTime l1 = LocalDateTime.parse(requestParam.get("fromDate"));
        LocalDateTime l2 = LocalDateTime.parse(requestParam.get("toDate"));
        Utility utility = new Utility();
        return utility.getDateTimeDiffInTime(l1, l2).toString();
    }

    @PostMapping(path = "/getTimeDiffFromTimes")
    public Object getTimeDiffFromTimes(@RequestBody Map<String, String> requestParam, HttpServletRequest request) throws ParseException {
        LocalTime l1 = LocalTime.parse(requestParam.get("fromTime"));
        LocalTime l2 = LocalTime.parse(requestParam.get("toTime"));
        Utility utility = new Utility();
        return utility.getTimeDiffFromTimes(l1, l2).toString();
    }

    @PostMapping(path = "/getYearFormDate")
    public String getYearFormDate(HttpServletRequest request) throws ParseException {
        try {
            SimpleDateFormat df1 = new SimpleDateFormat("yy");
            LocalDate l1 = LocalDate.parse("2022-10-15");
            String timeInHHMMSS = "";
            /*String yr = String.valueOf(l1.getYear());
            System.out.println("yr " + yr);
            System.out.println("yr " + String.valueOf(yr).substring(2));
            timeInHHMMSS = yr.substring(2);
            System.out.println("timeInHHMMSS " + timeInHHMMSS);*/


//            Date d = new Date();
            Date d = new SimpleDateFormat("yyyy-MM-dd").parse(l1.toString());

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
            String format = dateFormat.format(d);
            System.out.println("Current date and time = " + format);
            System.out.printf("Four-digit Year = %TY", d);
            System.out.printf("Two-digit Year = %ty", d);

            timeInHHMMSS = String.format("%ty", d);
            return timeInHHMMSS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }
    /* PRACTICE URLS */


    //Automatic Mark Late and Half day
    @PostMapping(path = "/bo/setLateAndHalfDayOfAllEmployees")
    public Object setLateAndHalfDayOfAllEmployees(HttpServletRequest request) {
        return attendanceService.setLateAndHalfDayOfAllEmployees(request).toString();
    }
}
