package com.truethic.soninx.SoniNxAPI.service;


import com.google.gson.*;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageProperties;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageService;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.viewRepository.AttendanceViewRepository;
import com.truethic.soninx.SoniNxAPI.dto.AttendanceDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.EmployeeDTDTO;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.TaskDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import com.truethic.soninx.SoniNxAPI.views.AttendanceView;
import com.truethic.soninx.SoniNxAPI.views.TaskView;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.apache.commons.math3.util.Precision;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
public class AttendanceService {
    private static final Logger attendanceLogger = LoggerFactory.getLogger(AttendanceService.class);
    static String[] empAttHEADERs = {"Employee Name", "Att. Date", "In Time", "Out Time", "Working Hours", "Machine",
            "Item/Break", "Operation", "Cycle Time", "REQ. Qty.", "ACT. Qty.", "OK Qty.", "M/R Qty.", "R/W Qty.",
            "D/F Qty.", "U/M Qty.", "Actual Time (MIN.)", "Break Time (MIN.)", "Wages In Hr.", "Wages In Pt.",
            "Wages In Pcs.", "Break Wages", "Net Pcs Wages", "Wages Per Day"};
    static String[] empSalHEADERs = {"Employee Name", "Att. Date", "Designation", "Employee Id",
            "In Time", "Out Time", "Total Time", "Lunch Time (Min).", "Working Hours", "Wages Per Day", "Wages Per Hour"};
    static String empAttSHEET = "Emp_Attendance";
    static String empSalSHEET = "Emp_Salary";
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeLeaveRepository employeeLeaveRepository;
    @Autowired
    private EmployeePayrollRepository employeePayrollRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private MasterPayheadRepository masterPayheadRepository;
    @Autowired
    private PayheadRepository payheadRepository;
    @Autowired
    private AdvancePaymentRepository advancePaymentRepository;
    @Autowired
    private TaskMasterRepository taskMasterRepository;
    @Autowired
    private AllowanceRepository allowanceRepository;
    @Autowired
    private DeductionRepository deductionRepository;
    @Autowired
    private TaskViewRepository taskViewRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private AttendanceHistoryRepository attendanceHistoryRepository;
    @Autowired
    private ShiftAssignRepository shiftAssignRepository;
    @Autowired
    private ShiftRepository shiftRepository;
    @Autowired
    private AttendanceViewRepository attendanceViewRepository;
    @Autowired
    private WorkBreakRepository workBreakRepository;
    @Autowired
    private Utility utility;
    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    private TaskMasterRepository taskRepository;

    public static List<String> getListMonths(Date dateFrom, Date dateTo, Locale locale, DateFormat df) {
        Calendar calendar = Calendar.getInstance(locale);
        calendar.setTime(dateFrom);

        List<String> months = new ArrayList<>();

        while (calendar.getTime().getTime() <= dateTo.getTime()) {
            months.add(df.format(calendar.getTime()));
            calendar.add(Calendar.MONTH, 1);
        }

        return months;
    }

    public JsonObject saveAttendance(MultipartHttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Map<String, String[]> paramMap = request.getParameterMap();
        LocalTime timeToCompare = employee.getShift().getThreshold();
        try {
            LocalDate attendanceDate = LocalDate.now();
            int daysInMonth = getTotalDaysFromYearAndMonth(attendanceDate.getYear(), attendanceDate.getMonthValue());
            System.out.println("totalDays" + daysInMonth);
//            Double wagesPerDay = utility.getEmployeeWages(employee.getId());
            Double wagesPerDay = employee.getExpectedSalary() / daysInMonth;
            System.out.println("wagesPerDay =" + wagesPerDay);
            if (wagesPerDay == null) {
                System.out.println("employee wagesPerDay =" + wagesPerDay);
                System.out.println("Your salary not updated! Please contact to Admin +++++++++++++++++++++++++++");
                responseMessage.addProperty("message","Your salary not updated! Please contact to Admin");
                responseMessage.addProperty("responseStatus",HttpStatus.BAD_REQUEST.value());
            } else {
                double wagesPerHour = (wagesPerDay / utility.getTimeInDouble(employee.getShift().getWorkingHours().toString()));
                /*double wagesPerMinute = (wagesPerHour / 60.0);*/
//                double wagesPoint = (wagesPerDay / 100.0);
                if (Boolean.parseBoolean(request.getParameter("attendanceStatus"))) {
                    Attendance attendanceExist = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), LocalDate.now(), true);

                    if (attendanceExist == null) {
                        Attendance attendance = new Attendance();
                        attendance.setAttendanceDate(LocalDate.now());
                        attendance.setEmployee(employee);
                        attendance.setShift(getEmployeeShift(LocalDate.now(), employee.getId(),employee.getShift().getId()));
                        LocalDateTime inTime = LocalDateTime.now();
                        System.out.println("inTime " + inTime);
                        attendance.setCheckInTime(inTime);
                        attendance.setWagesPerDay(wagesPerDay);
                        attendance.setWagesPerHour(wagesPerHour);
                        if(inTime.toLocalTime().compareTo(timeToCompare) > 0)
                            attendance.setIsLate(true);
                        attendance.setCreatedBy(employee.getId());
                        attendance.setCreatedAt(LocalDateTime.now());
                        attendance.setStatus(true);
                        attendance.setInstitute(employee.getInstitute());
                        if (request.getFile("punch_in_image") != null) {
                            MultipartFile image = request.getFile("punch_in_image");
                            fileStorageProperties.setUploadDir("." + File.separator + "uploads" + File.separator + "punch-in" + File.separator);
                            String imagePath = fileStorageService.storeFile(image, fileStorageProperties);
                            if (imagePath != null) {
                                attendance.setPunchInImage(File.separator + "uploads" + File.separator + "punch-in" + File.separator + imagePath);
                            } else {
                                responseMessage.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
                                responseMessage.addProperty("message","Failed to upload image. Please try again!");
                                return responseMessage;
                            }
                        }
                        try {
                            Attendance attendance1 = attendanceRepository.save(attendance);
                            responseMessage.addProperty("message","Check-in successfully");
                            responseMessage.addProperty("attendance_id",attendance1.getId());
                            responseMessage.addProperty("responseStatus",HttpStatus.OK.value());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Exception " + e.getMessage());
                            responseMessage.addProperty("message","Failed to checkin");
                            responseMessage.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
                        }
                    } else {
                        responseMessage.addProperty("message","Already checked In");
                        responseMessage.addProperty("responseStatus",HttpStatus.BAD_REQUEST.value());
                    }
                } else if (!Boolean.parseBoolean(request.getParameter( "attendanceStatus"))) {
//                    Attendance attendance = attendanceRepository.findTop1ByEmployeeIdOrderByIdDesc(employee.getId());
                    Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), LocalDate.now(), true);
                    if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() == null) {
                        try {
                            LocalDateTime outTime = LocalDateTime.now();
                            System.out.println("outTime " + outTime);
                            LocalTime timeDiff = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), outTime);
//                            LocalTime time = employee1.getShift().getWorkingHours();
                            String[] timeParts = timeDiff.toString().split(":");
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            double workedHours = utility.getTimeInDouble(hours+":"+minutes);
                            if(workedHours < 5)
                                attendance.setIsHalfDay(true);
                            attendance.setCheckOutTime(outTime);
                            attendance.setUpdatedAt(LocalDateTime.now());
                            attendance.setUpdatedBy(employee.getId());
                            if (request.getFile("punch_out_image") != null) {
                                MultipartFile image = request.getFile("punch_out_image");
                                fileStorageProperties.setUploadDir("." + File.separator + "uploads" + File.separator + "punch-out" + File.separator);
                                String imagePath = fileStorageService.storeFile(image, fileStorageProperties);
                                if (imagePath != null) {
                                    attendance.setPunchInImage(File.separator + "uploads" + File.separator + "punch-out" + File.separator + imagePath);
                                } else {
                                    responseMessage.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
                                    responseMessage.addProperty("message","Failed to upload image. Please try again!");
                                    return responseMessage;
                                }
                            }

                            LocalTime totalTime = LocalTime.parse("00:00:00");
                            LocalDateTime firstTaskStartTime = null;
                            LocalDateTime lastTaskEndTime = null;

                            System.out.println("employee.getDesignation().getCode() --->" + employee.getDesignation().getCode());
                            if (employee.getDesignation().getCode().equalsIgnoreCase("l3") ||
                                    employee.getDesignation().getCode().equalsIgnoreCase("l2")) {
                                firstTaskStartTime = attendance.getCheckInTime();
                                lastTaskEndTime = outTime;
                                /* From Attendance Data */
                                totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), outTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendance.setTotalTime(totalTime);
                            }

                            double actualWorkTime = taskMasterRepository.getSumOfActualWorkTime(attendance.getId());
                            attendance.setActualWorkTime(actualWorkTime);
                            double lunchTimeInMin = 0;
                            LocalTime lunchTime = null;

                            WorkBreak workBreak = workBreakRepository.findByBreakName(employee.getInstitute().getId());
                            TaskMaster taskMaster = null;
                            if (workBreak != null) {
                                lunchTimeInMin = taskMasterRepository.getSumOfLunchTime(attendance.getId(), workBreak.getId(), true, false);
                                /*taskMaster = taskMasterRepository.findByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(attendance.getId(), workBreak.getId(), true, false);
                                if (taskMaster != null) {
                                    lunchTimeInMin = taskMaster.getTotalTime();
                                    lunchTime = taskMaster.getWorkingTime();
                                }*/
                            }
                            attendance.setLunchTime(lunchTimeInMin);

                            int s = 0;
                            int attendanceSec = 0;
                            double totalMinutes = 0;
                            double attendanceMinutes = 0;
                            if (totalTime != null) {
                                // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                                s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                                attendanceSec = Math.abs(s);
                                System.out.println("attendanceSec " + attendanceSec);
                                attendanceMinutes = ((double) attendanceSec / 60.0);

                                totalMinutes = attendanceMinutes;
                                if (taskMaster != null && !taskMaster.getWorkDone()) {
                                    totalMinutes = attendanceMinutes - lunchTimeInMin;
                                }

                                System.out.println("attendanceMinutes " + attendanceMinutes);
                                System.out.println("totalMinutes " + totalMinutes);

                                double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                                double wagesHourBasis = wagesPerHour * workingHours;
                                System.out.println("workingHours " + workingHours);
                                System.out.println("wagesPerHour " + wagesPerHour);
                                System.out.println("wagesHourBasis " + wagesHourBasis);

                                attendance.setWorkingHours(workingHours);
                                attendance.setWagesHourBasis(wagesHourBasis);
                            }

                            if (paramMap.containsKey("remark")) {
                                String remark = request.getParameter("remark");
                                if (!remark.equalsIgnoreCase("")) {
                                    attendance.setRemark(remark);
                                }
                            }

                            Attendance attendance1 = attendanceRepository.save(attendance);

                            /*try {
                                updateSalaryForDay(attendance1);
                            } catch (Exception e) {
                                attendanceLogger.error("updateSalaryForDay Exception ===> " + e);
                                System.out.println("updateSalaryForDay Exception ===>" + e.getMessage());
                                e.printStackTrace();
                            }*/
                            responseMessage.addProperty("message","Checkout successfully");
                            responseMessage.addProperty("responseStatus",HttpStatus.OK.value());
                        } catch (Exception e) {

                            attendanceLogger.error("Failed to checkout Exception ===> " + e);
                            e.printStackTrace();
                            System.out.println("Exception " + e.getMessage());
                            responseMessage.addProperty("message","Failed to checkout");
                            responseMessage.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
                        }
                    } else if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
                        attendanceLogger.info("attendnace", "You already done checkout ...........");
                        responseMessage.addProperty("message","You already done checkout");
                        responseMessage.addProperty("responseStatus",HttpStatus.NOT_FOUND.value());
                    } else {
                        responseMessage.addProperty("message","Please process checkin first");
                        responseMessage.addProperty("responseStatus",HttpStatus.NOT_FOUND.value());
                    }
                }
            }
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data ===> " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message","Data inconsistency, please validate data");
            responseMessage.addProperty("responseStatus",HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    private Shift getEmployeeShift(LocalDate now, Long id, Long shiftId) {
        Shift shift = null;
        ShiftAssign shiftAssign = shiftAssignRepository.getDataFromShiftAssign(now, id);

        if (shiftAssign != null && shiftAssign.getEmployee() != null) {
            shift = shiftAssign.getShift();
        }

        if (shift == null) {
            shift = shiftRepository.findByIdAndStatus(shiftId, true);
            LocalTime currentTime = LocalTime.now();
            System.out.println("currentTime " + currentTime);

            LocalTime midday = LocalTime.of(12, 0); // 12:00 Uhr
            System.out.println("midday " + midday);
            if (currentTime.isAfter(midday)) {
                shift = shiftRepository.findByIdAndStatus(shiftId, true);
            }
        }
        return shift;
    }


    public void updateSalaryForDay(Attendance attendance1, Users users) {
        try {
            Long employeeId = attendance1.getEmployee().getId();
            LocalDate attendanceDate = attendance1.getAttendanceDate();
            String monthValue = attendanceDate.getMonthValue() < 10 ? "0"+attendanceDate.getMonthValue() : String.valueOf(attendanceDate.getMonthValue());
            String yearMonth = attendanceDate.getYear() + "-" + monthValue;
            int year = attendanceDate.getYear();

            int month = attendanceDate.getMonthValue();

            Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
            String wagesType = employee.getEmployeeWagesType();

            int daysInMonth = getTotalDaysFromYearAndMonth(year, month);
            System.out.println("totalDays" + daysInMonth);
            int totalDays = 0;
            if(daysInMonth == 31){
                totalDays = 27;
            } else if (daysInMonth == 30){
                totalDays = 26;
            } else {
                totalDays = 24;
            }
//            Double wagesPerDay = employee.getExpectedSalary() / daysInMonth;
            double monthlyPay = employee.getExpectedSalary();
            Double wagesPerDaySalary = monthlyPay / daysInMonth;
            double perDaySalary = 0;
            double perHourSalary = 0;
            if (wagesPerDaySalary != null) {
                perDaySalary = wagesPerDaySalary;
                perHourSalary = (wagesPerDaySalary / utility.getTimeInDouble(employee.getShift().getWorkingHours().toString()));
            }

            System.out.println("perDaySalary " + perDaySalary);
            System.out.println("perHourSalary " + perHourSalary);
            double totalDaysInMonth = attendanceRepository.getPresentDaysOfEmployeeOfMonth(year, month, employeeId, true, "approve");
            System.out.println("totalDaysInMonth " + totalDaysInMonth);

            double totalHoursInMonth = 0;
            double netSalaryInHours = 0;
            double netSalaryInDays = 0;
            double final_day_salary = 0;

            List<Attendance> attendanceList = attendanceRepository.getAttendanceListOfEmployee(year, month, employeeId, true, "approve");
//            for (int i = 0; i < attendanceList.size(); i++) {
//                Object[] attObj = attendanceList.get(i);
//                System.out.println("attendance Id=" + attObj[0].toString());
//
//                if (attObj[11].toString().equalsIgnoreCase("day"))
//                    netSalaryInDays += Double.parseDouble(attObj[10].toString());
//                if (attObj[11].toString().equalsIgnoreCase("hr"))
//                    netSalaryInHours += Double.parseDouble(attObj[10].toString());
//
//                final_day_salary += Double.parseDouble(attObj[10].toString());
//                totalHoursInMonth += Double.parseDouble(attObj[34].toString());
//            }
            double presentDays = 0;
            Integer leaveDays = 0;
            Integer absentDays = 0;
            double extraDays = 0;
            double halfDays = 0;
            double extraHalfDays = 0;
//            double workedHours = 0.0;
            for (Attendance attendance: attendanceList) {
                int hours = 0;
                int minutes = 0;
                if (attendance != null) {
                    LocalDateTime checkInTime = attendance.getCheckInTime();
                    LocalDateTime checkOutTime = attendance.getCheckOutTime();
                    if(checkOutTime != null){
//                        LocalTime timeDiff = utility.getDateTimeDiffInTime(checkInTime, checkOutTime);
////                            LocalTime time = employee.getShift().getWorkingHours();
//                        String[] timeParts = timeDiff.toString().split(":");
//                        hours = Integer.parseInt(timeParts[0]);
//                        minutes = Integer.parseInt(timeParts[1]);
//                        workedHours = utility.getTimeInDouble(hours+":"+minutes);
//                        if(workedHours > 0){
//
//                        }
                        if (attendance.getAttendanceDate().getDayOfWeek().toString().contains(employee.getWeeklyOffDay())) {
                            if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay()){
                                extraHalfDays+=0.5;
                            }else {
                                extraDays++;
                            }
                        } else {
                            if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay()) {
                                halfDays+=0.5;
                            }else{
                                presentDays++;
                            }
                        }
                    }
                } else {
                    EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), attendance.getAttendanceDate(), attendance.getAttendanceDate());
                    if (employeeLeave != null) {
                        if (employeeLeave.getLeaveStatus().equals("Approved")) {
                            leaveDays++;
                        } else {
                            absentDays++;
                        }
                    } else {
                        absentDays++;
                    }
                }
            }

            double totalDaysOfEmployee = presentDays + leaveDays + absentDays + halfDays + extraHalfDays + extraDays;
            double presentDaysSalary = 0.0;
            double absentDaysSalary = 0.0;
            double extraDaysSalary = 0.0;
            double extraHalfDaysSalary = 0.0;
            double halfDaysSalary = 0.0;
            double salaryDrawn = 0.0;
            presentDaysSalary = presentDays * perDaySalary;
            absentDaysSalary = absentDays * perDaySalary;
            extraDaysSalary = extraDays * perDaySalary;
            halfDaysSalary = halfDays * perDaySalary;
            extraHalfDaysSalary = extraHalfDays * perDaySalary;
            salaryDrawn = presentDaysSalary + extraDaysSalary + halfDaysSalary + extraHalfDaysSalary;

            System.out.println("totalHoursInMonth " + totalHoursInMonth);
            System.out.println("presentDaysSalary"+presentDaysSalary);
            System.out.println("extraDaysSalary"+extraDaysSalary);
            System.out.println("salaryDrawn"+salaryDrawn);

            double netSalary = 0;
            double basicPer = 0;
            double basic = 0;
            double specialAllowance = 0;
            double pfPer = 0;
            double pf = 0;
            double esiPer = 0;
            double esi = 0;
            double pfTax = 0;
            double totalDeduction = 0;
            double payableAmount = 0;
            double advance = 0;
            double incentive = 0;
            double netPayableAmount = 0;
            double allowanceAmount = 0;
            double deductionAmount = 0;
            Long lateCount = 0L;
            double daysToBeDeducted = 0.0;
            double hoursToBeDeducted = 0.0;
            double latePunchDeductionAmt = 0.0;

            EmployeePayroll employeePayroll = null;
            employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), yearMonth);
            if (employeePayroll == null) {
                employeePayroll = new EmployeePayroll();
            }

//            System.out.println("netSalaryInDays " + netSalaryInDays);
//            System.out.println("netSalaryInHours " + netSalaryInHours);
//            System.out.println("final_day_salary " + final_day_salary);

//            netSalary = netSalaryInDays + netSalaryInHours;
            netSalary = salaryDrawn;
            System.out.println("netSalary " + netSalary);

//            List<Payhead> payheads = allocationRepository.findByEmployeeIdAndStatus(employeeId, true);
//            for(Payhead allocation : allocations){
//                if(allocation.getIsAllowance()){
//                    Payhead payhead1 = payheadRepository.findByIdAndStatus(allocation.getPayhead().getId(), true);
//                    if (payhead1 != null) {
//                        basicPer = payhead1.getPercentage();
//                        basic = (netSalary * (basicPer / 100.0));
//                    }
//                } else {
//                    Deduction deduction = deductionRepository.findByIdAndStatus(allocation.getDeduction().getId(), true);
//                }
//            }
            List<Payhead> payheads = payheadRepository.findByStatus(true);
            for(Payhead payhead : payheads){
                if(payhead.getPayheadStatus()){                     // if this payhead is maraked as default
                    if(payhead.getPercentageOf() != null){
                        if(payhead.getName().toLowerCase().contains("basic")){
                            basicPer = payhead.getPercentage();
                            basic = ((perDaySalary * totalDays) * (basicPer / 100.0));
                        } else if(payhead.getName().toLowerCase().contains("special")){
                            specialAllowance = (perDaySalary * totalDays) * (payhead.getPercentage() / 100.0);
                        }
                    }
                } else {

                }
            }

            List<Deduction> deductions = deductionRepository.findAllByStatus();
            for(Deduction deduction : deductions){
                if(deduction.getPercentageOf() != null){
                    if(deduction.getName().toLowerCase().contains("pf") && employee.getEmployeeHavePf() != null && employee.getEmployeeHavePf()){
                        pfPer = deduction.getPercentage();
                        pf = (basic * (pfPer / 100.0));
                    } else if(deduction.getName().toLowerCase().contains("esi") && employee.getEmployeeHaveEsi() != null && employee.getEmployeeHaveEsi()){
                        esiPer = deduction.getPercentage();
                        esi = (netSalary * (esiPer / 100.0));
                    } else if(deduction.getName().toLowerCase().contains("pt") && employee.getEmployeeHaveProfTax() != null && employee.getEmployeeHaveProfTax()) {
                        if(employee.getGender().equalsIgnoreCase("male")) {
                            if (netSalary >= 7500 && netSalary < 10000) {
                                pfTax = 175;
                            } else if (netSalary >= 10000) {
                                pfTax = 200;
                                if (month == 3) {
                                    pfTax = 300;
                                }
                            }
                        } else {
                            if (netSalary < 25000) {
                                pfTax = 0;
                            } else if (netSalary > 25000) {
                                pfTax = 200;
                                if (month == 3) {
                                    pfTax = 300;
                                }
                            }
                        }
                    }
                }
            }

//            String basicQuery = "SELECT * FROM `payhead_tbl` WHERE name LIKE '%basic%' AND status=1 ORDER BY id " + "DESC LIMIT 1";
//            Query q = entityManager.createNativeQuery(basicQuery, Payhead.class);
//
//            Payhead payhead = (Payhead) q.getSingleResult();
//            if (payhead != null) {
//                basicPer = payhead.getPercentage();
//                basic = (netSalary * (basicPer / 100.0));
//            }
//            specialAllowance = netSalary - basic;
//            if (employee.getEmployeePf() != null && employee.getEmployeePf() > 0) {
//                pfPer = employee.getEmployeePf();
//                pf = (basic * (pfPer / 100.0));
//            }
//
//            if (employee.getEmployeeEsi() != null && employee.getEmployeeEsi() > 0) {
//                esiPer = employee.getEmployeeEsi();
//                esi = (netSalary * (esiPer / 100.0));
//            }
//
//            if (employee.getEmployeeHaveProfTax() == true) {
//                if (netSalary >= 7500 && netSalary < 10000) {
//                    pfTax = 175;
//                } else if (netSalary >= 10000) {
//                    pfTax = 200;
//                    if (month == 3) {
//                        pfTax = 300;
//                    }
//                }
//            }

//            allowanceAmount = allowanceRepository.getSumOfAllowance();
//            deductionAmount = deductionRepository.getSumOfDeduction();
            lateCount = attendanceRepository.getLateCount(employee.getId(), monthValue);
            Shift shift = shiftRepository.findByIdAndStatus(employee.getShift().getId(), true);
            if(shift != null && shift.getConsiderationCount() != 0 && lateCount >= shift.getConsiderationCount()){
                if(shift.getIsDayDeduction()){
                    employeePayroll.setIsDayDeduction(true);
                    daysToBeDeducted = lateCount / shift.getConsiderationCount();
                    if(shift.getDayValueOfDeduction().equalsIgnoreCase("quarter")) {
                        latePunchDeductionAmt = daysToBeDeducted * (perDaySalary/4);
                        employeePayroll.setDeductionType(shift.getDayValueOfDeduction());
                    } else if (shift.getDayValueOfDeduction().equalsIgnoreCase("half")) {
                        latePunchDeductionAmt = daysToBeDeducted * (perDaySalary/2);
                        employeePayroll.setDeductionType(shift.getDayValueOfDeduction());
                    } else {
                        latePunchDeductionAmt = daysToBeDeducted * perDaySalary;
                        employeePayroll.setDeductionType(shift.getDayValueOfDeduction());
                    }
                } else {
                    hoursToBeDeducted = shift.getHourValueOfDeduction();
                    latePunchDeductionAmt = lateCount * (shift.getHourValueOfDeduction() * perHourSalary);
                    employeePayroll.setDeductionType("hour");
                    employeePayroll.setHoursToBeDeducted(hoursToBeDeducted);
                }
            }
//            allowanceAmount = allocationRepository.getSumOfAllocations(employee.getId(),true, true, year, month);
//            deductionAmount = allocationRepository.getSumOfAllocations(employee.getId(),false, true, year, month);

            if(deductionAmount > 0)
                totalDeduction = (pf + esi + pfTax + latePunchDeductionAmt + deductionAmount);
            else
                totalDeduction = (pf + esi + pfTax + latePunchDeductionAmt);
            if(allowanceAmount >0)
                payableAmount = ((netSalary + allowanceAmount) - totalDeduction);
            else
                payableAmount = (netSalary - totalDeduction);

            double sumAdvance = advancePaymentRepository.getEmployeeAdvanceOfMonth(employee.getId(), year, month);
            advance = sumAdvance;
            netPayableAmount = (payableAmount - advance + incentive);

            employeePayroll.setHalfDays(halfDays);
            employeePayroll.setExtraDays(extraDays);
            employeePayroll.setExtraHalfDays(extraHalfDays);
            employeePayroll.setExtraDaysSalary(extraDaysSalary);
            employeePayroll.setHalfDaysSalary(halfDaysSalary);
            employeePayroll.setExtraHalfDaysSalary(extraHalfDaysSalary);
            employeePayroll.setLateCount(lateCount);
            employeePayroll.setDaysToBeDeducted(daysToBeDeducted);
            employeePayroll.setLatePunchDeductionAmt(latePunchDeductionAmt);
            employeePayroll.setEmployee(employee);
            employeePayroll.setWagesType(wagesType);
            employeePayroll.setYearMonth(yearMonth);
            employeePayroll.setDesignation(employee.getDesignation().getName());
            employeePayroll.setPerDaySalary(perDaySalary);
            employeePayroll.setPerHourSalary(perHourSalary);
            employeePayroll.setNoDaysPresent(presentDays+halfDays+extraHalfDays);
            employeePayroll.setTotalDaysInMonth(totalDaysInMonth);
            employeePayroll.setTotalHoursInMonth(totalHoursInMonth);
            employeePayroll.setNetSalary(netSalary);
            employeePayroll.setNetSalaryInDays(netSalaryInDays);
            employeePayroll.setNetSalaryInHours(netSalaryInHours);
            employeePayroll.setBasicPer(basicPer);
            employeePayroll.setBasic(basic);
            employeePayroll.setSpecialAllowance(specialAllowance);
            employeePayroll.setPfPer(pfPer);
            employeePayroll.setPf(pf);
            employeePayroll.setEsiPer(esiPer);
            employeePayroll.setEsi(esi);
            employeePayroll.setPfTax(pfTax);
            employeePayroll.setAllowanceAmount(allowanceAmount);
            employeePayroll.setDeductionAmount(deductionAmount);
            employeePayroll.setTotalDeduction(totalDeduction);
            employeePayroll.setPayableAmount(payableAmount);
            employeePayroll.setAbsentDaysSalary(absentDaysSalary);
            employeePayroll.setTotalDaysOfEmployee(totalDaysOfEmployee);
            employeePayroll.setAbsentDays(Double.parseDouble(absentDays.toString()));
            employeePayroll.setPresentDays(presentDays);
            employeePayroll.setLeaveDays(Double.parseDouble(leaveDays.toString()));
            employeePayroll.setAdvance(advance);
            employeePayroll.setIncentive(incentive);
            employeePayroll.setNetPayableAmount(netPayableAmount);
            employeePayroll.setUpdatedAt(LocalDateTime.now());
            employeePayroll.setUpdatedBy(users.getId());
            employeePayroll.setInstitute(attendance1.getInstitute());
            employeePayroll.setTotalDays(totalDays);
            employeePayroll.setMonthlyPay(monthlyPay);
            employeePayroll.setDaysInMonth(daysInMonth);
            employeePayroll.setGrosstotal(basic+specialAllowance);
//            employeePayroll.setIsHalfDay(attendance1.getIsHalfDay());

            employeePayrollRepository.save(employeePayroll);

        } catch (Exception e) {
            attendanceLogger.error("updateSalaryForDay Exception ===>" + e);
            System.out.println("updateSalaryForDay Exception ===>" + e.getMessage());
            e.printStackTrace();
        }
    }

//    public void updateSalaryForDayBkp(Attendance attendance1) {
//        try {
//            Long employeeId = attendance1.getEmployee().getId();
//            LocalDate attendanceDate = attendance1.getAttendanceDate();
//            String yearMonth = attendanceDate.getYear() + "-" + attendanceDate.getMonthValue();
//
//            Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
//            String wagesType = employee.getEmployeeWagesType();
//            Double wagesPerDaySalary = utility.getEmployeeWages(employeeId);
//            double perDaySalary = 0;
//            double perHourSalary = 0;
//            if (wagesPerDaySalary != null) {
//                perDaySalary = wagesPerDaySalary;
//                perHourSalary = Precision.round(perDaySalary / 8.0, 2);
//            }
//            double totalDaysInMonth = 1;
//            int noDaysPresent = 1;
//            double actualWorkTime = attendance1.getActualWorkTime() != null ? Precision.round(attendance1.getActualWorkTime(), 2) : 0;
//            double totalMinutesInMonth = actualWorkTime;
//            double totalHoursInMonth = totalMinutesInMonth / 60.0;
//            double netSalary = 0;
//            double netSalaryInDays = 0;
//            double netSalaryInHours = 0;
//            double netSalaryInPoints = 0;
//            double netSalaryInPcs = 0;
//            double basicPer = 0;
//            double basic = 0;
//            double specialAllowance = 0;
//            double pfPer = 0;
//            double pf = 0;
//            double esiPer = 0;
//            double esi = 0;
//            double pfTax = 0;
//            double totalDeduction = 0;
//            double payableAmount = 0;
//            double advance = 0;
//            double incentive = 0;
//            double netPayableAmount = 0;
//            double allowanceAmount = 0;
//            double deductionAmount = 0;
//
//            EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employeeId, yearMonth);
//            /* If data present for that month */
//            if (employeePayroll != null) {
//                noDaysPresent = noDaysPresent + employeePayroll.getNoDaysPresent();
//                totalDaysInMonth = totalDaysInMonth + employeePayroll.getTotalDaysInMonth();
//                totalHoursInMonth = totalHoursInMonth + employeePayroll.getTotalHoursInMonth();
//
//                /*wagesType = employeePayroll.getWagesType();
//                perDaySalary = employeePayroll.getPerDaySalary();
//                perHourSalary = employeePayroll.getPerHourSalary();
//                netSalary = employeePayroll.getNetSalary();*/
//
//                basicPer = employeePayroll.getBasicPer();
//                basic = employeePayroll.getBasic();
//                pfPer = employeePayroll.getPfPer();
//                esiPer = employeePayroll.getEsiPer();
//
//                /*netSalaryInDays = updateNetDaySalary(employeePayroll.getTotalDaysInMonth(), perDaySalary);
//                netSalaryInHours = updateNetHourSalary(totalHoursInMonth, employeePayroll.getTotalHoursInMonth(), perHourSalary);
//                netSalaryInPoints = updateNetPointSalary(employeePayroll.getNetSalaryInPoints(), attendance1.getWagesPointBasis());
//                netSalaryInPcs = updateNetPcsSalary(employeePayroll.getNetSalaryInPcs(), attendance1.getWagesPcsBasis());*/
//
//                /*if (wagesType.equalsIgnoreCase("day")) {
//                    totalDaysInMonth = employeePayroll.getTotalDaysInMonth() + 1;
//                    noDaysPresent = employeePayroll.getNoDaysPresent() + 1;
//                    netSalary = totalDaysInMonth * perDaySalary;
//                } else if (wagesType.equalsIgnoreCase("hr")) {
//                    totalHoursInMonth = totalHoursInMonth + employeePayroll.getTotalHoursInMonth();
//                    totalDaysInMonth = (totalHoursInMonth / attendanceDate.getDayOfMonth());
//                    noDaysPresent = employeePayroll.getNoDaysPresent() + 1;
//
//                    netSalary = totalHoursInMonth * perHourSalary;
//                } else if (wagesType.equalsIgnoreCase("point")) {
//                    totalDaysInMonth = employeePayroll.getTotalDaysInMonth() + 1;
//                    noDaysPresent = employeePayroll.getNoDaysPresent() + 1;
//                    netSalary = netSalaryInPoints;
//                } else if (wagesType.equalsIgnoreCase("pcs")) {
//                    totalDaysInMonth = employeePayroll.getTotalDaysInMonth() + 1;
//                    noDaysPresent = employeePayroll.getNoDaysPresent() + 1;
//                    netSalary = netSalaryInPcs;
//                }*/
//
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("pcs")) {
//                    netSalaryInPcs = employeePayroll.getNetSalaryInPcs() + attendance1.getFinalDaySalary();
//                    employeePayroll.setNetSalaryInPcs(netSalaryInPcs);
//                }
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("point")) {
//                    netSalaryInPoints = employeePayroll.getNetSalaryInPoints() + attendance1.getFinalDaySalary();
//                    employeePayroll.setNetSalaryInPoints(netSalaryInPoints);
//                }
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("day")) {
//                    netSalaryInDays = employeePayroll.getNetSalaryInDays() + attendance1.getFinalDaySalary();
//                    employeePayroll.setNetSalaryInDays(netSalaryInDays);
//                }
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("hr")) {
//                    netSalaryInHours = employeePayroll.getNetSalaryInHours() + attendance1.getFinalDaySalary();
//                    employeePayroll.setNetSalaryInHours(netSalaryInHours);
//                }
//
////                netSalary = netSalary + attendance1.getFinalDaySalary(); // old code
//                netSalary = employeePayroll.getNetSalaryInDays() + employeePayroll.getNetSalaryInPcs() + employeePayroll.getNetSalaryInPoints() + employeePayroll.getNetSalaryInHours();
//
//                String basicQuery = "SELECT * FROM `payhead_tbl` WHERE name LIKE '%basic%' AND status=1 ORDER BY id " + "DESC LIMIT 1";
//                Query q = entityManager.createNativeQuery(basicQuery, Payhead.class);
//                Payhead payhead = (Payhead) q.getSingleResult();
//                if (payhead != null) {
//                    basic = (netSalary * (basicPer / 100.0));
//                }
//                specialAllowance = (netSalary - basic);
//                if (employee.getEmployeeHavePf()) {
//                    pf = (basic * (pfPer / 100.0));
//                }
//                if (employee.getEmployeeHaveEsi()) {
//                    esi = (netSalary * (esiPer / 100.0));
//                }
//                if (employee.getEmployeeHaveProfTax()) {
//                    if (netSalary >= 7500 && netSalary < 10000) {
//                        pfTax = 175;
//                    } else if (netSalary >= 10000) {
//                        pfTax = 200;
//                        if (attendance1.getAttendanceDate().getMonthValue() == 3) {
//                            pfTax = 300;
//                        }
//                    }
//                }
//
//                allowanceAmount = allowanceRepository.getSumOfAllowance();
//                deductionAmount = deductionRepository.getSumOfDeduction();
//
//                totalDeduction = (pf + esi + pfTax + deductionAmount);
//                payableAmount = (netSalary + allowanceAmount - totalDeduction);
//
//                double sumAdvance = advancePaymentRepository.getEmployeeAdvanceOfMonth(employeeId, attendanceDate.getYear(), attendanceDate.getMonthValue());
//                advance = sumAdvance;
//                netPayableAmount = (payableAmount - advance + incentive);
//
//                employeePayroll.setNoDaysPresent(noDaysPresent);
//                employeePayroll.setTotalDaysInMonth(totalDaysInMonth);
//                employeePayroll.setTotalHoursInMonth(totalHoursInMonth);
//                employeePayroll.setNetSalary(netSalary);
//                employeePayroll.setNetSalaryInDays(netSalaryInDays);
//                employeePayroll.setNetSalaryInHours(netSalaryInHours);
//                employeePayroll.setNetSalaryInPoints(netSalaryInPoints);
//                employeePayroll.setNetSalaryInPcs(netSalaryInPcs);
//                employeePayroll.setBasic(basic);
//                employeePayroll.setSpecialAllowance(specialAllowance);
//                employeePayroll.setPf(pf);
//                employeePayroll.setEsi(esi);
//                employeePayroll.setPfTax(pfTax);
//                employeePayroll.setAllowanceAmount(allowanceAmount);
//                employeePayroll.setDeductionAmount(deductionAmount);
//                employeePayroll.setTotalDeduction(totalDeduction);
//                employeePayroll.setPayableAmount(payableAmount);
//                employeePayroll.setAdvance(advance);
//                employeePayroll.setIncentive(incentive);
//                employeePayroll.setNetPayableAmount(netPayableAmount);
//                employeePayroll.setUpdatedAt(LocalDateTime.now());
//                employeePayroll.setUpdatedBy(employeeId);
//
//                employeePayrollRepository.save(employeePayroll);
//
//            } else {
//
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("pcs"))
//                    netSalaryInPcs = attendance1.getFinalDaySalary();
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("point"))
//                    netSalaryInPoints = attendance1.getFinalDaySalary();
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("day"))
//                    netSalaryInDays = attendance1.getFinalDaySalary();
//                if (attendance1.getFinalDaySalaryType().equalsIgnoreCase("hr"))
//                    netSalaryInHours = attendance1.getFinalDaySalary();
//
//                /*netSalaryInDays = totalDaysInMonth * perDaySalary;
//                netSalaryInHours = totalHoursInMonth * perHourSalary;
//                netSalaryInPoints = attendance1.getWagesPointBasis() != null ? attendance1.getWagesPointBasis() : 0;
//                netSalaryInPcs = attendance1.getWagesPcsBasis() != null ? attendance1.getWagesPcsBasis() : 0;*/
//
//                System.out.println("netSalaryInDays " + netSalaryInDays);
//                System.out.println("netSalaryInHours " + netSalaryInHours);
//                System.out.println("netSalaryInPoints " + netSalaryInPoints);
//                System.out.println("netSalaryInPcs " + netSalaryInPcs);
//
//                /*if (wagesType.equalsIgnoreCase("day")) {
//                    netSalary = totalDaysInMonth * perDaySalary;
//                } else if (wagesType.equalsIgnoreCase("hr")) {
//                    netSalary = totalHoursInMonth * perHourSalary;
//                } else if (wagesType.equalsIgnoreCase("point")) {
//                    netSalary = netSalaryInPoints;
//                } else if (wagesType.equalsIgnoreCase("pcs")) {
//                    netSalary = netSalaryInPcs;
//                }*/
//
//                netSalary = attendance1.getFinalDaySalary();
//
//                String basicQuery = "SELECT * FROM `payhead_tbl` WHERE name LIKE '%basic%' AND status=1 ORDER BY id " + "DESC LIMIT 1";
//                Query q = entityManager.createNativeQuery(basicQuery, Payhead.class);
//                Payhead payhead = (Payhead) q.getSingleResult();
//                if (payhead != null) {
//                    basicPer = payhead.getPercentage();
//                    basic = (netSalary * (basicPer / 100.0));
//                }
//                specialAllowance = netSalary - basic;
//                if (employee.getEmployeeHavePf()) {
//                    /*String pfQuery = "SELECT * FROM `master_payhead_tbl` WHERE name LIKE '%pf%' AND status=1 ORDER BY" +
//                            " id DESC LIMIT 1";
//                    Query pfQ = entityManager.createNativeQuery(pfQuery, MasterPayhead.class);
//                    MasterPayhead masterPayhead = (MasterPayhead) pfQ.getSingleResult();*/
//                    if (employee.getEmployeePf() != null && employee.getEmployeePf() > 0) {
//                        pfPer = employee.getEmployeePf();
//                        pf = (basic * (pfPer / 100.0));
//                    }
//                }
//                if (employee.getEmployeeHaveEsi()) {
//                    /*String esiQuery = "SELECT * FROM `master_payhead_tbl` WHERE name LIKE '%esi%' AND status=1 " +
//                            "ORDER BY id DESC LIMIT 1";
//                    Query esiQ = entityManager.createNativeQuery(esiQuery, MasterPayhead.class);
//                    MasterPayhead masterPayhead = (MasterPayhead) esiQ.getSingleResult();*/
//
//                    if (employee.getEmployeeEsi() != null && employee.getEmployeeEsi() > 0) {
//                        esiPer = employee.getEmployeeEsi();
//                        esi = (netSalary * (esiPer / 100.0));
//                    }
//                }
//                if (employee.getEmployeeHaveProfTax()) {
//                    if (netSalary >= 7500 && netSalary < 10000) {
//                        pfTax = 175;
//                    } else if (netSalary >= 10000) {
//                        pfTax = 200;
//                        if (attendance1.getAttendanceDate().getMonthValue() == 3) {
//                            pfTax = 300;
//                        }
//                    }
//                }
//
//                allowanceAmount = allowanceRepository.getSumOfAllowance();
//                deductionAmount = deductionRepository.getSumOfDeduction();
//
//                totalDeduction = (pf + esi + pfTax + deductionAmount);
//                payableAmount = (netSalary + allowanceAmount - totalDeduction);
//
//                double sumAdvance = advancePaymentRepository.getEmployeeAdvanceOfMonth(employeeId, attendanceDate.getYear(), attendanceDate.getMonthValue());
//                advance = sumAdvance;
//                netPayableAmount = (payableAmount - advance + incentive);
//
//                EmployeePayroll newEmployeePayroll = new EmployeePayroll();
//                newEmployeePayroll.setEmployee(employee);
//                newEmployeePayroll.setWagesType(wagesType);
//                newEmployeePayroll.setYearMonth(yearMonth);
//                newEmployeePayroll.setDesignation(employee.getDesignation().getName());
//                newEmployeePayroll.setPerDaySalary(perDaySalary);
//                newEmployeePayroll.setPerHourSalary(perHourSalary);
//                newEmployeePayroll.setNoDaysPresent(noDaysPresent);
//                newEmployeePayroll.setTotalDaysInMonth(totalDaysInMonth);
//                newEmployeePayroll.setTotalHoursInMonth(totalHoursInMonth);
//                newEmployeePayroll.setNetSalary(netSalary);
//                newEmployeePayroll.setNetSalaryInDays(netSalaryInDays);
//                newEmployeePayroll.setNetSalaryInHours(netSalaryInHours);
//                newEmployeePayroll.setNetSalaryInPoints(netSalaryInPoints);
//                newEmployeePayroll.setNetSalaryInPcs(netSalaryInPcs);
//                newEmployeePayroll.setBasicPer(basicPer);
//                newEmployeePayroll.setBasic(basic);
//                newEmployeePayroll.setSpecialAllowance(specialAllowance);
//                newEmployeePayroll.setPfPer(pfPer);
//                newEmployeePayroll.setPf(pf);
//                newEmployeePayroll.setEsiPer(esiPer);
//                newEmployeePayroll.setEsi(esi);
//                newEmployeePayroll.setPfTax(pfTax);
//                newEmployeePayroll.setAllowanceAmount(allowanceAmount);
//                newEmployeePayroll.setDeductionAmount(deductionAmount);
//                newEmployeePayroll.setTotalDeduction(totalDeduction);
//                newEmployeePayroll.setPayableAmount(payableAmount);
//                newEmployeePayroll.setAdvance(advance);
//                newEmployeePayroll.setIncentive(incentive);
//                newEmployeePayroll.setNetPayableAmount(netPayableAmount);
//                newEmployeePayroll.setCreatedAt(LocalDateTime.now());
//                newEmployeePayroll.setCreatedBy(employeeId);
//
//                employeePayrollRepository.save(newEmployeePayroll);
//            }
//        } catch (Exception e) {
//            attendanceLogger.error("updateSalaryForDay Exception ===>" + e);
//            System.out.println("updateSalaryForDay Exception ===>" + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    public JsonObject checkAttendanceStatus(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            LocalDate localDate = LocalDate.now();
            System.out.println("localDate " + localDate);
            JsonObject jsonObject = new JsonObject();
            Long attendanceId = Long.valueOf(0);
            Boolean todayAttendance = false;
            Boolean checkInStatus = false;
            Boolean checkOutStatus = false;
            LocalDateTime checkInTime = null;
            LocalDateTime checkOutTime = null;
            LocalTime currentTime = null;
//            String totalTime = null;
            LocalTime totalTime = null;
            Long currentBreakId = null;
            LocalTime fromTime = employee.getShift().getFromTime();
            LocalTime toTime = employee.getShift().getToTime();
            LocalTime totalHours = employee.getShift().getWorkingHours();

            Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), LocalDate.now(), true);
            if (attendance != null) {
                todayAttendance = true;
                attendanceId = attendance.getId();
                if (attendance.getCheckInTime() != null) {
                    checkInStatus = true;
                    checkInTime = attendance.getCheckInTime();
                }
                if (attendance.getCheckOutTime() != null) {
                    checkOutStatus = true;
                    checkOutTime = attendance.getCheckOutTime();
                    totalTime = utility.getDateTimeDiffInTime(checkInTime, checkOutTime);
                }
                currentBreakId = taskMasterRepository.getCurrentBreakId(attendanceId);
            } else {
                Attendance oldAttendance = attendanceRepository.findLastRecordOfEmployeeWithoutCheckOut(employee.getId(), true);
                if (oldAttendance != null) {
                    attendanceId = oldAttendance.getId();
                    if (oldAttendance.getCheckInTime() != null) {
                        checkInStatus = true;
                        checkInTime = oldAttendance.getCheckInTime();
                    }
                    if (oldAttendance.getCheckOutTime() != null) {
                        checkOutStatus = true;
                        checkOutTime = oldAttendance.getCheckOutTime();
                    }
                    currentBreakId = taskMasterRepository.getCurrentBreakId(attendanceId);
                }
                todayAttendance = false;
            }

            Attendance oldSecondAttendance = attendanceRepository.findLastSecondRecordOfEmployee(employee.getId(), localDate);
            if (oldSecondAttendance != null) {
                jsonObject.addProperty("oldAttendanceDate", String.valueOf(oldSecondAttendance.getAttendanceDate()));
                jsonObject.addProperty("oldCheckInTime", String.valueOf(oldSecondAttendance.getCheckInTime()));
                jsonObject.addProperty("oldCheckOutTime", String.valueOf(oldSecondAttendance.getCheckOutTime()));
            } else {
                jsonObject.addProperty("oldAttendanceDate", "null");
                jsonObject.addProperty("oldCheckInTime", "null");
                jsonObject.addProperty("oldCheckOutTime", "null");
            }
            jsonObject.addProperty("currentTime", String.valueOf(LocalTime.now()));
            jsonObject.addProperty("currentDate", String.valueOf(LocalDate.now()));
            jsonObject.addProperty("shiftFromTime", String.valueOf(fromTime));
            jsonObject.addProperty("shiftToTime", String.valueOf(toTime));
            jsonObject.addProperty("shiftTotalHours", String.valueOf(totalHours));
            jsonObject.addProperty("todayAttendance", todayAttendance);
            jsonObject.addProperty("attendanceId", attendanceId);
            jsonObject.addProperty("checkInStatus", checkInStatus);
            jsonObject.addProperty("checkInTime", String.valueOf((checkInTime)));
            jsonObject.addProperty("checkOutStatus", checkOutStatus);
            jsonObject.addProperty("checkOutTime", String.valueOf(checkOutTime));
            jsonObject.addProperty("totalTime", totalTime != null ? totalTime.toString() : "");
            jsonObject.addProperty("currentBreakId", currentBreakId != null ? currentBreakId : 0);

            System.out.println("jsonObject " + jsonObject);
            responseMessage.add("response", jsonObject);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Data inconsistency, please validate data");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public void findDifference(String start_date, String end_date) {

        // SimpleDateFormat converts the
        // string format to date object
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        // Try Block
        try {

            // parse method is used to parse
            // the text from a string to
            // produce the date
            Date d1 = sdf.parse(start_date);
            Date d2 = sdf.parse(end_date);

            // Calucalte time difference
            // in milliseconds
            long difference_In_Time = d2.getTime() - d1.getTime();

            // Calucalte time difference in
            // seconds, minutes, hours, years,
            // and days
            long difference_In_Seconds = (difference_In_Time / 1000) % 60;

            long difference_In_Minutes = (difference_In_Time / (1000 * 60)) % 60;

            long difference_In_Hours = (difference_In_Time / (1000 * 60 * 60)) % 24;

            long difference_In_Years = (difference_In_Time / (1000L * 60 * 60 * 24 * 365));

            long difference_In_Days = (difference_In_Time / (1000 * 60 * 60 * 24)) % 365;

            // Print the date difference in
            // years, in days, in hours, in
            // minutes, and in seconds

            System.out.print("Difference " + "between two dates is: ");

            System.out.println(difference_In_Years + " years, " + difference_In_Days + " days, " + difference_In_Hours + " hours, " + difference_In_Minutes + " minutes, " + difference_In_Seconds + " seconds");
        }

        // Catch the Exception
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public Object DTAbsent(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String attendanceDate = request.get("attendanceDate");
        String selectedShift = request.get("selectedShift");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Employee> employeeList = new ArrayList<>();
        List<EmployeeDTDTO> employeeDTDTOList = new ArrayList<>();
        try {

            if (attendanceDate.equalsIgnoreCase("")) {
                attendanceDate = LocalDate.now().toString();
            }
            String query = "SELECT * FROM employee_tbl WHERE status=1 AND institute_id="+user.getInstitute().getId()+" AND id NOT IN(SELECT employee_id FROM attendance_tbl WHERE attendance_date='" + attendanceDate + "')  ";

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (first_name LIKE '%" + searchText + "%' OR last_name LIKE '%" + searchText + "%' OR middle_name LIKE '%" + searchText + "%'" +
                        " OR mobile_number LIKE '%" + searchText + "%') ";
            }

            if (!selectedShift.equalsIgnoreCase("")) {
                query = query + " AND employee_tbl.shift_id ='" + selectedShift + "' ";
            }
            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") && jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY last_name DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, Employee.class);
            Query q1 = entityManager.createNativeQuery(query1, Employee.class);

            employeeList = q.getResultList();
            System.out.println("Limit total rows " + employeeList.size());

            for (Employee employee : employeeList) {
                employeeDTDTOList.add(convertToDTDTO1(employee, false));
            }

            List<Employee> employeeList1 = new ArrayList<>();
            employeeList1 = q1.getResultList();
            System.out.println("total rows " + employeeList1.size());

            genericDTData.setRows(employeeDTDTOList);
            genericDTData.setTotalRows(employeeList1.size());


        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            genericDTData.setRows(employeeDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;

    }

    private EmployeeDTDTO convertToDTDTO1(Employee employee, boolean b) {
        EmployeeDTDTO employeeDTDTO = new EmployeeDTDTO();
        employeeDTDTO.setEmployeeId(employee.getId());
        employeeDTDTO.setFullName(employee.getFirstName() + " " + employee.getLastName());
        employeeDTDTO.setDob(String.valueOf(employee.getDob()));
        employeeDTDTO.setGender(employee.getGender());
        employeeDTDTO.setMobileNumber(employee.getMobileNumber());
        employeeDTDTO.setEmployeeType(employee.getEmployeeType());
        employeeDTDTO.setCreatedAt(String.valueOf(employee.getCreatedAt()));
        employeeDTDTO.setStatus(employee.getStatus());
//        employeeDTDTO.setWagesPerDay(employee.getWagesPerDay());
        employeeDTDTO.setDesigName(employee.getDesignation().getName());
        employeeDTDTO.setShiftName(employee.getShift() != null ? employee.getShift().getName() : "");
        employeeDTDTO.setCompanyName(employee.getCompany() != null ? employee.getCompany().getCompanyName() : "");
        return employeeDTDTO;
    }

    public Object DTAttendance(Map<String, String> request) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String attendanceDate = request.get("attendanceDate");
//        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<AttendanceView> attendanceViewList = new ArrayList<>();
        List<AttendanceDTDTO> attendanceDTDTOList = new ArrayList<>();
        try {

            if (attendanceDate.equalsIgnoreCase("")) {
                attendanceDate = LocalDate.now().toString();
            }
            String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND Date(attendance_date)='" + attendanceDate + "'";

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (full_name LIKE '%" + searchText + "%' OR attendance_date LIKE '%" + searchText + "%' OR check_in_time LIKE '%" + searchText + "%' OR check_out_time LIKE '%" + searchText + "%'" + " OR total_time LIKE '%" + searchText + "%') ";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") && jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, AttendanceView.class);
            Query q1 = entityManager.createNativeQuery(query1, AttendanceView.class);

            attendanceViewList = q.getResultList();
            System.out.println("Limit total rows " + attendanceViewList.size());

            for (AttendanceView attendanceView : attendanceViewList) {
                attendanceDTDTOList.add(convertToDTDTO(attendanceView, false));
            }

            List<AttendanceView> attendanceViewArrayList = new ArrayList<>();
            attendanceViewArrayList = q1.getResultList();
            System.out.println("total rows " + attendanceViewArrayList.size());

            genericDTData.setRows(attendanceDTDTOList);
            genericDTData.setTotalRows(attendanceViewArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(attendanceDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private AttendanceDTDTO convertToDTDTO(AttendanceView attendanceView, Boolean status) {
        AttendanceDTDTO attendanceDTDTO = new AttendanceDTDTO();
        attendanceDTDTO.setId(attendanceView.getId());
        attendanceDTDTO.setEmployeeId(attendanceView.getEmployeeId());
        attendanceDTDTO.setFirstName(attendanceView.getFirstName());
        attendanceDTDTO.setMiddleName(attendanceView.getMiddleName());
        attendanceDTDTO.setLastName(attendanceView.getLastName());
        attendanceDTDTO.setFullName(attendanceView.getFullName());
        attendanceDTDTO.setAttendanceDate(String.valueOf(attendanceView.getAttendanceDate()));
        attendanceDTDTO.setCheckInTime(String.valueOf(attendanceView.getCheckInTime()));
        if (attendanceView.getCheckOutTime() != null)
            attendanceDTDTO.setCheckOutTime(String.valueOf(attendanceView.getCheckOutTime()));
        if (attendanceView.getTotalTime() != null) attendanceDTDTO.setTotalTime(attendanceView.getTotalTime());
        attendanceDTDTO.setStatus(attendanceView.getStatus());
        attendanceDTDTO.setRemark(attendanceView.getRemark());
        attendanceDTDTO.setAdminRemark(attendanceView.getAdminRemark());
        attendanceDTDTO.setFinalDaySalaryType(attendanceView.getFinalDaySalaryType());
        attendanceDTDTO.setFinalDaySalary(attendanceView.getFinalDaySalary());
        attendanceDTDTO.setTotalWorkTime(Precision.round(attendanceView.getTotalWorkTime(), 2));
        attendanceDTDTO.setActualWorkTime(Precision.round(attendanceView.getActualWorkTime(), 2));
        attendanceDTDTO.setWagesHourBasis(attendanceView.getWagesHourBasis() != null ? Precision.round(attendanceView.getWagesHourBasis(), 2) : 0);
        attendanceDTDTO.setWagesPerDay(Precision.round(attendanceView.getWagesPerDay(), 2));

        if (status) {
            List<TaskView> taskViewList = taskViewRepository.findByEmployeeIdAndTaskDateOrderById(attendanceView.getEmployeeId(), attendanceView.getAttendanceDate());

            List<TaskDTO> taskDTOList = new ArrayList<>();
            if (taskViewList.size() > 0) {
                for (TaskView taskView : taskViewList) {
                    taskDTOList.add(taskService.convertToDTO(taskView));
                }
            }
            attendanceDTDTO.setTaskDTOList(taskDTOList);
        }
        return attendanceDTDTO;
    }

    public Object submitEmployeeTodayWages(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long attendanceId = Long.valueOf(jsonRequest.get("attendanceId"));
            Attendance attendance = attendanceRepository.findByIdAndStatus(attendanceId, true);

            Boolean finalDaySalaryType = Boolean.valueOf(jsonRequest.get("finalDaySalaryType"));
            Double finalDaySalary = Double.valueOf(jsonRequest.get("finalDaySalary"));
            attendance.setFinalDaySalaryType(String.valueOf(finalDaySalaryType));
            attendance.setFinalDaySalary(finalDaySalary);
            attendance.setAttendanceStatus(jsonRequest.get("attendanceStatus"));
            attendance.setUpdatedBy(users.getId());
            attendance.setUpdatedAt(LocalDateTime.now());
            attendance.setInstitute(users.getInstitute());
            attendanceRepository.save(attendance);
            responseMessage.setMessage("Data updated successfully");
            responseMessage.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to submit data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public JsonObject getPaymentUptoDate(HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            LocalDate currentDate = LocalDate.now();
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            System.out.println("First day: " + currentDate.withDayOfMonth(1));
            System.out.println("Last day: " + currentDate.withDayOfMonth(currentDate.lengthOfMonth()));

            Double totalPendingAmount = attendanceRepository.getTotalPendingPaymentUpto(employee.getId(), firstDateOfMonth, lastDateOfMonth, "Pending");
            System.out.println("totalPendingAmount: " + totalPendingAmount);

            Double totalApproveAmount = attendanceRepository.getTotalApprovePaymentUpto(employee.getId(), firstDateOfMonth, lastDateOfMonth, "Approved");
            System.out.println("totalApproveAmount: " + totalApproveAmount);

            Double totalPaymentUpto = attendanceRepository.getTotalPaymentUpto(employee.getId(), firstDateOfMonth, currentDate);
            System.out.println("totalPaymentUpto: " + totalPaymentUpto);

            Double totalAdvancePaymentUpto = totalPendingAmount + totalApproveAmount;
            System.out.println("totalAdvancePaymentUpto: " + totalAdvancePaymentUpto);

            Double remRequestAmount = totalPaymentUpto - totalAdvancePaymentUpto;
            System.out.println("remRequestAmount: " + remRequestAmount);

            responseMessage.addProperty("response", remRequestAmount);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("Failed to load data " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public JsonObject getEmpMonthlyPresenty(Map<String, String> jsonRequest, HttpServletRequest request) {
        System.out.println("jsonRequest " + jsonRequest);
        JsonObject response = new JsonObject();
        JsonObject res = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

        JsonArray pEmpArr = new JsonArray();
        JsonArray hEmpArr = new JsonArray();
        JsonArray lEmpArr = new JsonArray();
        JsonArray abEmpArr = new JsonArray();
        JsonArray totalabandl = new JsonArray();
        JsonArray abPer = new JsonArray();
        try {
            int totalDays = 0;
            int sumofAllEmployeeTotalDays = 0;
            int sumOfAllEmployeePresenty = 0;
            int sumOfAllEmployeeAbsenty = 0;
            int sumOfAllEmployeeLeaves = 0;
            int sumOfAllEmployeeHalfDays = 0;


            int totalEmployees = 0;
            int presentEmployees = 0;
            int leaveEmployees = 0;
            int halfDayEmployees = 0;
            int absentEmployees = 0;
            int totalAbsentAndLeave = 0;

            System.out.println("jsonRequest " + jsonRequest.get("currentMonth"));
            String[] currentMonth = jsonRequest.get("currentMonth").split("-");
            String userMonth = currentMonth[1];
            String userYear = currentMonth[0];
            String userDay = "01";

            String newUserDate = userYear + "-" + userMonth + "-" + userDay;
            System.out.println("newUserDate" + newUserDate);
            LocalDate currentDate = LocalDate.parse(newUserDate);
            totalDays = getTotalDaysFromYearAndMonth(Integer.parseInt(userYear), Integer.parseInt(userMonth));
            System.out.println("totalDays" + totalDays);

            System.out.println("currentDate" + currentDate);
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            System.out.println("firstDateOfMonth" + firstDateOfMonth);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).plusDays(1);
            System.out.println("lastDateOfMonth " + lastDateOfMonth);

            List<LocalDate> localDates = firstDateOfMonth.datesUntil(lastDateOfMonth).collect(Collectors.toList());
            System.out.println("dates size" + localDates.size());

            List<LocalDate> localDates1 = firstDateOfMonth.datesUntil(lastDateOfMonth).collect(Collectors.toList());
            System.out.println("dates size" + localDates1.size());

            JsonObject jsonObject = new JsonObject();
//
            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {
                List<Employee> employees = employeeRepository.findByInstituteIdAndStatusOrderByFirstNameAsc(users.getInstitute().getId(), true);

                Double absentPer = 0.0;
                if (employees.size() > 0) {
                    for (LocalDate localDate : localDates1) {
                        totalEmployees = employeeRepository.getEmployeeCount(true, users.getInstitute().getId());
                        presentEmployees = attendanceRepository.getPresentEmployeeCount(localDate, users.getInstitute().getId());
                        leaveEmployees = attendanceRepository.getLeaveEmployeeCount(localDate, users.getInstitute().getId());
                        halfDayEmployees = attendanceRepository.getHalfDayEmployeeCount(localDate, users.getInstitute().getId());
                        absentEmployees = totalEmployees - (presentEmployees + leaveEmployees);
                        totalAbsentAndLeave = leaveEmployees + absentEmployees;

                        if (presentEmployees > 0) {
                            absentPer = Precision.round(Double.valueOf(totalAbsentAndLeave) / Double.valueOf(presentEmployees) * 100, 0);
                            System.out.println(" absentPer " + absentPer);
                        }
                        pEmpArr.add(presentEmployees);
                        lEmpArr.add(leaveEmployees);
                        abEmpArr.add(absentEmployees);
                        totalabandl.add(totalAbsentAndLeave);
                        abPer.add(absentPer);
                        hEmpArr.add(halfDayEmployees);
                    }
                }
                for (Employee employee1 : employees) {
                    Integer pDays = 0;
                    Integer lDays = 0;
                    Integer hDays = 0;
                    Integer aDays = 0;
                    Integer woDays = 0;
                    JsonObject empObj = new JsonObject();
                    JsonArray empArray = new JsonArray();
                    JsonObject empObjCount = new JsonObject();

                    empObj.addProperty("id", employee1.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee1));

                    int i = 0;
                    for (LocalDate localDate : localDates) {
                        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee1.getId(), localDate, true);
                        if (attendance != null) {
                            //if (localDate.getDayOfWeek().toString().contains(employee1.getWeeklyOffDay()))
                            if (attendance.getAttendanceDate().getDayOfWeek().toString().contains(employee1.getWeeklyOffDay())) {
                                if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay()) {
                                    jsonObject.addProperty("attendanceStatus" + i, "EH");
                                    empObj.addProperty("attendanceStatus" + i, "EH");
                                    hDays++;
                                } else {
                                    jsonObject.addProperty("attendanceStatus" + i, "EP");
                                    empObj.addProperty("attendanceStatus" + i, "EP");
                                    pDays++;
                                }
                            } else {
                                if(attendance.getIsHalfDay() != null){
                                    jsonObject.addProperty("attendanceStatus" + i, "H");
                                    empObj.addProperty("attendanceStatus" + i, "H");
                                    hDays++;
                                } else {
                                    jsonObject.addProperty("attendanceStatus" + i, "P");
                                    empObj.addProperty("attendanceStatus" + i, "P");
                                    pDays++;
                                }
                            }
                        } else {
//                            System.out.println("localDate.getDayOfWeek().toString() "+localDate.getDayOfWeek().toString());
                            EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndLeaveStatus(employee1.getId(), localDate, localDate, "Approved");
                            if (employeeLeave != null) {
                                /*if (employeeLeave.getLeaveStatus().equals("Approved"))
                                {*/
                                empObj.addProperty("attendanceStatus" + i, "L");
                                lDays++;
                                /*} else {
                                    empObj.addProperty("attendanceStatus" + i, "A");
                                    aDays++;
                                }*/
                            } else if (localDate.getDayOfWeek().toString().contains(employee1.getWeeklyOffDay())) {
//                            } else if (localDate.getDayOfWeek().toString().equalsIgnoreCase("WEDNESDAY")) {
                                empObj.addProperty("attendanceStatus" + i, "W/O");
                                woDays++;
                            } else {
                                empObj.addProperty("attendanceStatus" + i, "A");
                                aDays++;
                            }
                        }
                        i++;

                    }
                    Integer totalDaysOfEmployee = pDays + lDays + aDays + hDays;
                    sumofAllEmployeeTotalDays = sumofAllEmployeeTotalDays + totalDaysOfEmployee;
                    sumOfAllEmployeePresenty = sumOfAllEmployeePresenty + pDays;
                    sumOfAllEmployeeAbsenty = sumOfAllEmployeeAbsenty + aDays;
                    sumOfAllEmployeeLeaves = sumOfAllEmployeeLeaves + lDays;
                    sumOfAllEmployeeHalfDays = sumOfAllEmployeeHalfDays + hDays;
//                    sumOfFirstDay=sumOfFirstDay+pDays;
                    empObj.addProperty("id", employee1.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee1));
                    empObj.addProperty("pDays", pDays);
                    empObj.addProperty("lDays", lDays);
                    empObj.addProperty("hDays", hDays);
                    empObj.addProperty("aDays", aDays);
                    empObj.addProperty("totalDays", totalDays);
                    empObj.addProperty("woDays", woDays);
                    empObj.addProperty("totalDaysOfEmployee", totalDaysOfEmployee);

                    // jsonArray.add(jsonObject);
                    jsonArray.add(empObj);
                }

            } else {

                Long employeeId = Long.valueOf(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndInstituteIdAndStatus(employeeId, users.getInstitute().getId(), true);

                Integer pDays = 0;
                Integer lDays = 0;
                Integer hDays = 0;
                Integer aDays = 0;
                Integer woDays = 0;

                //jsonObject.addProperty("totalDays", totalDays);
                if (employee != null) {
                    int i = 0;
                    for (LocalDate localDate : localDates) {
                        System.out.println(" localDate " + localDate);
                        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), localDate, true);

                        if (attendance != null) {
                            System.out.println("attendanceStatus =>" + i);
                            if (attendance.getAttendanceDate().getDayOfWeek().toString().contains(employee.getWeeklyOffDay())) {
                                if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay()) {
                                    jsonObject.addProperty("attendanceStatus" + i, "EH");
                                    hDays++;
                                } else {
                                    jsonObject.addProperty("attendanceStatus" + i, "EP");
                                    pDays++;
                                }
                            } else {
                                if(attendance.getIsHalfDay() != null){
                                    jsonObject.addProperty("attendanceStatus" + i, "H");
                                    hDays++;
                                } else {
                                    jsonObject.addProperty("attendanceStatus" + i, "P");
                                    pDays++;
                                }
                            }
                        } else {
                            EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), localDate, localDate);
                            if (employeeLeave != null) {
                                if (employeeLeave.getLeaveStatus().equals("Approved")) {
                                    jsonObject.addProperty("attendanceStatus" + i, "L");
                                    lDays++;
                                } else {
                                    jsonObject.addProperty("attendanceStatus" + i, "A");
                                    aDays++;
                                }
                            } else if (localDate.getDayOfWeek().toString().contains(employee.getWeeklyOffDay())) {
//                            } else if (localDate.getDayOfWeek().toString().equalsIgnoreCase("WEDNESDAY")) {
                                jsonObject.addProperty("attendanceStatus" + i, "W/O");
                                woDays++;
                            } else {
                                jsonObject.addProperty("attendanceStatus" + i, "A");
                                aDays++;
                            }
                        }
                        i++;
                    }
                    Integer totalDaysOfEmployee = pDays + lDays + aDays + hDays;
                    jsonObject.addProperty("employeeName", utility.getEmployeeName(employee));
                    jsonObject.addProperty("designation", employee.getDesignation().getName());
                    jsonObject.addProperty("pDays", pDays);
                    jsonObject.addProperty("lDays", lDays);
                    jsonObject.addProperty("aDays", aDays);
                    jsonObject.addProperty("hDays", hDays);
                    jsonObject.addProperty("totalDays", totalDays);
                    jsonObject.addProperty("woDays", woDays);
                    jsonObject.addProperty("totalDaysOfEmployee", totalDaysOfEmployee);
                    jsonArray.add(jsonObject);
                }
            }


            res.add("list", jsonArray);
            res.add("pList", pEmpArr);
            res.add("lList", lEmpArr);
            res.add("abList", abEmpArr);
            res.add("hList",hEmpArr);
            res.add("tAbAndLeaveList", totalabandl);
            res.add("absentPercentage", abPer);
            res.addProperty("totalDays", totalDays);
            res.addProperty("sumofAllEmployeeTotalDays", sumofAllEmployeeTotalDays);
            res.addProperty("sumOfAllEmployeePresenty", sumOfAllEmployeePresenty);
            res.addProperty("sumOfAllEmployeeAbsenty", sumOfAllEmployeeAbsenty);
            res.addProperty("sumOfAllEmployeeLeaves", sumOfAllEmployeeLeaves);
            res.addProperty("sumOfAllEmployeeHalfDays", sumOfAllEmployeeHalfDays);
            res.addProperty("totalEmployee", totalEmployees);

            res.addProperty("presentEmployees", presentEmployees);
            res.addProperty("halfDayEmployees",halfDayEmployees);
            res.addProperty("leaveEmployees", leaveEmployees);
            res.addProperty("absentEmployees", absentEmployees);
//            res.addProperty("totalEmployees",totalEmployees);

            response.add("response", res);
            response.addProperty("userDay", userDay);
            response.addProperty("responseStatus", HttpStatus.OK.value());


        } catch (Exception e) {
            attendanceLogger.error("attendnaceList " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonObject getSalaryReportMonthWise(Map<String, String> jsonRequest, HttpServletRequest request) {
        System.out.println("jsonRequest " + jsonRequest);
        JsonObject response = new JsonObject();
        JsonObject res = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            int daysInMonth = 0;
            System.out.println("jsonRequest " + jsonRequest.get("currentMonth"));
            String[] currentMonth = jsonRequest.get("currentMonth").split("-");
            String userMonth = currentMonth[1];
            String userYear = currentMonth[0];
            String userDay = "01";

            int cYear = LocalDate.now().getYear();
            int uYear = Integer.parseInt(userYear);
            if(cYear != uYear){
                response.addProperty("message", "Invalid Year");
                response.addProperty("responseStatus", HttpStatus.NOT_ACCEPTABLE.value());
                return response;
            }

            String newUserDate = userYear + "-" + userMonth + "-" + userDay;
            System.out.println("newUserDate" + newUserDate);
            LocalDate currentDate = LocalDate.parse(newUserDate);
            daysInMonth = getTotalDaysFromYearAndMonth(Integer.parseInt(userYear), Integer.parseInt(userMonth));
            System.out.println("totalDays" + daysInMonth);

            System.out.println("currentDate" + currentDate);
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            System.out.println("firstDateOfMonth" + firstDateOfMonth);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).plusDays(1);
            System.out.println("lastDateOfMonth " + lastDateOfMonth);

            List<LocalDate> localDates = firstDateOfMonth.datesUntil(lastDateOfMonth).collect(Collectors.toList());
            System.out.println("dates size" + localDates.size());

            List<LocalDate> localDates1 = firstDateOfMonth.datesUntil(lastDateOfMonth).collect(Collectors.toList());
            System.out.println("dates size" + localDates1.size());

            JsonObject jsonObject = new JsonObject();

            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {
                List<Employee> employees = employeeRepository.findByInstituteIdAndStatusOrderByFirstNameAsc(users.getInstitute().getId(), true);

                for (Employee employee1 : employees) {
                    double presentDays = 0;
                    Integer leaveDays = 0;
                    Integer absentDays = 0;
                    double extraDays = 0;
//                    double workedHours = 0.0;
                    JsonObject empObj = new JsonObject();

                    empObj.addProperty("id", employee1.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee1));
                    empObj.addProperty("salaryPerMonth", employee1.getExpectedSalary());
                    for (LocalDate localDate : localDates) {
                        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee1.getId(), localDate, true);
                        int hours = 0;
                        int minutes = 0;
                        if (attendance != null) {
                            LocalDateTime checkInTime = attendance.getCheckInTime();
                            LocalDateTime checkOutTime = attendance.getCheckOutTime();
                            if(checkOutTime != null){
                                if (localDate.getDayOfWeek().toString().contains(employee1.getWeeklyOffDay())) {
                                    if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay())
                                        extraDays+=0.5;
                                    else
                                        presentDays++;
                                } else {
                                    if(attendance.getIsHalfDay() != null && attendance.getIsHalfDay())
                                        presentDays+=0.5;
                                    else
                                        presentDays++;
                                }
                            }
                        } else {
                            EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqualAndLeaveStatus(employee1.getId(), localDate, localDate, "Approved");
                            if (employeeLeave != null) {
                                leaveDays++;
                            } else {
                                absentDays++;
                            }
                        }
                    }
                    double totalDaysOfEmployee = presentDays + leaveDays + absentDays;
                    double presentDaysSalary = 0.0;
                    double absentDaysSalary = 0.0;
                    double extraDaysSalary = 0.0;
                    double salaryDrawn = 0.0;
                    double wagePerDay = employee1.getExpectedSalary() / daysInMonth;
                    double wagesPerHour = (wagePerDay / utility.getTimeInDouble(employee1.getShift().getWorkingHours().toString()));
                    presentDaysSalary = presentDays * wagePerDay;
                    absentDaysSalary = absentDays * wagePerDay;
                    extraDaysSalary = extraDays * wagePerDay;
                    salaryDrawn = presentDaysSalary + extraDaysSalary;
                    empObj.addProperty("id", employee1.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee1));
                    empObj.addProperty("presentDays", presentDays);
                    empObj.addProperty("leaveDays", leaveDays);
                    empObj.addProperty("absentDays", absentDays);
                    empObj.addProperty("daysInMonth", daysInMonth);
                    empObj.addProperty("extraDays", extraDays);
                    empObj.addProperty("totalDaysOfEmployee", totalDaysOfEmployee);
                    empObj.addProperty("presentDaysSalary", presentDaysSalary);
                    empObj.addProperty("absentDaysSalary", absentDaysSalary);
                    empObj.addProperty("extraDaysSalary", extraDaysSalary);
                    empObj.addProperty("salaryDrawn", salaryDrawn);
                    empObj.addProperty("designation", employee1.getDesignation().getName());

                    List<AttendanceView> attendanceViewList = new ArrayList<>();
                    JsonArray attArray = new JsonArray();

                    String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+users.getInstitute().getId()+
                            " AND attendance_date between '" + firstDateOfMonth + "' AND '" + LocalDate.now() + "'"+
                            " AND employee_id="+employee1.getId()+" ORDER BY attendance_date ASC";
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
                    Query q = entityManager.createNativeQuery(query, AttendanceView.class);
                    attendanceViewList = q.getResultList();
                    System.out.println("attendanceViewList.size() " + attendanceViewList.size());
                    for (AttendanceView attendanceView : attendanceViewList) {
                        JsonArray breaksArray = new JsonArray();
                        JsonObject jsonObj = new JsonObject();
                        jsonObj.addProperty("id", attendanceView.getId());
                        jsonObj.addProperty("attendanceId", attendanceView.getId());
                        jsonObj.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                        jsonObj.addProperty("designationCode", employee1.getDesignation().getCode().toUpperCase());
                        jsonObj.addProperty("employeeId", employee1.getId());
                        jsonObj.addProperty("employeeName", employee1.getFullName());
                        jsonObj.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
                                attendanceView.getFinalDaySalaryType() : employee1.getEmployeeWagesType() != null ?
                                employee1.getEmployeeWagesType() : "");
                        jsonObj.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
                                attendanceView.getAttendanceStatus() : "pending");
                        jsonObj.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                        jsonObj.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                        jsonObj.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

                        jsonObj.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
                        jsonObj.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);

                        jsonObj.addProperty("wagesPerDay", Precision.round(wagePerDay, 2));
                        jsonObj.addProperty("wagesPerHour", Precision.round(wagesPerHour, 2));

                        if(attendanceView.getCheckOutTime() != null){
                            String[] timeParts = attendanceView.getTotalTime().split(":");
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            int seconds = Integer.parseInt(timeParts[2]);

                            int totalMinutes = hours * 60 + minutes + (seconds / 60);


                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
                        } else {
                            jsonObj.addProperty("actualWorkingHoursInMinutes", "-");
                        }
                        List<Object[]> breakList = taskViewRepository.findDataByTaskDateAndGroupByBreaks(attendanceView.getId(), 2,attendanceView.getAttendanceDate().toString(), true);
                        for (int j = 0; j < breakList.size(); j++) {
                            Object[] breakObj = breakList.get(j);

                            JsonObject breakObject = new JsonObject();
                            breakObject.addProperty("id", breakObj[0].toString());
                            breakObject.addProperty("breakName", breakObj[1].toString());
                            breakObject.addProperty("actualTime", Precision.round(Double.parseDouble(breakObj[2].toString()), 2));
                            breakObject.addProperty("breakWages", Precision.round(Double.parseDouble(breakObj[4].toString()), 2));

                            List<TaskView> downtimeViewList = taskViewRepository.findByAttendanceIdAndWorkBreakIdAndStatus(attendanceView.getId(), Long.valueOf(breakObj[0].toString()), true);
                            JsonArray downtimeArray = new JsonArray();
                            for (TaskView taskView : downtimeViewList) {
                                JsonObject taskObject = new JsonObject();
                                taskObject.addProperty("taskType", taskView.getTaskType());
                                taskObject.addProperty("taskId", taskView.getId());
                                taskObject.addProperty("remark", taskView.getRemark());
                                taskObject.addProperty("endRemark", taskView.getEndRemark());
                                taskObject.addProperty("adminRemark", taskView.getAdminRemark());
                                taskObject.addProperty("startTime", taskView.getStartTime().toString());
                                taskObject.addProperty("endTime", taskView.getEndTime() != null ? taskView.getEndTime().toString() : "");
                                taskObject.addProperty("workingHour", taskView.getWorkingHour() != null ?
                                        Precision.round(taskView.getWorkingHour(), 2) : 0.0);
                                taskObject.addProperty("totalTime", taskView.getTotalTime() != null ? Precision.round(taskView.getTotalTime(), 2) : 0);
                                taskObject.addProperty("breakName", taskView.getBreakName() != null ? taskView.getBreakName() : "");
                                taskObject.addProperty("workDone", taskView.getWorkDone() ? "Working" : "Not Working");
                                taskObject.addProperty("breakWages", taskView.getBreakWages() != null ?
                                        Precision.round(taskView.getBreakWages(), 2) : 0.0);

                                downtimeArray.add(taskObject);
                            }
                            breakObject.add("breakList", downtimeArray);
                            breaksArray.add(breakObject);
                        }
                        jsonObj.add("breakData",breaksArray);
                        attArray.add(jsonObj);
                    }
                    empObj.add("attData",attArray);
                    jsonArray.add(empObj);
                }
            } else {
                Long employeeId = Long.valueOf(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndInstituteIdAndStatus(employeeId, users.getInstitute().getId(), true);

                double presentDays = 0;
                Integer leaveDays = 0;
                Integer absentDays = 0;
                double extraDays = 0;
                double workedHours = 0.0;

                if (employee != null) {
                    for (LocalDate localDate : localDates) {
                        System.out.println(" localDate " + localDate);
                        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), localDate, true);
                        int hours = 0;
                        int minutes = 0;
                        if (attendance != null) {
                            LocalDateTime checkInTime = attendance.getCheckInTime();
                            LocalDateTime checkOutTime = attendance.getCheckOutTime();
                            if(checkOutTime != null){
                                LocalTime timeDiff = utility.getDateTimeDiffInTime(checkInTime, checkOutTime);
//                            LocalTime time = employee.getShift().getWorkingHours();
                                String[] timeParts = timeDiff.toString().split(":");
                                hours = Integer.parseInt(timeParts[0]);
                                minutes = Integer.parseInt(timeParts[1]);
                                workedHours = utility.getTimeInDouble(hours+":"+minutes);
                                if(workedHours > 0){
                                    if (localDate.getDayOfWeek().toString().contains(employee.getWeeklyOffDay())) {
                                        if(workedHours < 5)
                                            extraDays+=0.5;
                                        else
                                            presentDays++;
                                    } else {
                                        if(workedHours < 5)
                                            presentDays+=0.5;
                                        else
                                            presentDays++;
                                    }
                                }
                            }
                        } else {
                            EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), localDate, localDate);
                            if (employeeLeave != null) {
                                if (employeeLeave.getLeaveStatus().equals("Approved")) {
                                    leaveDays++;
                                } else {
                                    absentDays++;
                                }
                            } else {
                                absentDays++;
                            }
                        }
                    }
                    double totalDaysOfEmployee = presentDays + leaveDays + absentDays;
                    double presentDaysSalary = 0.0;
                    double absentDaysSalary = 0.0;
                    double extraDaysSalary = 0.0;
                    double salaryDrawn = 0.0;
                    double wagePerDay = employee.getExpectedSalary() / daysInMonth;
                    double wagesPerHour = (wagePerDay / utility.getTimeInDouble(employee.getShift().getWorkingHours().toString()));
                    double wagesPerMinute = wagesPerHour / 60;
                    presentDaysSalary = presentDays * wagePerDay;
                    absentDaysSalary = absentDays * wagePerDay;
                    extraDaysSalary = extraDays * wagePerDay;
                    salaryDrawn = presentDaysSalary + extraDaysSalary;
                    jsonObject.addProperty("id", employee.getId());
                    jsonObject.addProperty("employeeName", utility.getEmployeeName(employee));
                    jsonObject.addProperty("presentDays", presentDays);
                    jsonObject.addProperty("leaveDays", leaveDays);
                    jsonObject.addProperty("absentDays", absentDays);
                    jsonObject.addProperty("daysInMonth", daysInMonth);
                    jsonObject.addProperty("extraDays", extraDays);
                    jsonObject.addProperty("totalDaysOfEmployee", totalDaysOfEmployee);
                    jsonObject.addProperty("presentDaysSalary", presentDaysSalary);
                    jsonObject.addProperty("absentDaysSalary", absentDaysSalary);
                    jsonObject.addProperty("extraDaysSalary", extraDaysSalary);
                    jsonObject.addProperty("salaryDrawn", salaryDrawn);
                    jsonObject.addProperty("designation", employee.getDesignation().getName());
                    jsonObject.addProperty("totalDaysOfEmployee", totalDaysOfEmployee);
                    jsonObject.addProperty("salaryPerMonth", employee.getExpectedSalary());

                    List<AttendanceView> attendanceViewList = new ArrayList<>();
                    JsonArray attArray = new JsonArray();

                    String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+users.getInstitute().getId()+
                            " AND attendance_date between '" + firstDateOfMonth + "' AND '" + LocalDate.now() + "'"+
                            " AND employee_id="+employee.getId()+" ORDER BY attendance_date ASC";
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
                    Query q = entityManager.createNativeQuery(query, AttendanceView.class);
                    attendanceViewList = q.getResultList();
                    System.out.println("attendanceViewList.size() " + attendanceViewList.size());
                    for (AttendanceView attendanceView : attendanceViewList) {
                        JsonArray breaksArray = new JsonArray();
                        JsonObject jsonObj = new JsonObject();
                        jsonObj.addProperty("id", attendanceView.getId());
                        jsonObj.addProperty("attendanceId", attendanceView.getId());
                        jsonObj.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                        jsonObj.addProperty("designationCode", employee.getDesignation().getCode().toUpperCase());
                        jsonObj.addProperty("employeeId", employee.getId());
                        jsonObj.addProperty("employeeName", employee.getFullName());
                        jsonObj.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
                                attendanceView.getFinalDaySalaryType() : employee.getEmployeeWagesType() != null ?
                                employee.getEmployeeWagesType() : "");
                        jsonObj.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
                                attendanceView.getAttendanceStatus() : "pending");
                        jsonObj.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                        jsonObj.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                        jsonObj.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

                        jsonObj.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
                        jsonObj.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);

                        jsonObj.addProperty("wagesPerDay", Precision.round(wagePerDay, 2));
                        jsonObj.addProperty("wagesPerHour", Precision.round(wagesPerHour, 2));

                        if(attendanceView.getCheckOutTime() != null){
                            String[] timeParts = attendanceView.getTotalTime().split(":");
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            int seconds = Integer.parseInt(timeParts[2]);

                            int totalMinutes = hours * 60 + minutes + (seconds / 60);

                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
                        } else {
                            jsonObj.addProperty("actualWorkingHoursInMinutes", "-");
                        }
                        List<Object[]> breakList = taskViewRepository.findDataByTaskDateAndGroupByBreaks(attendanceView.getId(), 2,attendanceView.getAttendanceDate().toString(), true);
                        for (int j = 0; j < breakList.size(); j++) {
                            Object[] breakObj = breakList.get(j);

                            JsonObject breakObject = new JsonObject();
                            breakObject.addProperty("id", breakObj[0].toString());
                            breakObject.addProperty("breakName", breakObj[1].toString());
                            breakObject.addProperty("actualTime", Precision.round(Double.parseDouble(breakObj[2].toString()), 2));
                            breakObject.addProperty("breakWages", Precision.round(Double.parseDouble(breakObj[4].toString()), 2));

                            List<TaskView> downtimeViewList = taskViewRepository.findByAttendanceIdAndWorkBreakIdAndStatus(attendanceView.getId(), Long.valueOf(breakObj[0].toString()), true);
                            JsonArray downtimeArray = new JsonArray();
                            for (TaskView taskView : downtimeViewList) {
                                JsonObject taskObject = new JsonObject();
                                taskObject.addProperty("taskType", taskView.getTaskType());
                                taskObject.addProperty("taskId", taskView.getId());
                                taskObject.addProperty("remark", taskView.getRemark());
                                taskObject.addProperty("endRemark", taskView.getEndRemark());
                                taskObject.addProperty("adminRemark", taskView.getAdminRemark());
                                taskObject.addProperty("startTime", taskView.getStartTime().toString());
                                taskObject.addProperty("endTime", taskView.getEndTime() != null ? taskView.getEndTime().toString() : "");
                                taskObject.addProperty("workingHour", taskView.getWorkingHour() != null ?
                                        Precision.round(taskView.getWorkingHour(), 2) : 0.0);
                                taskObject.addProperty("totalTime", taskView.getTotalTime() != null ? Precision.round(taskView.getTotalTime(), 2) : 0);
                                taskObject.addProperty("breakName", taskView.getBreakName() != null ? taskView.getBreakName() : "");
                                taskObject.addProperty("workDone", taskView.getWorkDone() ? "Working" : "Not Working");
                                taskObject.addProperty("breakWages", taskView.getBreakWages() != null ?
                                        Precision.round(taskView.getBreakWages(), 2) : 0.0);

                                downtimeArray.add(taskObject);
                            }
                            breakObject.add("breakList", downtimeArray);
                            breaksArray.add(breakObject);
                        }
                        jsonObj.add("breakData",breaksArray);
                        attArray.add(jsonObj);
                    }
                    jsonObject.add("attData",attArray);
                    jsonArray.add(jsonObject);
                }
            }
            res.add("list", jsonArray);
            response.add("response", res);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("attendnaceList " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonObject getSingleDayAttendanceDetails(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
//        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        Long employeeId = Long.parseLong(jsonRequest.get("employeeId"));
        String attendanceDate = jsonRequest.get("attendanceDate");
        Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
        List<AttendanceView> attendanceViewList = new ArrayList<>();
        JsonObject jsonObj = new JsonObject();

        String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+employee.getInstitute().getId()+
                " AND attendance_date = '" + attendanceDate + "' AND employee_id="+employee.getId();
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
        Query q = entityManager.createNativeQuery(query, AttendanceView.class);
        attendanceViewList = q.getResultList();
        System.out.println("attendanceViewList.size() " + attendanceViewList.size());
        for (AttendanceView attendanceView : attendanceViewList) {
            JsonArray breaksArray = new JsonArray();

            jsonObj.addProperty("id", attendanceView.getId());
            jsonObj.addProperty("attendanceId", attendanceView.getId());
            jsonObj.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
//            jsonObj.addProperty("designationCode", employee.getDesignation().getCode().toUpperCase());
//            jsonObj.addProperty("employeeId", employee.getId());
//            jsonObj.addProperty("employeeName", employee.getFullName());
//            jsonObj.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
//                    attendanceView.getFinalDaySalaryType() : employee.getEmployeeWagesType() != null ?
//                    employee.getEmployeeWagesType() : "");
            jsonObj.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
                    attendanceView.getAttendanceStatus() : "pending");
            jsonObj.addProperty("checkInTime", attendanceView.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
            jsonObj.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
            jsonObj.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

//            jsonObj.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
//            jsonObj.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);

            if(attendanceView.getCheckOutTime() != null){
                String[] timeParts = attendanceView.getTotalTime().split(":");
                int hours = Integer.parseInt(timeParts[0]);
                int minutes = Integer.parseInt(timeParts[1]);
                int seconds = Integer.parseInt(timeParts[2]);

                int totalMinutes = hours * 60 + minutes + (seconds / 60);

                double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
            } else {
                jsonObj.addProperty("actualWorkingHoursInMinutes", "-");
            }
            List<Object[]> breakList = taskViewRepository.findDataByTaskDateAndGroupByBreaks(attendanceView.getId(), 2,attendanceView.getAttendanceDate().toString(), true);
            JsonArray downtimeArray = new JsonArray();
            for (int j = 0; j < breakList.size(); j++) {
                Object[] breakObj = breakList.get(j);

//                JsonObject breakObject = new JsonObject();
//                breakObject.addProperty("id", breakObj[0].toString());
//                breakObject.addProperty("breakName", breakObj[1].toString());
//                breakObject.addProperty("actualTime", Precision.round(Double.parseDouble(breakObj[2].toString()), 2));
//                breakObject.addProperty("breakWages", Precision.round(Double.parseDouble(breakObj[4].toString()), 2));

                List<TaskView> downtimeViewList = taskViewRepository.findByAttendanceIdAndWorkBreakIdAndStatus(attendanceView.getId(), Long.valueOf(breakObj[0].toString()), true);

                for (TaskView taskView : downtimeViewList) {
                    JsonObject taskObject = new JsonObject();
//                    taskObject.addProperty("taskType", taskView.getTaskType());
//                    taskObject.addProperty("taskId", taskView.getId());
//                    taskObject.addProperty("remark", taskView.getRemark());
//                    taskObject.addProperty("endRemark", taskView.getEndRemark());
//                    taskObject.addProperty("adminRemark", taskView.getAdminRemark());
                    taskObject.addProperty("startTime", taskView.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    taskObject.addProperty("endTime", taskView.getEndTime() != null ? taskView.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
//                    taskObject.addProperty("workingHour", taskView.getWorkingHour() != null ?
//                            Precision.round(taskView.getWorkingHour(), 2) : 0.0);
                    LocalTime timeDiff = null;
                    try {

                        if(taskView.getEndTime() != null) {
                            timeDiff = utility.getDateTimeDiffInTime(taskView.getStartTime(), taskView.getEndTime());
                        }
                    }catch (Exception e){
                        System.out.println(e);
                    }
                    taskObject.addProperty("totalTime", timeDiff != null ? timeDiff.toString() :"");
//                    taskObject.addProperty("breakName", taskView.getBreakName() != null ? taskView.getBreakName() : "");
//                    taskObject.addProperty("workDone", taskView.getWorkDone() ? "Working" : "Not Working");
//                    taskObject.addProperty("breakWages", taskView.getBreakWages() != null ?
//                            Precision.round(taskView.getBreakWages(), 2) : 0.0);

                    downtimeArray.add(taskObject);
                }
//                breakObject.add("breakList", downtimeArray);
//                breaksArray.add(breakObject);
            }
            jsonObj.add("breakList",downtimeArray);
        }
//        res.add("responseObjcet",jsonObj);
        response.add("response", jsonObj);
        response.addProperty("responseStatus", HttpStatus.OK.value());
        return response;
    }

    public JsonObject getAttendanceList(Map<String, String> jsonRequest, HttpServletRequest request) {
        System.out.println("jsonRequest " + jsonRequest);
        JsonObject response = new JsonObject();
        JsonObject res = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonArray taskList = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            System.out.println("employee.getId() " + employee.getId());

            LocalDate currentDate = LocalDate.now();
            Integer totalDays = 0;
            Integer pDays = 0;
            Integer lDays = 0;
            Boolean flag = false;
            if (!jsonRequest.get("currentMonth").equals("")) {
                System.out.println("jsonRequest " + jsonRequest.get("currentMonth"));
                String[] currentMonth = jsonRequest.get("currentMonth").split("-");
                String userMonth = currentMonth[0];
                String userYear = currentMonth[1];
                String userDay = "01";

                String newUserDate = userYear + "-" + userMonth + "-" + userDay;
                System.out.println("newUserDate " + newUserDate);
                currentDate = LocalDate.parse(newUserDate);
                totalDays = getTotalDaysFromYearAndMonth(Integer.parseInt(userYear), Integer.parseInt(userMonth));
                flag = true;
            }
            System.out.println("currentDate " + currentDate);
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            System.out.println("firstDateOfMonth " + firstDateOfMonth);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            System.out.println("lastDateOfMonth " + lastDateOfMonth);
            totalDays = getTotalDaysFromYearAndMonth(currentDate.getYear(), currentDate.getMonthValue());

            if (flag) {
                currentDate = lastDateOfMonth;
                totalDays = getTotalDaysFromYearAndMonth(currentDate.getYear(), currentDate.getMonthValue());
            }
            currentDate = currentDate.plusDays(1);

            List<LocalDate> localDates = firstDateOfMonth.datesUntil(currentDate).collect(Collectors.toList());
            System.out.println("dates " + localDates);

            for (LocalDate localDate : localDates) {
                System.out.println("localDate " + localDate);
                Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), localDate, true);
                if (attendance != null) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("attendanceStatus", "P");
                    jsonObject.addProperty("shiftName", attendance.getShift() != null ? attendance.getShift().getName() : "");
                    jsonObject.addProperty("attendanceId", attendance.getId());
                    jsonObject.addProperty("attendanceDate", String.valueOf(attendance.getAttendanceDate()));
                    jsonObject.addProperty("checkInTime", attendance.getCheckInTime() != null ? attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
                    jsonObject.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
//                    jsonObject.addProperty("totalTime", attendance.getTotalTime() != null ? String.valueOf(attendance.getTotalTime()) : "");
                    LocalTime totalTime = null;
                    if(attendance.getCheckOutTime() != null)
                        totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                    jsonObject.addProperty("totalTime", totalTime != null ? totalTime.toString() : "");
                    jsonObject.addProperty("status", attendance.getAttendanceStatus() != null ? attendance.getAttendanceStatus() : "pending");
                    jsonObject.addProperty("workingHours", attendance.getWorkingHours() != null ? Precision.round(attendance.getWorkingHours(), 2) : 0);
                    jsonObject.addProperty("finalDaySalaryType", attendance.getFinalDaySalaryType() != null ? attendance.getFinalDaySalaryType() : "");
                    jsonObject.addProperty("finalDaySalary", attendance.getFinalDaySalary() != null ? Precision.round(attendance.getFinalDaySalary(), 2) : 0);
                    jsonObject.addProperty("lunchTime", attendance.getLunchTime());
                    WorkBreak workBreak = workBreakRepository.findByBreakName(employee.getInstitute().getId());
                    TaskMaster taskMaster = null;
                    double lunchTimeInMin = 0;
                    LocalTime lunchTime = null;
                    if (workBreak != null) {
                        lunchTimeInMin = taskMasterRepository.getSumOfLunchTime(attendance.getId(), workBreak.getId(), true, false);
                        /*taskMaster = taskMasterRepository.findTop1ByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(attendance.getId(), workBreak.getId(), true, false);
                        if (taskMaster != null) {
                            lunchTimeInMin = taskMaster.getTotalTime();
                            lunchTime = taskMaster.getWorkingTime();
                        }*/
                    }
                    jsonObject.addProperty("lunchTime", String.valueOf(lunchTime));
                    jsonObject.addProperty("lunchTimeInMin", lunchTimeInMin);
                    jsonObject.addProperty("firstTaskStartTime", taskMasterRepository.getInTime(attendance.getId()) != null ?
                            taskMasterRepository.getInTime(attendance.getId()).toString() : "");
                    jsonObject.addProperty("lastTaskEndTime", taskMasterRepository.getOutTime(attendance.getId()) != null ?
                            taskMasterRepository.getOutTime(attendance.getId()).toString() : "");
                    jsonArray.add(jsonObject);
                    pDays++;
                } else {
                    JsonObject jsonObject = new JsonObject();
                    EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), localDate, localDate);
                    if (employeeLeave != null) {
                        if (employeeLeave.getLeaveStatus().equals("Approved")) {
                            jsonObject.addProperty("attendanceStatus", "PL");
                            jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                            jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                            jsonObject.addProperty("leaveName", employeeLeave.getLeaveType().getName());
                            jsonObject.addProperty("leaveReason", employeeLeave.getReason());
                            jsonObject.addProperty("approvedBy", employeeLeave.getLeaveApprovedBy());
                            jsonObject.addProperty("leaveRemark", employeeLeave.getLeaveRemark());

                            lDays++;
                        } else {
                            jsonObject.addProperty("attendanceStatus", "L");
                            jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                            jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                        }
                    } else if (localDate.isBefore(LocalDate.now())) {
                        jsonObject.addProperty("attendanceStatus", "A");
                        jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                        jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                    }
                    System.out.println("jsonObject.size() " + jsonObject.size());
                    if (jsonObject.size() > 0)
                        jsonArray.add(jsonObject);
                }
            }
            res.add("list", jsonArray);
            res.add("taskList", taskList);
            res.addProperty("totalDays", totalDays);
            res.addProperty("pDays", pDays);
            res.addProperty("lDays", lDays);

            response.add("response", res);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("attendnaceList " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonObject getEmpMonthlyAttendance(Map<String, String> jsonRequest, HttpServletRequest request) {
        System.out.println("jsonRequest " + jsonRequest);
        JsonObject response = new JsonObject();
        JsonObject res = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            System.out.println("employee.getId() " + employee.getId());

            LocalDate currentDate = LocalDate.now();
            Integer totalDays = 0;
            Integer pDays = 0;
            Integer lDays = 0;
            Boolean flag = false;
            if (!jsonRequest.get("currentMonth").equals("")) {
                System.out.println("jsonRequest " + jsonRequest.get("currentMonth"));
                String[] currentMonth = jsonRequest.get("currentMonth").split("-");
                String userMonth = currentMonth[0];
                String userYear = currentMonth[1];
                String userDay = "01";

                String newUserDate = userYear + "-" + userMonth + "-" + userDay;
                System.out.println("newUserDate " + newUserDate);
                currentDate = LocalDate.parse(newUserDate);
                totalDays = getTotalDaysFromYearAndMonth(Integer.parseInt(userYear), Integer.parseInt(userMonth));
                flag = true;
            }
            System.out.println("currentDate " + currentDate);
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            System.out.println("firstDateOfMonth " + firstDateOfMonth);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            System.out.println("lastDateOfMonth " + lastDateOfMonth);

            if (flag) {
                currentDate = lastDateOfMonth;
                totalDays = getTotalDaysFromYearAndMonth(currentDate.getYear(), currentDate.getMonthValue());
            }
            currentDate = currentDate.plusDays(1);

            List<LocalDate> localDates = firstDateOfMonth.datesUntil(currentDate).collect(Collectors.toList());
            System.out.println("dates " + localDates);

            for (LocalDate localDate : localDates) {
                Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), localDate, true);
                if (attendance != null) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("attendanceStatus", "P");
                    jsonObject.addProperty("shiftName", attendance.getShift() != null ? attendance.getShift().getName() : "");
                    jsonObject.addProperty("attendanceId", attendance.getId());
                    jsonObject.addProperty("attendanceDate", String.valueOf(attendance.getAttendanceDate()));
                    jsonObject.addProperty("checkInTime", attendance.getCheckInTime() != null ? String.valueOf(attendance.getCheckInTime()) : "");
                    jsonObject.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? String.valueOf(attendance.getCheckOutTime()) : "");
                    jsonObject.addProperty("totalTime", attendance.getTotalTime() != null ? String.valueOf(attendance.getTotalTime()) : "");

                    jsonArray.add(jsonObject);
                    pDays++;
                } else {
                    System.out.println("localDate " + localDate);
                    JsonObject jsonObject = new JsonObject();
                    EmployeeLeave employeeLeave = employeeLeaveRepository.findByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(employee.getId(), localDate, localDate);
                    if (employeeLeave != null) {
                        if (employeeLeave.getLeaveStatus().equals("Approved")) {
                            jsonObject.addProperty("attendanceStatus", "PL");
                            jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                            jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                            jsonObject.addProperty("leaveName", employeeLeave.getLeaveType().getName());
                            jsonObject.addProperty("leaveReason", employeeLeave.getReason());
                            jsonObject.addProperty("approvedBy", employeeLeave.getLeaveApprovedBy());
                            jsonObject.addProperty("leaveRemark", employeeLeave.getLeaveRemark());

                            lDays++;
                        } else {
                            jsonObject.addProperty("attendanceStatus", "A");
                            jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                            jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                        }
                    } else {
                        jsonObject.addProperty("attendanceStatus", "A");
                        jsonObject.addProperty("shiftName", employee.getShift() != null ? employee.getShift().getName() : "");
                        jsonObject.addProperty("attendanceDate", String.valueOf(localDate));
                    }
                    jsonArray.add(jsonObject);
                }
            }
            res.add("list", jsonArray);
            res.addProperty("totalDays", totalDays);
            res.addProperty("pDays", pDays);
            res.addProperty("lDays", lDays);

            response.add("response", res);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("attendnaceList " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public Integer getTotalDaysFromYearAndMonth(int userYear, int userMonth) {
        // Get the number of days in that month
        YearMonth yearMonthObject = YearMonth.of(userYear, userMonth);
        int daysInMonth = yearMonthObject.lengthOfMonth(); //28
        return daysInMonth;
    }

    public LocalTime getDateTimeDiffInTime(LocalDateTime fromDate, LocalDateTime toDate) throws ParseException {
        System.out.println("fromDate " + fromDate);
        System.out.println("toDate " + toDate);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date d1 = df.parse(fromDate.toString());
        Date d2 = df.parse(toDate.toString());

        long d = Math.abs(d2.getTime() - d1.getTime());
        /*long hh = Math.abs(d / (3600 * 1000));
        long mm = Math.abs((d - hh * 3600 * 1000) / (60 * 1000));
        System.out.printf("\n %02d:%02d \n", hh, mm);

        String totalTime = null;
        if (hh > 23) {
            totalTime = "16:00";
        } else {
            totalTime = (hh < 10 ? "0" + hh : hh) + ":" + (mm < 10 ? "0" + mm : mm);
        }
        System.out.println("totalTime " + totalTime);*/


        Duration duration = Duration.ofMillis(d);
        long seconds1 = duration.getSeconds();
        long HH = (seconds1 / 3600);
        long MM = ((seconds1 % 3600) / 60);
        long SS = (seconds1 % 60);
        String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS);
        System.out.println("String.format(\"%02d:%02d:%02d\", HH, MM, SS) " + String.format("%02d:%02d:%02d", HH, MM, SS));

        String totalTime = null;
        if (HH > 23) {
            totalTime = "16:00:00";
        } else {
            totalTime = timeInHHMMSS;
        }
        return LocalTime.parse(totalTime);
    }

    public Object getAttendanceHistory(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        String fromDate = request.get("fromDate");
        String toDate = request.get("toDate");

        List<AttendanceView> attendanceViewList = new ArrayList<>();
        List<AttendanceDTDTO> attendanceDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND Date(attendance_date) " + "BETWEEN '" + fromDate + "' AND '" + toDate + "' ORDER BY id DESC";
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, AttendanceView.class);

            attendanceViewList = q.getResultList();
            System.out.println("Limit total rows " + attendanceViewList.size());

            if (attendanceViewList.size() > 0) {
                for (AttendanceView attendanceView : attendanceViewList) {
                    attendanceDTDTOList.add(convertToDTDTO(attendanceView, true));
                }

                responseMessage.setResponse(attendanceDTDTOList);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("Data not exist");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            attendanceLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to load data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object updateAttendance(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        try {
            Long attendanceId = Long.valueOf(requestParam.get("attendanceId"));
            Attendance attendance = attendanceRepository.findByIdAndStatus(attendanceId, true);

            if (attendance != null) {
                AttendanceHistory attendanceHistory1 = attendanceHistoryRepository.save(convertToHistory(attendance, users));
                if (attendanceHistory1 != null) {
                    attendance.setAttendanceDate(LocalDate.parse(requestParam.get("attendanceDate")));

                    attendance.setCheckInTime(null);
                    if (!requestParam.get("checkInTime").equalsIgnoreCase("")) {
                        LocalDateTime localDateTime = LocalDateTime.parse(requestParam.get("checkInTime"), myFormatObj);
                        attendance.setCheckInTime(localDateTime);
                    }
                    attendance.setCheckOutTime(null);
                    if (!requestParam.get("checkOutTime").equalsIgnoreCase("")) {
                        LocalDateTime localDateTime = LocalDateTime.parse(requestParam.get("checkOutTime"), myFormatObj);
                        attendance.setCheckOutTime(localDateTime);
                    }
                    attendance.setTotalTime(null);
                    /*if (!requestParam.get("totalTime").equalsIgnoreCase("")) {
                        attendance.setTotalTime(LocalTime.parse(requestParam.get("totalTime")));
                    }*/

                    LocalTime totalTime = LocalTime.parse("00:00:00");
                    LocalDateTime firstTaskStartTime = null;
                    LocalDateTime lastTaskEndTime = null;
                    if (!requestParam.get("checkOutTime").equalsIgnoreCase("")) {

                        TaskMaster runningTask = taskMasterRepository.findTop1ByAttendanceIdAndStatusAndTaskStatus(attendanceId, true, "in-progress");
                        if (runningTask == null) {
                            if (attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l3") ||
                                    attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l2")) {
                                /*From Task Data*/
                                firstTaskStartTime = taskMasterRepository.getInTime(attendance.getId());
                                System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                                lastTaskEndTime = taskMasterRepository.getOutTime(attendance.getId());
                                System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                                if (firstTaskStartTime != null && !firstTaskStartTime.equals("null") && lastTaskEndTime != null && !lastTaskEndTime.equals("null")) {

                                    LocalDate fDate = firstTaskStartTime.toLocalDate();
                                    LocalDate tDate = lastTaskEndTime.toLocalDate();
                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< fDate " + fDate);
                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< tDate " + tDate);

                                    if (tDate.compareTo(fDate) > 0) {
                                        String newdtime = fDate + " " + lastTaskEndTime.toLocalTime();
                                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<< newdtime " + newdtime);
//                                    LocalDateTime newEndTime = LocalDateTime.parse(df2.format(newdtime));
                                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<< newdtime " + newdtime);
                                        lastTaskEndTime = LocalDateTime.of(fDate, lastTaskEndTime.toLocalTime());
                                    }
                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< lastTaskEndTime " + lastTaskEndTime);

                                    totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                    System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                    attendance.setTotalTime(totalTime);
                                } else {
                                /*firstTaskStartTime = LocalDateTime.now();
                                lastTaskEndTime = firstTaskStartTime;
                                totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendance.setTotalTime(totalTime);*/

                                    responseMessage.setMessage("Please start or end tasks");
                                    responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                                    return responseMessage;
                                }
                            } else {
                                /* From Attendance Data */
                                totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendance.setTotalTime(totalTime);
                            }
                        } else {
                            responseMessage.setMessage("Task already running, Please end running task.");
                            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                            return responseMessage;
                        }
                    }

                    double actualWorkTime = taskMasterRepository.getSumOfActualWorkTime(attendance.getId());
                    attendance.setActualWorkTime(actualWorkTime);


                    WorkBreak workBreak = workBreakRepository.findByBreakName(users.getInstitute().getId());
                    TaskMaster taskMaster = null;
                    double lunchTimeInMin = 0;
                    if (workBreak != null) {
                        lunchTimeInMin = taskMasterRepository.getSumOfLunchTime(attendance.getId(), workBreak.getId(), true, false);
                        /*taskMaster = taskMasterRepository.findByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(attendance.getId(), workBreak.getId(), true, false);
                        if (taskMaster != null) {
                            lunchTimeInMin = taskMaster.getTotalTime();
//                            lunchTime = taskMaster.getWorkingTime();
                        }*/
                    }
                    lunchTimeInMin = requestParam.get("lunchTimeInMin").equalsIgnoreCase("") ?
                            lunchTimeInMin : Double.valueOf(requestParam.get("lunchTimeInMin"));
                    attendance.setLunchTime(lunchTimeInMin);
                    System.out.println("lunchTimeInMin >>>>>>>>>>>>>>" + lunchTimeInMin);

                    double totalAllBreakMinutes = taskRepository.getSumOfAllBreaksWithNotWorking(attendance.getId(), 0,
                            workBreak.getId());
                    System.out.println("totalAllBreakMinutes >>>>>>>>>>>>>>" + totalAllBreakMinutes);

                    int s = 0;
                    int attendanceSec = 0;
                    double totalMinutes = 0;
                    double attendanceMinutes = 0;

                    if (!requestParam.get("checkOutTime").equalsIgnoreCase("")) {
                        // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                        s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                        System.out.println("s " + s);
                        attendanceSec = Math.abs(s);
                        System.out.println("attendanceSec " + attendanceSec);
                        attendanceMinutes = (attendanceSec / 60.0);
                        totalMinutes = attendanceMinutes - lunchTimeInMin - totalAllBreakMinutes;

                        /*if (lunchTime != null) {

                        } else {
                            // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                            s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                            attendanceSec = s;
                            System.out.println("attendanceSec " + attendanceSec);
                            attendanceMinutes = (double) attendanceSec / 60.0;
                            totalMinutes = attendanceMinutes;
                        }*/

                        System.out.println("attendanceMinutes " + attendanceMinutes);
                        System.out.println("totalMinutes " + totalMinutes);

                        double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                        double wagesHourBasis = attendance.getWagesPerHour() * workingHours;
                        System.out.println("workingHours " + workingHours);
                        System.out.println("wagesPerHour " + attendance.getWagesPerHour());
                        System.out.println("wagesHourBasis " + wagesHourBasis);

                        attendance.setWorkingHours(workingHours);
                        attendance.setWagesHourBasis(wagesHourBasis);
                    }

                    if (!requestParam.get("adminRemark").equalsIgnoreCase("")) {
                        attendance.setAdminRemark(requestParam.get("adminRemark"));
                    }

                    try {

                        String taskWagesData = taskRepository.getTaskWagesData(attendance.getId());
                        System.out.println("taskWagesData --> " + taskWagesData);
                        if (taskWagesData != null) {
                            String[] taskSalaryData = taskWagesData.split(",");

                            double wagesPcsBasis = Double.valueOf(taskSalaryData[2]);
                            double breakWages = Double.valueOf(taskSalaryData[6]);
                            double totalPcsWages = wagesPcsBasis + breakWages;

                            attendance.setActualProduction(Double.valueOf(taskSalaryData[0]));
                            attendance.setWagesPointBasis(Double.valueOf(taskSalaryData[1]));
                            attendance.setWagesPcsBasis(wagesPcsBasis);
                            attendance.setBreakWages(breakWages);
                            attendance.setNetPcsWages(totalPcsWages);
                            attendance.setTotalWorkPoint(Double.valueOf(taskSalaryData[3]));
                            attendance.setProdWorkingHours(Double.valueOf(taskSalaryData[4]));
                            attendance.setWorkingHourWithSetting(Double.valueOf(taskSalaryData[5]));
                        }
                        attendance.setUpdatedAt(LocalDateTime.now());
                        attendance.setUpdatedBy(users.getId());
                        attendance.setInstitute(users.getInstitute());
                        attendanceRepository.save(attendance);
                        responseMessage.setMessage("Attendance updated successfully");
                        responseMessage.setResponseStatus(HttpStatus.OK.value());
                    } catch (Exception e) {
                        System.out.println("Exception " + e.getMessage());
                        e.printStackTrace();
                        responseMessage.setMessage("Failed to update attendance");
                        responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                } else {
                    responseMessage.setMessage("Failed to update history");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            attendanceLogger.error("Failed to update attendance " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to update attendance");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    private AttendanceHistory convertToHistory(Attendance attendance, Users users) {
        AttendanceHistory attendanceHistory = new AttendanceHistory();
        attendanceHistory.setAttendanceId(attendance.getId());
        attendanceHistory.setEmployeeId(attendance.getEmployee().getId());
        attendanceHistory.setUpdatingUserId(users.getId());
        if (attendance.getShift() != null) {
            attendanceHistory.setShiftId(attendance.getShift().getId());
        }
        attendanceHistory.setAttendanceDate(attendance.getAttendanceDate());
        if (attendance.getCheckInTime() != null) {
            attendanceHistory.setCheckInTime(attendance.getCheckInTime());
        }
        if (attendance.getCheckOutTime() != null) {
            attendanceHistory.setCheckOutTime(attendance.getCheckOutTime());
        }
        if (attendance.getTotalTime() != null) {
            attendanceHistory.setTotalTime(attendance.getTotalTime());
        }
        attendanceHistory.setWagesPerDay(attendance.getWagesPerDay());
        attendanceHistory.setWagesPerHour(attendance.getWagesPerHour());
        attendanceHistory.setWagesPerMin(attendance.getWagesPerMin());
        attendanceHistory.setWagesPoint(attendance.getWagesPoint());
        attendanceHistory.setTotalProdQty(attendance.getTotalProdQty());
        attendanceHistory.setTotalWorkTime(attendance.getTotalWorkTime());
        attendanceHistory.setActualWorkTime(attendance.getActualWorkTime());
        attendanceHistory.setTotalWorkPoint(attendance.getTotalWorkPoint());
        attendanceHistory.setWagesPointBasis(attendance.getWagesPointBasis());
        attendanceHistory.setWagesHourBasis(attendance.getWagesHourBasis());
        attendanceHistory.setWagesMinBasis(attendance.getWagesMinBasis());
        attendanceHistory.setWagesPcsBasis(attendance.getWagesPcsBasis());
        attendanceHistory.setFinalDaySalaryType(attendance.getFinalDaySalaryType());
        attendanceHistory.setFinalDaySalary(attendance.getFinalDaySalary());

        attendanceHistory.setActualProduction(attendance.getActualProduction());
        attendanceHistory.setReworkQty(attendance.getReworkQty());
        attendanceHistory.setMachineRejectQty(attendance.getMachineRejectQty());
        attendanceHistory.setDoubtfulQty(attendance.getDoubtfulQty());
        attendanceHistory.setUnMachinedQty(attendance.getUnMachinedQty());
        attendanceHistory.setOkQty(attendance.getOkQty());

        attendanceHistory.setRemark(attendance.getRemark());
        attendanceHistory.setAdminRemark(attendance.getAdminRemark());
        attendanceHistory.setStatus(attendance.getStatus());
        attendanceHistory.setCreatedAt(attendance.getCreatedAt());
        attendanceHistory.setCreatedBy(attendance.getCreatedBy());
        attendanceHistory.setUpdatedAt(attendance.getUpdatedAt());
        attendanceHistory.setUpdatedBy(attendance.getUpdatedBy());

        return attendanceHistory;
    }

    public JsonObject getEmployeeAttendanceHistory(Map<String, String> request) {
        JsonObject responseMessage = new JsonObject();
        String fromDate = request.get("fromDate");
        String toDate = request.get("toDate");
        String employeeId = request.get("employeeId");

        List<AttendanceView> attendanceViewList = new ArrayList<>();
        JsonArray jsonArray = new JsonArray();
        try {

            if (employeeId.equalsIgnoreCase("all")) {
                List<Employee> employees = employeeRepository.findByStatus(true);

                for (Employee employee : employees) {
                    String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND employee_id='" +
                            employee.getId() + "' AND  Date(attendance_date) BETWEEN '" + fromDate + "' AND '" + toDate + "' ORDER BY id DESC";
                    System.out.println("query " + query);

                    Query q = entityManager.createNativeQuery(query, AttendanceView.class);

                    attendanceViewList = q.getResultList();
                    System.out.println("Limit total rows" + attendanceViewList.size());
                    if (attendanceViewList.size() > 0) {
                        for (AttendanceView attendanceView : attendanceViewList) {
                            if (employee.getDesignation().getCode().equalsIgnoreCase("l3")) {
                                List<Object[]> taskList = taskViewRepository.findDataGroupByOperation(attendanceView.getEmployeeId(), attendanceView.getId(), "1", true);
                                System.out.println("total taskList rows" + taskList.size());
                                for (int i = 0; i < taskList.size(); i++) {
                                    Object[] taskObj = taskList.get(i);
                                    JsonObject jsonObject = new JsonObject();
                                    double totalWorkTime = Precision.round(attendanceView.getTotalWorkTime(), 2);
                                    double actualWorkTime = Precision.round(attendanceView.getActualWorkTime(), 2);
                                    double breakTime = Precision.round(totalWorkTime - actualWorkTime, 2);

                                    jsonObject.addProperty("employeeId", attendanceView.getEmployeeId());
                                    jsonObject.addProperty("fullName", attendanceView.getFullName());
                                    jsonObject.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                                    jsonObject.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                                    jsonObject.addProperty("totalWorkTime", totalWorkTime);
                                    jsonObject.addProperty("workBreakId", 0);

                                    Object[] taskData = taskViewRepository.findTaskData(attendanceView.getEmployeeId(), attendanceView.getId(), taskObj[0].toString(), true);
                                    if (taskData != null) {
                                        jsonObject.addProperty("machineName", taskData[2].toString());
                                        jsonObject.addProperty("machineNo", taskData[3].toString());
                                        jsonObject.addProperty("jobName", taskData[4].toString());
                                        jsonObject.addProperty("jobOperationName", taskData[1].toString());
                                        jsonObject.addProperty("cycleTime", taskData[5].toString());
                                        jsonObject.addProperty("pcsRate", taskData[6].toString());
                                    }
                                    jsonObject.addProperty("requiredQty", taskObj[2].toString());
                                    jsonObject.addProperty("actualQty", taskObj[3].toString());
                                    jsonObject.addProperty("totalQty", taskObj[4].toString());
                                    jsonObject.addProperty("okQty", taskObj[5].toString());
                                    jsonObject.addProperty("reworkQty", taskObj[6].toString());
                                    jsonObject.addProperty("machineRejectQty", taskObj[7].toString());
                                    jsonObject.addProperty("doubtfulQty", taskObj[8].toString());
                                    jsonObject.addProperty("unMachinedQty", taskObj[9].toString());
                                    jsonObject.addProperty("actualWorkTime", taskObj[10].toString());
                                    jsonObject.addProperty("breakTime", breakTime);
                                    jsonObject.addProperty("wagesPerDay", Precision.round(attendanceView.getWagesPerDay(), 2));
                                    jsonObject.addProperty("wagesInHour", Precision.round(attendanceView.getWagesHourBasis(), 2));

                                    jsonArray.add(jsonObject);
                                }

                                List<Object[]> taskList2 = taskViewRepository.findBreakDataGroupByOperation(attendanceView.getEmployeeId(), attendanceView.getId(), "2", true);
                                System.out.println("total taskList rows" + taskList2.size());
                                for (int i = 0; i < taskList2.size(); i++) {
                                    Object[] taskObj = taskList2.get(i);

                                    JsonObject jsonObject = new JsonObject();
                                    double totalWorkTime = Precision.round(attendanceView.getTotalWorkTime(), 2);
                                    double actualWorkTime = Precision.round(attendanceView.getActualWorkTime(), 2);
                                    double breakTime = Precision.round(totalWorkTime - actualWorkTime, 2);

                                    jsonObject.addProperty("employeeId", attendanceView.getEmployeeId());
                                    jsonObject.addProperty("fullName", attendanceView.getFullName());
                                    jsonObject.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                                    jsonObject.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                                    jsonObject.addProperty("totalWorkTime", totalWorkTime);

                                    jsonObject.addProperty("workBreakId", taskObj[0].toString());
                                    jsonObject.addProperty("workBreakName", taskObj[1].toString());
                                    jsonObject.addProperty("machineName", "");
                                    jsonObject.addProperty("machineNo", "");
                                    jsonObject.addProperty("jobName", "");
                                    jsonObject.addProperty("jobOperationName", "");
                                    jsonObject.addProperty("cycleTime", "");
                                    jsonObject.addProperty("pcsRate", "");

                                    jsonObject.addProperty("requiredQty", "");
                                    jsonObject.addProperty("actualQty", "");
                                    jsonObject.addProperty("totalQty", "");
                                    jsonObject.addProperty("okQty", "");
                                    jsonObject.addProperty("reworkQty", "");
                                    jsonObject.addProperty("machineRejectQty", "");
                                    jsonObject.addProperty("doubtfulQty", "");
                                    jsonObject.addProperty("unMachinedQty", "");
                                    jsonObject.addProperty("actualWorkTime", taskObj[2].toString());
                                    jsonObject.addProperty("breakTime", breakTime);
                                    jsonObject.addProperty("wagesPerDay", Precision.round(attendanceView.getWagesPerDay(), 2));
                                    jsonObject.addProperty("wagesInHour", Precision.round(attendanceView.getWagesHourBasis(), 2));

                                    jsonArray.add(jsonObject);
                                }

                            }
                        }
                    }
                }
                responseMessage.add("response", jsonArray);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {

                String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND employee_id='" + employeeId
                        + "' AND  Date(attendance_date) BETWEEN '" + fromDate + "' AND '" + toDate + "' ORDER BY id DESC";
                System.out.println("query " + query);

                Query q = entityManager.createNativeQuery(query, AttendanceView.class);

                attendanceViewList = q.getResultList();
                System.out.println("Limit total rows" + attendanceViewList.size());

                if (attendanceViewList.size() > 0) {
                    for (AttendanceView attendanceView : attendanceViewList) {
                        List<TaskView> taskViewList = taskViewRepository.findByEmployeeIdAndAttendanceIdAndStatusOrderById(attendanceView.getEmployeeId(), attendanceView.getId(), true);
                        System.out.println("total rows getEmployeeYearlyAttendencegetEmployeeYearlyAttendence" + taskViewList.size());
                        for (TaskView taskView : taskViewList) {
                            JsonObject jsonObject = new JsonObject();

                            double totalWorkTime = Precision.round(attendanceView.getTotalWorkTime(), 2);
                            double actualWorkTime = Precision.round(attendanceView.getActualWorkTime(), 2);
                            double breakTime = Precision.round(totalWorkTime - actualWorkTime, 2);

                            jsonObject.addProperty("employeeId", attendanceView.getEmployeeId());
                            jsonObject.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                            jsonObject.addProperty("fullName", attendanceView.getFullName());
                            jsonObject.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                            jsonObject.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                            jsonObject.addProperty("totalWorkTime", totalWorkTime);
                            jsonObject.addProperty("workingHour", attendanceView.getWorkingHours() != null ?
                                    Precision.round(attendanceView.getWorkingHours(), 2) : 0);
                            jsonObject.addProperty("workBreakId", 0);
                            jsonObject.addProperty("taskType", taskView.getTaskType());

                            if (taskView.getWorkBreakId() != null) {
                                jsonObject.addProperty("workBreakName", taskView.getBreakName());
                            }
                            if (taskView.getTaskType() == 1) {
                                jsonObject.addProperty("machineName", taskView.getMachineName());
                                jsonObject.addProperty("machineNo", taskView.getMachineNumber());
                                jsonObject.addProperty("jobName", taskView.getJobName());
                                jsonObject.addProperty("jobOperationName", taskView.getOperationName());
                                jsonObject.addProperty("cycleTime", taskView.getCycleTime());
                                jsonObject.addProperty("pcsRate", taskView.getPcsRate());
                            } else if (taskView.getTaskType() == 3) {
                                jsonObject.addProperty("machineName", taskView.getMachineName());
                                jsonObject.addProperty("machineNo", taskView.getMachineNumber());
                            }
                            jsonObject.addProperty("requiredQty", taskView.getRequiredProduction() != null ?
                                    Precision.round(taskView.getRequiredProduction(), 2) : 0);
                            jsonObject.addProperty("actualQty", taskView.getActualProduction());
                            jsonObject.addProperty("totalQty", taskView.getTotalCount());
                            jsonObject.addProperty("okQty", taskView.getOkQty());
                            jsonObject.addProperty("reworkQty", taskView.getReworkQty());
                            jsonObject.addProperty("machineRejectQty", taskView.getMachineRejectQty());
                            jsonObject.addProperty("doubtfulQty", taskView.getDoubtfulQty());
                            jsonObject.addProperty("unMachinedQty", taskView.getUnMachinedQty());
                            jsonObject.addProperty("actualWorkTime", actualWorkTime);
                            jsonObject.addProperty("breakTime", breakTime);
                            jsonObject.addProperty("wagesPerDay", Precision.round(attendanceView.getWagesPerDay(), 2));
                            jsonObject.addProperty("wagesInHour", Precision.round(attendanceView.getWagesHourBasis(), 2));

                            jsonArray.add(jsonObject);
                        }
                    }

                    responseMessage.add("response", jsonArray);
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } else {
                    responseMessage.addProperty("message", "Data not exist");
                    responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                }
            }
        } catch (Exception e) {
            attendanceLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public InputStream getEmployeeAttendanceReport(String fromDate, String toDate, String employeeId, HttpServletRequest req) {
        JsonObject responseMessage = new JsonObject();

        List<AttendanceView> attendanceViewList = new ArrayList<>();
        JsonArray jsonArray = new JsonArray();
        try {
            String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND employee_id='" + employeeId
                    + "' AND  Date(attendance_date) BETWEEN '" + fromDate + "' AND '" + toDate + "'";
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, AttendanceView.class);

            attendanceViewList = q.getResultList();
            System.out.println("Limit total rows " + attendanceViewList.size());

            if (attendanceViewList.size() > 0) {

                for (AttendanceView attendanceView : attendanceViewList) {
                    List<TaskView> taskViewList = taskViewRepository.findByEmployeeIdAndAttendanceIdAndStatusOrderById(attendanceView.getEmployeeId(), attendanceView.getId(), true);
                    System.out.println("total rows " + taskViewList.size());
                    for (TaskView taskView : taskViewList) {
                        JsonObject jsonObject = new JsonObject();

                        double totalWorkTime = Precision.round(attendanceView.getTotalWorkTime(), 2);
                        double actualWorkTime = Precision.round(attendanceView.getActualWorkTime(), 2);
                        double breakTime = Precision.round(totalWorkTime - actualWorkTime, 2);

                        jsonObject.addProperty("employeeId", attendanceView.getEmployeeId());
                        jsonObject.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                        jsonObject.addProperty("fullName", attendanceView.getFullName());
                        jsonObject.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                        jsonObject.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                        jsonObject.addProperty("totalWorkTime", totalWorkTime);
                        jsonObject.addProperty("workingHours", attendanceView.getWorkingHours() != null ?
                                Precision.round(attendanceView.getWorkingHours(), 2) : 0);
                        jsonObject.addProperty("workBreakId", 0);
                        jsonObject.addProperty("taskType", taskView.getTaskType());

                        if (taskView.getWorkBreakId() != null) {
                            jsonObject.addProperty("workBreakName", taskView.getBreakName());
                        }
                        if (taskView.getTaskType() == 1) {
                            jsonObject.addProperty("machineName", taskView.getMachineName());
                            jsonObject.addProperty("machineNo", taskView.getMachineNumber());
                            jsonObject.addProperty("jobName", taskView.getJobName());
                            jsonObject.addProperty("jobOperationName", taskView.getOperationName());
                            jsonObject.addProperty("cycleTime", taskView.getCycleTime());
                            jsonObject.addProperty("pcsRate", taskView.getPcsRate());
                        } else if (taskView.getTaskType() == 3) {
                            jsonObject.addProperty("machineName", taskView.getMachineName());
                            jsonObject.addProperty("machineNo", taskView.getMachineNumber());
                        }
                        jsonObject.addProperty("requiredQty", taskView.getRequiredProduction() != null ?
                                Precision.round(taskView.getRequiredProduction(), 2) : 0);
                        jsonObject.addProperty("actualQty", taskView.getActualProduction() != null ?
                                taskView.getActualProduction() : 0);
                        jsonObject.addProperty("totalQty", taskView.getTotalCount() != null ?
                                taskView.getTotalCount() : 0);
                        jsonObject.addProperty("okQty", taskView.getOkQty() != null ? taskView.getOkQty() : 0);
                        jsonObject.addProperty("reworkQty", taskView.getReworkQty() != null ? taskView.getReworkQty() : 0);
                        jsonObject.addProperty("machineRejectQty", taskView.getMachineRejectQty() != null ?
                                taskView.getMachineRejectQty() : 0);
                        jsonObject.addProperty("doubtfulQty", taskView.getDoubtfulQty() != null ? taskView.getDoubtfulQty() : 0);
                        jsonObject.addProperty("unMachinedQty", taskView.getUnMachinedQty() != null ?
                                taskView.getUnMachinedQty() : 0);
                        jsonObject.addProperty("actualWorkTime", actualWorkTime);
                        jsonObject.addProperty("breakTime", breakTime);
                        jsonObject.addProperty("wagesPerDay", Precision.round(attendanceView.getWagesPerDay(), 2));
                        jsonObject.addProperty("wagesInHour", Precision.round(attendanceView.getWagesHourBasis(), 2));
                        jsonObject.addProperty("breakWages", Precision.round(attendanceView.getBreakWages(), 2));

                        jsonArray.add(jsonObject);
                    }
                }

                ByteArrayInputStream in = convertToExcel(jsonArray);

                return in;
            } else {
                responseMessage.addProperty("message", "Data not exist");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            attendanceLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
        throw new RuntimeException("fail to import data to Excel file: ");
    }

    private ByteArrayInputStream convertToExcel(JsonArray jsonArray) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(empAttSHEET);

            // Header
            Row headerRow = sheet.createRow(0);

            // Define header cell style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int col = 0; col < empAttHEADERs.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(empAttHEADERs[col]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (JsonElement jsonElement : jsonArray) {
                JsonObject obj = jsonElement.getAsJsonObject();
                Row row = sheet.createRow(rowIdx++);
                try {
                    row.createCell(0).setCellValue(obj.get("fullName").getAsString());
                    String attendanceDate = obj.get("attendanceDate").getAsString();
                    System.out.println("attendanceDate " + attendanceDate);
                    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    attendanceDate = LocalDate.parse(attendanceDate, formatter1).format(DateTimeFormatter.ofPattern("dd-MM-yy"));
                    System.out.println("attendanceDate " + attendanceDate);
                    //set Date
                    row.createCell(1).setCellValue(attendanceDate); // attendance date

                    row.createCell(2).setCellValue(obj.get("checkInTime").getAsString());
                    row.createCell(3).setCellValue(obj.get("checkOutTime").getAsString());
                    row.createCell(4).setCellValue(obj.get("workingHours").getAsDouble());

                    row.createCell(5).setCellValue("-");
                    row.createCell(6).setCellValue("-");
                    row.createCell(7).setCellValue("-");
                    row.createCell(8).setCellValue(0);
                    row.createCell(9).setCellValue(0);
                    row.createCell(10).setCellValue(0);
                    row.createCell(11).setCellValue(0);
                    row.createCell(12).setCellValue(0);
                    row.createCell(13).setCellValue(0);
                    row.createCell(14).setCellValue(0);
                    row.createCell(15).setCellValue(0);
                    row.createCell(16).setCellValue(0);
                    row.createCell(17).setCellValue(0);
                    row.createCell(18).setCellValue("");
                    row.createCell(19).setCellValue("");
                    row.createCell(20).setCellValue("");
                    row.createCell(21).setCellValue("");
                    row.createCell(22).setCellValue("");
                    row.createCell(23).setCellValue("");

                    if (obj.get("taskType").getAsInt() == 1) {
                        row.createCell(5).setCellValue(obj.get("machineNo").getAsString());
                        row.createCell(6).setCellValue(obj.get("jobName").getAsString());
                        row.createCell(7).setCellValue(obj.get("jobOperationName").getAsString());
                        row.createCell(8).setCellValue(obj.get("cycleTime").getAsDouble());
                        row.createCell(9).setCellValue(obj.get("pcsRate").getAsDouble());

                        row.createCell(9).setCellValue(obj.get("requiredQty").getAsDouble());
                        row.createCell(10).setCellValue(obj.get("actualQty").getAsDouble());
                        row.createCell(11).setCellValue(obj.get("okQty").getAsDouble());
                        row.createCell(12).setCellValue(obj.get("reworkQty").getAsDouble());
                        row.createCell(13).setCellValue(obj.get("machineRejectQty").getAsDouble());
                        row.createCell(14).setCellValue(obj.get("doubtfulQty").getAsDouble());
                        row.createCell(15).setCellValue(obj.get("unMachinedQty").getAsDouble());

                    } else if (obj.get("taskType").getAsInt() == 2) {
                        row.createCell(6).setCellValue(obj.get("workBreakName").getAsString());
                    } else if (obj.get("taskType").getAsInt() == 3) {
                        row.createCell(5).setCellValue(obj.get("machineNo").getAsString());
                    }

                    row.createCell(16).setCellValue(obj.get("actualWorkTime").getAsDouble());
                    row.createCell(17).setCellValue(obj.get("breakTime").getAsDouble());
                    row.createCell(18).setCellValue(obj.get("wagesInHour").getAsDouble());
                    row.createCell(19).setCellValue(obj.get("wagesInPoint").getAsDouble());
                    row.createCell(20).setCellValue(obj.get("wagesInPcs").getAsDouble());
                    row.createCell(21).setCellValue(obj.get("breakWages").getAsDouble());
                    row.createCell(22).setCellValue(obj.get("netPcsWages").getAsDouble());
                    row.createCell(23).setCellValue(obj.get("wagesPerDay").getAsDouble());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception e");
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            byte[] b = new ByteArrayInputStream(out.toByteArray()).readAllBytes();
            if (b.length > 0) {
                String s = new String(b);
                System.out.println("data ------> " + s);
            } else {
                System.out.println("Empty");
            }
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public InputStream getEmployeeSalaryReportInExcel(String employeeId, String currentMonthByUser, HttpServletRequest req) {
        JsonObject responseMessage = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(req.getHeader("Authorization").substring(7));
        List<AttendanceView> attendanceViewList = new ArrayList<>();
        JsonArray jsonArray = new JsonArray();

        System.out.println("jsonRequest " +currentMonthByUser);
        String[] currentMonth = currentMonthByUser.split("-");
        String userMonth = currentMonth[1];
        String userYear = currentMonth[0];
        String userDay = "01";

        int cYear = LocalDate.now().getYear();
        int uYear = Integer.parseInt(userYear);
//        if(cYear != uYear){
//            responseMessage.addProperty("message", "Invalid Year");
//            responseMessage.addProperty("responseStatus", HttpStatus.NOT_ACCEPTABLE.value());
//            return responseMessage;
//        }
        int daysInMonth = getTotalDaysFromYearAndMonth(Integer.parseInt(userYear), Integer.parseInt(userMonth));
        String newUserDate = userYear + "-" + userMonth + "-" + userDay;
        System.out.println("newUserDate" + newUserDate);
        LocalDate currentDate = LocalDate.parse(newUserDate);

        System.out.println("currentDate" + currentDate);
        LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
        System.out.println("firstDateOfMonth" + firstDateOfMonth);
        try {
//            String query = "SELECT * FROM attendance_view WHERE attendance_view.status=1 AND employee_id='" + employeeId
//                    + "' AND  Date(attendance_date) BETWEEN '" + firstDateOfMonth + "' AND '" + LocalDate.now() + "'";
            if(employeeId.equalsIgnoreCase("all")){

                List<Employee> employees = employeeRepository.findByInstituteIdAndStatusOrderByFirstNameAsc(users.getInstitute().getId(), true);
                for (Employee employee1 : employees) {
                    String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+users.getInstitute().getId()+
                            " AND attendance_date between '" + firstDateOfMonth + "' AND '" + LocalDate.now() + "'"+
                            " AND employee_id="+employee1.getId()+" ORDER BY attendance_date ASC";
                    System.out.println("query " + query);
                    Query q = entityManager.createNativeQuery(query, AttendanceView.class);

                    attendanceViewList = q.getResultList();
                    System.out.println("Limit total rows " + attendanceViewList.size());
                    double wagePerDay = employee1.getExpectedSalary() / daysInMonth;
                    double wagesPerHour = (wagePerDay / utility.getTimeInDouble(employee1.getShift().getWorkingHours().toString()));

                    if (attendanceViewList.size() > 0) {

                        for (AttendanceView attendanceView : attendanceViewList) {
                            JsonObject jsonObj = new JsonObject();
                            jsonObj.addProperty("id", attendanceView.getId());
                            jsonObj.addProperty("attendanceId", attendanceView.getId());
                            jsonObj.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                            jsonObj.addProperty("designationCode", employee1.getDesignation().getCode().toUpperCase());
                            jsonObj.addProperty("employeeId", employee1.getId());
//                            jsonObj.addProperty("fullName", employee1.getFullName());
                            jsonObj.addProperty("fullName", attendanceView.getFullName());
                            jsonObj.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
                                    attendanceView.getFinalDaySalaryType() : employee1.getEmployeeWagesType() != null ?
                                    employee1.getEmployeeWagesType() : "");
                            jsonObj.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
                                    attendanceView.getAttendanceStatus() : "pending");
                            jsonObj.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                            jsonObj.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                            jsonObj.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

                            jsonObj.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
                            jsonObj.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);

                            jsonObj.addProperty("wagesPerDay", Precision.round(wagePerDay, 2));
                            jsonObj.addProperty("wagesPerHour", Precision.round(wagesPerHour, 2));

                            if(attendanceView.getCheckOutTime() != null){
                                String[] timeParts = attendanceView.getTotalTime().split(":");
                                int hours = Integer.parseInt(timeParts[0]);
                                int minutes = Integer.parseInt(timeParts[1]);
                                int seconds = Integer.parseInt(timeParts[2]);

                                int totalMinutes = hours * 60 + minutes + (seconds / 60);


                                double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                                jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
                            } else {
                                jsonObj.addProperty("actualWorkingHoursInMinutes", "-");
                            }
                            jsonArray.add(jsonObj);
                        }
                    }
                }
                ByteArrayInputStream in = convertSalaryReportToExcel(jsonArray);
                return in;
            } else {
                Employee employee1 = employeeRepository.findByIdAndStatus(Long.parseLong(employeeId), true);

                String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+users.getInstitute().getId()+
                        " AND attendance_date between '" + firstDateOfMonth + "' AND '" + LocalDate.now() + "'"+
                        " AND employee_id="+employeeId+" ORDER BY attendance_date ASC";
                System.out.println("query " + query);

                Query q = entityManager.createNativeQuery(query, AttendanceView.class);

                attendanceViewList = q.getResultList();
                System.out.println("Limit total rows " + attendanceViewList.size());
                double wagePerDay = employee1.getExpectedSalary() / daysInMonth;
                double wagesPerHour = (wagePerDay / utility.getTimeInDouble(employee1.getShift().getWorkingHours().toString()));

                if (attendanceViewList.size() > 0) {

                    for (AttendanceView attendanceView : attendanceViewList) {
                        JsonObject jsonObj = new JsonObject();
                        jsonObj.addProperty("id", attendanceView.getId());
                        jsonObj.addProperty("attendanceId", attendanceView.getId());
                        jsonObj.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                        jsonObj.addProperty("designationCode", employee1.getDesignation().getCode().toUpperCase());
                        jsonObj.addProperty("employeeId", employee1.getId());
//                        jsonObj.addProperty("employeeName", employee1.getFullName());
                        jsonObj.addProperty("fullName", attendanceView.getFullName());
                        jsonObj.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
                                attendanceView.getFinalDaySalaryType() : employee1.getEmployeeWagesType() != null ?
                                employee1.getEmployeeWagesType() : "");
                        jsonObj.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
                                attendanceView.getAttendanceStatus() : "pending");
                        jsonObj.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                        jsonObj.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                        jsonObj.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

                        jsonObj.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
                        jsonObj.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);

                        jsonObj.addProperty("wagesPerDay", Precision.round(wagePerDay, 2));
                        jsonObj.addProperty("wagesPerHour", Precision.round(wagesPerHour, 2));

                        if(attendanceView.getCheckOutTime() != null){
                            String[] timeParts = attendanceView.getTotalTime().split(":");
                            int hours = Integer.parseInt(timeParts[0]);
                            int minutes = Integer.parseInt(timeParts[1]);
                            int seconds = Integer.parseInt(timeParts[2]);

                            int totalMinutes = hours * 60 + minutes + (seconds / 60);


                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
                        } else {
                            jsonObj.addProperty("actualWorkingHoursInMinutes", "-");
                        }
                        jsonArray.add(jsonObj);
                    }
                    ByteArrayInputStream in = convertSalaryReportToExcel(jsonArray);
                    return in;
                } else {
                    responseMessage.addProperty("message", "Data not exist");
                    responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                }
            }
        } catch (Exception e) {
            attendanceLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
        throw new RuntimeException("fail to import data to Excel file: ");
    }

    private ByteArrayInputStream convertSalaryReportToExcel(JsonArray jsonArray) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(empAttSHEET);

            // Header
            Row headerRow = sheet.createRow(0);

            // Define header cell style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int col = 0; col < empSalHEADERs.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(empSalHEADERs[col]);
                cell.setCellStyle(headerCellStyle);
            }

            int rowIdx = 1;
            for (JsonElement jsonElement : jsonArray) {
                JsonObject obj = jsonElement.getAsJsonObject();
                Row row = sheet.createRow(rowIdx++);
                try {
                    row.createCell(0).setCellValue(obj.get("fullName").getAsString());
                    String attendanceDate = obj.get("attendanceDate").getAsString();
                    System.out.println("attendanceDate " + attendanceDate);
                    DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    attendanceDate = LocalDate.parse(attendanceDate, formatter1).format(DateTimeFormatter.ofPattern("dd-MM-yy"));
                    System.out.println("attendanceDate " + attendanceDate); //set Date

                    row.createCell(1).setCellValue(attendanceDate); // attendance date
                    row.createCell(2).setCellValue(obj.get("designationCode").getAsString());
                    row.createCell(3).setCellValue(obj.get("employeeId").getAsDouble());
//                    row.createCell(4).setCellValue(obj.get("employeeName").getAsString());
//                    row.createCell(4).setCellValue(obj.get("employeeWagesType").getAsString());
//                    row.createCell(5).setCellValue(obj.get("attendanceStatus").getAsString());
                    row.createCell(4).setCellValue(obj.get("checkInTime").getAsString());
                    row.createCell(5).setCellValue(obj.get("checkOutTime") != null ? obj.get("checkOutTime").getAsString():"-");
                    row.createCell(6).setCellValue(obj.get("totalTime").getAsString());
                    row.createCell(7).setCellValue(obj.get("lunchTimeInMin").getAsString());
                    row.createCell(8).setCellValue(obj.get("workingHours").getAsDouble());
                    row.createCell(9).setCellValue(obj.get("wagesPerDay").getAsDouble());
                    row.createCell(10).setCellValue(obj.get("wagesPerHour").getAsDouble());
//                    row.createCell(13).setCellValue(obj.get("actualWorkingHoursInMinutes").getAsDouble());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception e");
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            byte[] b = new ByteArrayInputStream(out.toByteArray()).readAllBytes();
            if (b.length > 0) {
                String s = new String(b);
                System.out.println("data ------> " + s);
            } else {
                System.out.println("Empty");
            }
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public JsonObject getEmployeeYearlyPresent(Map<String, String> jsonRequest, HttpServletRequest request) throws Exception {
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject responseMessage = new JsonObject();
        try {
            DateFormat df1 = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String[] fromMonth = jsonRequest.get("fromMonth").split("-");
            int fMonth = Integer.parseInt(fromMonth[1]);
            int fYear = Integer.parseInt(fromMonth[0]);

            String[] toMonth = jsonRequest.get("toMonth").split("-");
            int tMonth = Integer.parseInt(toMonth[1]);
            int tYear = Integer.parseInt(toMonth[0]);

            Date dateFrom = df1.parse(fMonth + "/01/" + fYear);
            Date dateTo = df1.parse(tMonth + "/01/" + tYear);
            final Locale locale = Locale.US;

            DateFormat df2 = new SimpleDateFormat("yyyy-MM", Locale.US);
            List<String> months = getListMonths(dateFrom, dateTo, locale, df2);

            DateFormat df = new SimpleDateFormat("MMM-yy", Locale.US);
            List<String> months1 = getListMonths(dateFrom, dateTo, locale, df);

            JsonArray empArray = new JsonArray();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "id");
            jsonObject.addProperty("employeeName", "employeeName");
            jsonObject.addProperty("avg", "Avg");
            jsonObject.addProperty("total", "Total");

            JsonArray jsonArray = new JsonArray();
            for (String month : months1) {
                jsonArray.add(month);
            }
            jsonObject.add("months", jsonArray);
            empArray.add(jsonObject);

            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {

                List<Employee> employees = employeeRepository.findByInstituteIdAndStatusOrderByFirstNameAsc(user.getInstitute().getId(),true);

                for (Employee employee : employees) {
                    JsonObject empObj = new JsonObject();

                    empObj.addProperty("id", employee.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                    double totalDaysInYear = 0;
                    JsonArray jsonArray1 = new JsonArray();
                    for (String month : months) {
                        EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), month);

                        double noOfDays = 0;
                        if (employeePayroll != null) {
                            noOfDays = Precision.round(employeePayroll.getTotalDaysInMonth(), 2);
                        }
                        totalDaysInYear = Precision.round(totalDaysInYear + noOfDays, 2);
                        System.out.println("emp ID " + employee.getId() + " month " + month + " noOfDays " + noOfDays);
                        jsonArray1.add(noOfDays);
                    }
                    double avg = Precision.round(totalDaysInYear / months.size(), 2);
                    empObj.add("attendance", jsonArray1);
                    empObj.addProperty("avg", avg);
                    empObj.addProperty("totalDaysInYear", totalDaysInYear);
                    empArray.add(empObj);

                }
            } else {
                Long eId = Long.valueOf(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndInstituteIdAndStatus(eId, user.getInstitute().getId(), true);
                JsonObject empObj = new JsonObject();

                empObj.addProperty("id", employee.getId());
                empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                double totalDaysInYear = 0;
                JsonArray jsonArray1 = new JsonArray();
                for (String month : months) {
                    EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), month);

                    double noOfDays = 0;
                    if (employeePayroll != null) {
                        noOfDays = Precision.round(employeePayroll.getTotalDaysInMonth(), 2);
                    }
                    totalDaysInYear = Precision.round(totalDaysInYear + noOfDays, 2);
                    System.out.println("emp ID " + employee.getId() + " month " + month + " noOfDays " + noOfDays);
                    jsonArray1.add(noOfDays);
                }
                double avg = Precision.round(totalDaysInYear / months.size(), 2);
                empObj.add("attendance", jsonArray1);
                empObj.addProperty("avg", avg);
                empObj.addProperty("totalDaysInYear", totalDaysInYear);
                empArray.add(empObj);
            }
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            responseMessage.add("responseObject", empArray);
        } catch (Exception e) {
            attendanceLogger.error("Failed to load employee year present data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.addProperty("message", "Failed to load data");
        }
        return responseMessage;
    }

    public JsonObject todayEmployeeAttendance(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            LocalDate today = LocalDate.now();
            LocalDate fromDate = null;
            if (!requestParam.get("attendanceDate").equalsIgnoreCase("")) {
                today = LocalDate.parse(requestParam.get("attendanceDate"));
            }
            if (!requestParam.get("fromDate").equalsIgnoreCase("")) {
                fromDate = LocalDate.parse(requestParam.get("fromDate"));
            }
            List<AttendanceView> attendanceViewList = new ArrayList<>();

            String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+user.getInstitute().getId();
            if (requestParam.get("fromDate").equalsIgnoreCase("")) {
                query += " AND attendance_date ='" + today + "'";
            } else if (!requestParam.get("fromDate").equalsIgnoreCase("")) {
                query += " AND attendance_date between '" + fromDate + "' AND '" + today + "'";
            }
            if (!requestParam.get("employeeId").equalsIgnoreCase("")) {
                query += " AND employee_id ='" + requestParam.get("employeeId") + "'";
            }
            if (!requestParam.get("attStatus").equalsIgnoreCase("")) {
                if (!requestParam.get("attStatus").equalsIgnoreCase("pending")) {
                    query += " AND attendance_status ='" + requestParam.get("attStatus") + "'";
                } else {
                    query += " AND attendance_status IS NULL";
                }
            }
            if (!requestParam.get("selectedShift").equalsIgnoreCase("")) {
                query += " AND shift_id ='" + requestParam.get("selectedShift") + "'";
            }
            query += " ORDER BY attendance_date, first_name ASC";
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
            Query q = entityManager.createNativeQuery(query, AttendanceView.class);
            attendanceViewList = q.getResultList();
            System.out.println("attendanceViewList.size() " + attendanceViewList.size());
            for (AttendanceView attendanceView : attendanceViewList) {
                System.out.println("attendanceView.getEmployeeId() " + attendanceView.getEmployeeId() + " attendanceView.getId() " + attendanceView.getId());

                Employee employee = employeeRepository.findById(attendanceView.getEmployeeId()).get();
                System.out.println("employee.getDesignation().getCode() " + employee.getDesignation().getCode());

                String empName = employee.getFirstName();
                if (employee.getLastName() != null)
                    empName = empName + " " + employee.getLastName();

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", attendanceView.getId());
                jsonObject.addProperty("attendanceId", attendanceView.getId());
                jsonObject.addProperty("attendanceDate", attendanceView.getAttendanceDate().toString());
                jsonObject.addProperty("designationCode", employee.getDesignation().getCode().toUpperCase());
                jsonObject.addProperty("employeeId", employee.getId());
                jsonObject.addProperty("employeeName", empName);
                jsonObject.addProperty("employeeWagesType", attendanceView.getFinalDaySalaryType() != null ?
                        attendanceView.getFinalDaySalaryType() : employee.getEmployeeWagesType() != null ?
                        employee.getEmployeeWagesType() : "");
//                if(attendanceView.getAttendanceStatus().equals("approve"))
//                        jsonObject.addProperty("attendanceStatus",true);
//                else

                jsonObject.addProperty("attendanceStatus",attendanceView.getAttendanceStatus() != null ? attendanceView.getAttendanceStatus().equals("approve") ? true : false : false);
//                jsonObject.addProperty("attendanceStatus", attendanceView.getAttendanceStatus() != null ?
//                        attendanceView.getAttendanceStatus() : "pending");
                jsonObject.addProperty("isAttendanceApproved", attendanceView.getIsAttendanceApproved());
                jsonObject.addProperty("checkInTime", attendanceView.getCheckInTime().toString());
                jsonObject.addProperty("checkOutTime", attendanceView.getCheckOutTime() != null ? attendanceView.getCheckOutTime().toString() : "");
                jsonObject.addProperty("totalTime", attendanceView.getTotalTime() != null ? attendanceView.getTotalTime() : "");

                jsonObject.addProperty("lunchTimeInMin", attendanceView.getLunchTime() != null ? Precision.round(attendanceView.getLunchTime(), 2) : 0);
                jsonObject.addProperty("workingHours", attendanceView.getWorkingHours() != null ? Precision.round(attendanceView.getWorkingHours(), 2) : 0);
                Double sumOfAvgTaskPercentage = 0.0;

                double wagesPerHour = (attendanceView.getWagesPerDay() / 8);
                jsonObject.addProperty("wagesPerDay", Precision.round(attendanceView.getWagesPerDay(), 2));
                jsonObject.addProperty("wagesPerHour", Precision.round(wagesPerHour, 2));
                jsonObject.addProperty("wagesHourBasis", Precision.round(attendanceView.getWagesHourBasis(), 2));
//                jsonObject.addProperty("wagesPcsBasis", Precision.round(attendanceView.getWagesPcsBasis(), 2));
                jsonObject.addProperty("breakWages", Precision.round(attendanceView.getBreakWages(), 2));

                if(attendanceView.getCheckOutTime() != null){
                    String[] timeParts = attendanceView.getTotalTime().split(":");
                    int hours = Integer.parseInt(timeParts[0]);
                    int minutes = Integer.parseInt(timeParts[1]);
                    int seconds = Integer.parseInt(timeParts[2]);

                    int totalMinutes = hours * 60 + minutes + (seconds / 60);


                    double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendanceView.getLunchTime(), 2);
                    jsonObject.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
                } else {
                    jsonObject.addProperty("actualWorkingHoursInMinutes", "-");
                }
                LocalDateTime firstTaskStartTime = taskMasterRepository.getInTime(attendanceView.getId());
                System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                LocalDateTime lastTaskEndTime = taskMasterRepository.getOutTime(attendanceView.getId());
                System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                jsonObject.addProperty("firstTaskStartTime", "");
                jsonObject.addProperty("lastTaskEndTime", "");

                if (attendanceView.getTotalTime() != null) {
                    jsonObject.addProperty("firstTaskStartTime", firstTaskStartTime != null ? firstTaskStartTime.toString() : "");
                    jsonObject.addProperty("lastTaskEndTime", lastTaskEndTime != null ? lastTaskEndTime.toString() : "");
                }

                jsonObject.addProperty("remark", attendanceView.getRemark() != null ? attendanceView.getRemark() : null);
                jsonObject.addProperty("adminRemark", attendanceView.getAdminRemark() != null ? attendanceView.getAdminRemark() : null);
                jsonObject.addProperty("empType", employee.getDesignation().getCode().toLowerCase());
                jsonObject.addProperty("finalDaySalaryType", attendanceView.getFinalDaySalaryType() != null ?
                        attendanceView.getFinalDaySalaryType() : "");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                JsonArray breaksArray = new JsonArray();
                List<Object[]> breakList = taskViewRepository.findDataGroupByBreaks(attendanceView.getId(), 2, true);
                for (int j = 0; j < breakList.size(); j++) {
                    Object[] breakObj = breakList.get(j);

                    JsonObject breakObject = new JsonObject();
                    breakObject.addProperty("id", breakObj[0].toString());
                    breakObject.addProperty("breakName", breakObj[1].toString());
                    breakObject.addProperty("actualTime", Precision.round(Double.parseDouble(breakObj[2].toString()), 2));
                    breakObject.addProperty("breakWages", Precision.round(Double.parseDouble(breakObj[4].toString()), 2));

                    List<TaskView> downtimeViewList = taskViewRepository.findByAttendanceIdAndWorkBreakIdAndStatus(attendanceView.getId(), Long.valueOf(breakObj[0].toString()), true);
                    JsonArray downtimeArray = new JsonArray();
                    for (TaskView taskView : downtimeViewList) {
                        JsonObject taskObject = new JsonObject();
                        taskObject.addProperty("taskType", taskView.getTaskType());
                        taskObject.addProperty("taskId", taskView.getId());
                        taskObject.addProperty("remark", taskView.getRemark());
                        taskObject.addProperty("endRemark", taskView.getEndRemark());
                        taskObject.addProperty("adminRemark", taskView.getAdminRemark());
                        taskObject.addProperty("startTime", taskView.getStartTime().toString());
                        taskObject.addProperty("endTime", taskView.getEndTime() != null ? taskView.getEndTime().toString() : "");
                        taskObject.addProperty("workingHour", taskView.getWorkingHour() != null ?
                                Precision.round(taskView.getWorkingHour(), 2) : 0.0);
                        taskObject.addProperty("totalTime", taskView.getTotalTime() != null ? Precision.round(taskView.getTotalTime(), 2) : 0);
                        taskObject.addProperty("breakName", taskView.getBreakName() != null ? taskView.getBreakName() : "");
                        taskObject.addProperty("workDone", taskView.getWorkDone() ? "Working" : "Not Working");
                        taskObject.addProperty("breakWages", taskView.getBreakWages() != null ?
                                Precision.round(taskView.getBreakWages(), 2) : 0.0);

                        downtimeArray.add(taskObject);
                    }
                    breakObject.add("breakList", downtimeArray);
                    breaksArray.add(breakObject);
                }
                jsonObject.add("downtimeData", breaksArray);
//                jsonObject.add("machineData", settingTimeArray);
                jsonArray.add(jsonObject);
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data ===> " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Data inconsistency, please validate data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object calculateTime(Map<String, String> requestParam, HttpServletRequest request) throws ParseException {
        try {
            LocalDateTime fromDate = LocalDateTime.parse(requestParam.get("fromDate"));
            LocalDateTime toDate = LocalDateTime.parse(requestParam.get("toDate"));

//            Attendance attendance = attendanceRepository.findById(Long.valueOf(requestParam.get("id"))).get();
//
//            LocalDateTime fromDate = attendance.getCheckInTime();
//            LocalDateTime toDate = attendance.getCheckOutTime();

            System.out.println("fromDate " + fromDate);
            System.out.println("toDate " + toDate);

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date d1 = df.parse(fromDate.toString());
            Date d2 = df.parse(toDate.toString());

            System.out.println("d2.getTime()  " + d2.getTime());
            System.out.println("d1.getTime()  " + d1.getTime());
            long d = Math.abs(d2.getTime() - d1.getTime());
            System.out.println(" d => " + d);
           /* double hh = ((double) d / (3600 * 1000));
            System.out.println(" hh => " + hh);
            long fullHours = Math.abs(d / (3600 * 1000));
            System.out.println("fullHours => " + fullHours);

            String doubleAsString = String.valueOf(hh);
            int indexOfDecimal = doubleAsString.indexOf(".");
            double decHours = Double.parseDouble(doubleAsString.substring(indexOfDecimal));
            System.out.println("decHours Decimal Part: " + decHours);

            double minutes =  decHours * 60.0;
            System.out.println(" minutes : " + minutes);
            long fullMinutes = (long) Math.abs(minutes);
            System.out.println(" fullMinutes : " + fullMinutes);

            String mdoubleAsString = String.valueOf(minutes);
            int mindexOfDecimal = mdoubleAsString.indexOf(".");
            double decMinutes = Double.parseDouble(mdoubleAsString.substring(mindexOfDecimal));
            System.out.println("decMinutes Decimal Part: " + decMinutes);


            double seconds =  decMinutes * 60.0;
            System.out.println(" seconds : " + seconds);
            long fullSeconds = (long) Math.abs(seconds);
            System.out.printf("\n"+fullHours+" : "+fullMinutes+" : "+ fullSeconds);

*/
            Duration duration = Duration.ofMillis(d);
            long seconds1 = duration.getSeconds();
            long HH = Math.abs(seconds1 / 3600);
            long MM = Math.abs((seconds1 % 3600) / 60);
            long SS = Math.abs(seconds1 % 60);
            String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS);
            System.out.println("String.format(\"%02d:%02d:%02d\", HH, MM, SS) " + String.format("%02d:%02d:%02d", HH, MM, SS));
            System.out.println("result " + LocalTime.parse(timeInHHMMSS));

            /*long mm = Math.abs((d - fullHours * 3600 * 1000) / (60 * 1000));
            System.out.println(" mm => " + mm);
            System.out.printf("\n %02d:%02d \n", hh, mm);

            String totalTime = null;
            if (hh > 23) {
                totalTime = "16:00:00";
            } else {
                totalTime = (hh < 10 ? "0" + hh : hh) + ":" + (mm < 10 ? "0" + mm : mm);
            }
            System.out.println("totalTime " + totalTime);*/

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
        }
        return true;
    }

    public JsonObject addManualAttendance(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Long employeeId = Long.valueOf(requestParam.get("employeeId"));
            Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
            LocalTime timeToCompare = employee.getShift().getThreshold();
            LocalDate attendanceDate = LocalDate.parse(requestParam.get("attendanceDate"));
            Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employeeId, attendanceDate, true);
            if (attendance == null) {

                Double wagesPerDay = utility.getEmployeeWages(employee.getId());
                System.out.println("wagesPerDay =" + wagesPerDay);
                if (wagesPerDay == null) {
                    System.out.println("employee wagesPerDay =" + wagesPerDay);
                    System.out.println("Your salary not updated! Please contact to Admin +++++++++++++++++++++++++++");
                    response.addProperty("message", "Your salary not updated! Please contact to Admin");
                    response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
                } else {
                    attendance = new Attendance();

                    int daysInMonth = getTotalDaysFromYearAndMonth(attendanceDate.getYear(), attendanceDate.getMonthValue());
                    wagesPerDay = employee.getExpectedSalary() / daysInMonth;
                    double wagesPerHour = (wagesPerDay / utility.getTimeInDouble(employee.getShift().getWorkingHours().toString()));


                    attendance.setWagesPerDay(wagesPerDay);
                    attendance.setWagesPerHour(wagesPerHour);
                    attendance.setShift(getEmployeeShift(LocalDate.now(), employee.getId(), employee.getShift().getId()));
                    attendance.setEmployee(employee);
                    attendance.setAttendanceDate(attendanceDate);

                    attendance.setCheckInTime(null);
                    if (!requestParam.get("checkInTime").equalsIgnoreCase("")) {
                        LocalDateTime localDateTime = LocalDateTime.parse(requestParam.get("checkInTime"), myFormatObj);
                        attendance.setCheckInTime(localDateTime);
                        if(localDateTime.toLocalTime().compareTo(timeToCompare) > 0)
                            attendance.setIsLate(true);
                        attendance.setIsManualPunchIn(true);
                    }
                    attendance.setCheckOutTime(null);
                    if (!requestParam.get("checkOutTime").equalsIgnoreCase("")) {
                        LocalDateTime localDateTime = LocalDateTime.parse(requestParam.get("checkOutTime"), myFormatObj);
                        attendance.setCheckOutTime(localDateTime);
                        attendance.setIsManualPunchOut(true);
                    }
                    attendance.setTotalTime(null);
                    double lunchTimeInMin = 0.0;
                    if (!requestParam.get("totalTime").equalsIgnoreCase("")) {
                        LocalTime totalTime = LocalTime.parse(requestParam.get("totalTime"));
                        String[] timeParts = totalTime.toString().split(":");
                        int hours, minutes, totalMinutes = 0;
                        hours = Integer.parseInt(timeParts[0]);
                        minutes = Integer.parseInt(timeParts[1]);
                        totalMinutes = hours * 60 + minutes;
                        lunchTimeInMin = Double.parseDouble(requestParam.get("lunchTimeInMin"));
                        double actualWorkingHoursInMinutes = totalMinutes - lunchTimeInMin;
                        attendance.setTotalTime(LocalTime.parse(requestParam.get("totalTime")));
//                        double actualWorkTime = taskMasterRepository.getSumOfActualWorkTime(attendance.getId());
                        attendance.setActualWorkTime(actualWorkingHoursInMinutes);
                        attendance.setLunchTime(lunchTimeInMin);
                    }



                    LocalTime totalTime = LocalTime.parse("00:00:00");
                    LocalDateTime firstTaskStartTime = null;
                    LocalDateTime lastTaskEndTime = null;

                    int s = 0;
                    int attendanceSec = 0;
                    double totalMinutes = 0;
                    double attendanceMinutes = 0;

                    if (!requestParam.get("checkOutTime").equalsIgnoreCase("") && attendance.getId() != null) {
                        if (attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l3") ||
                                attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l2")) {
                            /*From Task Data*/
                            firstTaskStartTime = taskMasterRepository.getInTime(attendance.getId());
                            System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                            lastTaskEndTime = taskMasterRepository.getOutTime(attendance.getId());
                            System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                            if (firstTaskStartTime != null && lastTaskEndTime != null &&
                                    !firstTaskStartTime.equals("null") && !lastTaskEndTime.equals("null")) {
                                totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendance.setTotalTime(totalTime);
                            } else {
                                firstTaskStartTime = LocalDateTime.now();
                                lastTaskEndTime = firstTaskStartTime;
                                totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendance.setTotalTime(totalTime);
                            }
                        } else {
                            /* From Attendance Data */
                            totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                            System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                            attendance.setTotalTime(totalTime);
                        }

                        LocalTime timeDiff = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                        String[] timeParts = timeDiff.toString().split(":");
                        int hours = Integer.parseInt(timeParts[0]);
                        int minutes = Integer.parseInt(timeParts[1]);
                        double workedHours = utility.getTimeInDouble(hours+":"+minutes);
                        if(workedHours < 5){
                            attendance.setIsHalfDay(true);
                        }

                        // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                        s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                        attendanceSec = Math.abs(s);
                        System.out.println("attendanceSec " + attendanceSec);
                        attendanceMinutes = (attendanceSec / 60.0);
                        totalMinutes = attendanceMinutes - lunchTimeInMin;

                        System.out.println("attendanceMinutes " + attendanceMinutes);
                        System.out.println("totalMinutes " + totalMinutes);

                        double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                        double wagesHourBasis = wagesPerHour * workingHours;
                        System.out.println("workingHours " + workingHours);
                        System.out.println("wagesPerHour " + attendance.getWagesPerHour());
                        System.out.println("wagesHourBasis " + wagesHourBasis);

                        attendance.setWorkingHours(workingHours);
                        attendance.setWagesHourBasis(wagesHourBasis);
                    }

                    if (requestParam.containsKey("adminRemark") && !requestParam.get("adminRemark").equalsIgnoreCase("")) {
                        attendance.setAdminRemark(requestParam.get("adminRemark"));
                    }

                    try {
                        attendance.setStatus(true);
                        attendance.setCreatedAt(LocalDateTime.now());
                        attendance.setCreatedBy(users.getId());
                        attendance.setInstitute(users.getInstitute());
                        attendanceRepository.save(attendance);
                        response.addProperty("message", "Attendance saved successfully");
                        response.addProperty("responseStatus", HttpStatus.OK.value());
                    } catch (Exception e) {
                        System.out.println("Exception " + e.getMessage());
                        e.printStackTrace();
                        response.addProperty("message", "Failed to save attendance");
                        response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                }
            } else {
                response.addProperty("message", "Employee attendance already exists.");
                response.addProperty("responseStatus", HttpStatus.CONFLICT.value());
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            response.addProperty("message", "Failed to save attendance");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object recalculateEmployeeTasksAttendance(HttpServletRequest request) throws ParseException {
        JsonObject response = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            Long employeeId = Long.parseLong(request.getParameter("employeeId"));
            Long attendanceId = Long.parseLong(request.getParameter("attendanceId"));
            Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
            LocalTime shiftTime = employee.getShift().getWorkingHours();
            Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
            System.out.println("shiftMinutes in min " + shiftMinutes);
            Double shiftHours = Double.valueOf((shiftMinutes / 60));
            System.out.println("shiftHours" + shiftHours);
            if (employee != null) {
                Double wagesPerDay = utility.getEmployeeWages(employee.getId());
                System.out.println("wagesPerDay =" + wagesPerDay);
                if (wagesPerDay == null) {
                    System.out.println("employee wagesPerDay =" + wagesPerDay);
                    System.out.println("Your salary not updated! Please contact to Admin +++++++++++++++++++++++++++");
                    response.addProperty("message", "Your salary not updated! Please contact to Admin");
                    response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
                    return response;
                } else {
                    double wagesPerHour = (wagesPerDay / 8.0);
                    double wagesPoint = (wagesPerDay / 100.0);

                    Attendance attendanceExist = attendanceRepository.findByIdAndEmployeeIdAndStatus(attendanceId, employee.getId(), true);
                    if (attendanceExist != null) {
                        attendanceExist.setWagesPerDay(wagesPerDay);
                        attendanceExist.setWagesPerHour(wagesPerHour);
                        attendanceExist.setWagesPoint(wagesPoint);
                        attendanceExist.setStatus(true);

                        List<TaskMaster> taskMasters = taskMasterRepository.findByAttendanceIdAndStatus(attendanceId, true);
                        System.out.println("taskMaster List size=" + taskMasters.size());
                        for (TaskMaster task : taskMasters) {
                            double wagesPerPoint = (wagesPerDay / 100.0);
                            task.setWagesPerDay(wagesPerDay);
                            task.setWagesPerPoint(wagesPerPoint);
                            task.setEmployeeWagesType(employee.getEmployeeWagesType());

                            double time = task.getActualWorkTime() != null ? task.getActualWorkTime() : 0;
                            if (task.getTaskType() == 2) { // 2=>Downtime
                                System.out.println("user time in minutes 2=>Downtime" + time);
                                if (task.getWorkDone()) {
                                    task.setActualWorkTime(time);
                                }
                                /*calculate break hour wages for PCS basis employee which breaks are working only*/
                                task.setBreakWages(taskService.calculateBreakData(time, task, shiftHours));
                            }
//                            if (task.getTaskType() == 3) { // 3=>Setting time
//                                System.out.println("user time in minutes 2=>Setting time" + time);
//                                task.setActualWorkTime(time);
//                                /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                                task.setBreakWages(taskService.calculateBreakData(time, task));
//                            } else if (task.getTaskType() == 1) { // 1=>Task
//                                System.out.println("user time in minutes 2=>Task" + time);
//                                double wagesPointBasis = wagesPerPoint * task.getTotalPoint();
//                                task.setWagesPerPoint(wagesPerPoint);
//                                task.setWagesPointBasis(wagesPointBasis);
//
//                            } else if (task.getTaskType() == 2) { // 2=>Downtime
//                                System.out.println("user time in minutes 2=>Downtime" + time);
//                                if (task.getWorkDone()) {
//                                    task.setActualWorkTime(time);
//                                }
//                                /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                                task.setBreakWages(taskService.calculateBreakData(time, task));
//                            } else if (task.getTaskType() == 4) { // 4=> task without machine
//                                System.out.println("user time in minutes 4=> task without machine " + time);
//                                task.setActualWorkTime(time);
//                            }
                            task.setUpdatedAt(LocalDateTime.now());
                            task.setUpdatedBy(users.getId());
                            taskMasterRepository.save(task);
                        }

                        LocalTime totalTime = LocalTime.parse("00:00:00");
                        LocalDateTime firstTaskStartTime = null;
                        LocalDateTime lastTaskEndTime = null;

                        System.out.println("employee.getDesignation().getCode() --->" + employee.getDesignation().getCode());
                        if (employee.getDesignation().getCode().equalsIgnoreCase("l3") ||
                                employee.getDesignation().getCode().equalsIgnoreCase("l2")) {
                            /*From Task Data*/
                            firstTaskStartTime = taskMasterRepository.getInTime(attendanceExist.getId());
                            System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                            lastTaskEndTime = taskMasterRepository.getOutTime(attendanceExist.getId());
                            System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                            if (firstTaskStartTime != null && !firstTaskStartTime.equals("null") && lastTaskEndTime != null && !lastTaskEndTime.equals("null")) {
                                totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendanceExist.setTotalTime(totalTime);
                            } else {
                                firstTaskStartTime = LocalDateTime.now();
                                lastTaskEndTime = firstTaskStartTime;
                                totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendanceExist.setTotalTime(totalTime);
                            }
                        } else {
                            firstTaskStartTime = attendanceExist.getCheckInTime();
                            lastTaskEndTime = attendanceExist.getCheckOutTime();
                            /* From Attendance Data */
                            totalTime = utility.getDateTimeDiffInTime(attendanceExist.getCheckInTime(), attendanceExist.getCheckOutTime());
                            System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                            attendanceExist.setTotalTime(totalTime);
                        }

                        WorkBreak workBreak = workBreakRepository.findByBreakName(employee.getInstitute().getId());
                        double actualWorkTime = taskMasterRepository.getSumOfActualWorkTime(attendanceExist.getId());
                        attendanceExist.setActualWorkTime(actualWorkTime);
                        double lunchTimeInMin = attendanceExist.getLunchTime();

                        double totalAllBreakMinutes = taskRepository.getSumOfAllBreaksWithNotWorking(
                                attendanceExist.getId(), 0, workBreak.getId());
                        System.out.println("totalAllBreakMinutes >>>>>>>>>>>>>>" + totalAllBreakMinutes);

                        int s = 0;
                        int attendanceSec = 0;
                        double totalMinutes = 0;
                        double attendanceMinutes = 0;
                        if (totalTime != null) {
                            // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                            s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                            attendanceSec = Math.abs(s);
                            System.out.println("attendanceSec " + attendanceSec);
                            attendanceMinutes = ((double) attendanceSec / 60.0);

                            totalMinutes = attendanceMinutes;
                            totalMinutes = attendanceMinutes - lunchTimeInMin - totalAllBreakMinutes;
                            System.out.println("attendanceMinutes " + attendanceMinutes);
                            System.out.println("totalMinutes " + totalMinutes);

                            double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                            double wagesHourBasis = wagesPerHour * workingHours;
                            System.out.println("workingHours " + workingHours);
                            System.out.println("wagesPerHour " + wagesPerHour);
                            System.out.println("wagesHourBasis " + wagesHourBasis);

                            attendanceExist.setWorkingHours(workingHours);
                            attendanceExist.setWagesHourBasis(wagesHourBasis);
                        }

                        try {
                            String taskWagesData = taskRepository.getTaskWagesData(attendanceExist.getId());
                            System.out.println("taskWagesData --> " + taskWagesData);
                            if (taskWagesData != null) {
                                String[] taskSalaryData = taskWagesData.split(",");

                                double wagesPcsBasis = Double.valueOf(taskSalaryData[2]);
                                double breakWages = Double.valueOf(taskSalaryData[6]);
                                double totalPcsWages = wagesPcsBasis + breakWages;

                                attendanceExist.setActualProduction(Double.valueOf(taskSalaryData[0]));
                                attendanceExist.setWagesPointBasis(Double.valueOf(taskSalaryData[1]));
                                attendanceExist.setWagesPcsBasis(wagesPcsBasis);
                                attendanceExist.setBreakWages(breakWages);
                                attendanceExist.setNetPcsWages(totalPcsWages);
                                attendanceExist.setTotalWorkPoint(Double.valueOf(taskSalaryData[3]));
                                attendanceExist.setProdWorkingHours(Double.valueOf(taskSalaryData[4]));
                                attendanceExist.setWorkingHourWithSetting(Double.valueOf(taskSalaryData[5]));
                            }

                            Attendance attendance1 = attendanceRepository.save(attendanceExist);
                            response.addProperty("message", "Revise done");
                            response.addProperty("responseStatus", HttpStatus.OK.value());
                        } catch (Exception e) {
                            attendanceLogger.error("Failed to checkout Exception ===> " + e);
                            e.printStackTrace();
                            System.out.println("Exception " + e.getMessage());
                            response.addProperty("message", "Failed to checkout");
                            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                        }
                    } else {
                        response.addProperty("message", "Please process checkin first");
                        response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                    }
                }
            } else {
                System.out.println("Employee status not true ");
                response.addProperty("message", "Please check employee status");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
            return response;
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data ===> " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Data inconsistency, please validate data");
            response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    public JsonObject approveSalaryAttendance(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            String jsonStr = request.getParameter("list");
            if(jsonStr != null){
                JsonArray approvalList = new JsonParser().parse(jsonStr).getAsJsonArray();
                for (int i = 0; i < approvalList.size(); i++) {
                    JsonObject mObject = approvalList.get(i).getAsJsonObject();
                    Attendance attendance = attendanceRepository.findByIdAndStatus(mObject.get("id").getAsLong(), true);
                    if (attendance != null) {
                        attendance.setFinalDaySalary(mObject.get("wagesPerDay").getAsDouble());
                        attendance.setFinalDaySalaryType(mObject.get("employeeWagesType").getAsString());
                        attendance.setAttendanceStatus(mObject.get("attendanceStatus").getAsString().equals("true") ? "approve" : "");
                        attendance.setIsAttendanceApproved(true);
                        attendance.setUpdatedBy(users.getId());
                        attendance.setUpdatedAt(LocalDateTime.now());
                        attendance.setInstitute(users.getInstitute());
                        attendance.setRemark(null);
                        attendance.setAdminRemark(null);
                        if (mObject.has("remark") && !mObject.get("remark").isJsonNull()) {
                            if(!mObject.get("remark").getAsString().equalsIgnoreCase(""))
                                attendance.setRemark(request.getParameter("remark"));
                        }
                        if (mObject.has("adminRemark") && !mObject.get("adminRemark").isJsonNull()) {
                            if(!mObject.get("adminRemark").getAsString().equalsIgnoreCase(""))
                                attendance.setAdminRemark(request.getParameter("adminRemark"));
                        }
                        try {
                            attendanceRepository.save(attendance);
                            updateSalaryForDay(attendance, users);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("updateSalaryForDay -> Exception ====>>>>>>" + e.getMessage());
                            response.addProperty("message", "Failed to update salary");
                            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                            return response;
                        }
                        response.addProperty("message", "Salary updated successfully");
                        response.addProperty("responseStatus", HttpStatus.OK.value());
                    } else {
                        response.addProperty("message", "Failed to update salary");
                        response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                }
            } else {
                Long attendanceId = Long.valueOf(request.getParameter("attendanceId"));
                Attendance attendance = attendanceRepository.findByIdAndStatus(attendanceId, true);
                if (attendance != null) {
                    if(request.getParameter("employeeWagesType").equals("hr"))
                        attendance.setWagesHourBasis(Double.parseDouble(request.getParameter("wagesHourBasis")));
                    else
                        attendance.setFinalDaySalary(Double.valueOf(request.getParameter("wagesPerDay")));
                    attendance.setFinalDaySalaryType(request.getParameter("employeeWagesType"));
                    attendance.setAttendanceStatus(request.getParameter("attendanceStatus").equals("true") ? "approve" : "");
                    attendance.setIsAttendanceApproved(true);
                    attendance.setUpdatedBy(users.getId());
                    attendance.setUpdatedAt(LocalDateTime.now());
                    attendance.setInstitute(users.getInstitute());
                    attendance.setRemark(null);
                    attendance.setAdminRemark(null);
                    if(request.getParameterMap().containsKey("remark") && request.getParameter("remark") != null){
                        if (!request.getParameter("remark").equalsIgnoreCase("")) {
                            attendance.setRemark(request.getParameter("remark"));
                        }
                    }
                    if(request.getParameterMap().containsKey("adminRemark") && request.getParameter("adminRemark") != null){
                        if (!request.getParameter("adminRemark").equalsIgnoreCase("")) {
                            attendance.setAdminRemark(request.getParameter("adminRemark"));
                        }
                    }

                    try {
                        attendanceRepository.save(attendance);
                        updateSalaryForDay(attendance, users);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("updateSalaryForDay -> Exception ====>>>>>>" + e.getMessage());
                        response.addProperty("message", "Failed to update salary");
                        response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                        return response;
                    }
                    response.addProperty("message", "Salary updated successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                } else {
                    response.addProperty("message", "Failed to update salary");
                    response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            response.addProperty("message", "Failed to update salary");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject deleteAttendance(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long attendanceId = Long.valueOf(request.getParameter("attendanceId"));
            Attendance attendance = attendanceRepository.findByIdAndStatus(attendanceId, true);

            if (attendance != null) {
                List<TaskMaster> taskMasters = taskMasterRepository.findByAttendanceIdAndStatus(attendanceId, true);
                if (taskMasters.size() > 0) {
                    response.addProperty("message", "Delete tasks before going to delete attendance");
                    response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());

                    return response;
                } else {
                    try {
                        attendance.setStatus(false);
                        attendance.setUpdatedBy(users.getId());
                        attendance.setUpdatedAt(LocalDateTime.now());
                        attendance.setInstitute(users.getInstitute());
                        attendance.setAdminRemark("Attendance Deleted by " + users.getUsername() + " - " + users.getId());
                        attendanceRepository.save(attendance);

                        response.addProperty("message", "Attendance deleted successfully");
                        response.addProperty("responseStatus", HttpStatus.OK.value());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exception " + e.getMessage());
                        attendanceLogger.error("Exception => deleteAttendance " + e);

                        response.addProperty("message", "Failed to delete attendance");
                        response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());

                    }
                    return response;
                }
            } else {
                response.addProperty("message", "Attendance not exists");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            attendanceLogger.error("Exception => deleteAttendance " + e);

            response.addProperty("message", "Failed to find attendance");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object recalculateAllEmployeeTasksAttendance(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            LocalDate today = LocalDate.now();
            LocalDate fromDate = null;
            if (!requestParam.get("attendanceDate").equalsIgnoreCase("")) {
                today = LocalDate.parse(requestParam.get("attendanceDate"));
            }
            if (!requestParam.get("fromDate").equalsIgnoreCase("")) {
                fromDate = LocalDate.parse(requestParam.get("fromDate"));
            }
            List<AttendanceView> attendanceViewList = new ArrayList<>();

            String query = "SELECT * from attendance_view WHERE status=1";
            if (requestParam.get("fromDate").equalsIgnoreCase("")) {
                query += " AND attendance_date ='" + today + "'";
            } else if (!requestParam.get("fromDate").equalsIgnoreCase("")) {
                query += " AND attendance_date between '" + fromDate + "' AND '" + today + "'";
            }
           /* if (!requestParam.get("employeeId").equalsIgnoreCase("")) {
                query += " AND employee_id='" + requestParam.get("employeeId")+"'";
            }*/
            System.out.println("recalculateAllEmployeeTasksAttendance >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
            Query q = entityManager.createNativeQuery(query, AttendanceView.class);
            attendanceViewList = q.getResultList();
            System.out.println("attendanceViewList.size() " + attendanceViewList.size());
            for (AttendanceView attendanceView : attendanceViewList) {
                Employee employee = employeeRepository.findByIdAndStatus(attendanceView.getEmployeeId(), true);
                if (employee != null) {
                    Double wagesPerDay = utility.getEmployeeWages(employee.getId());
                    System.out.println("wagesPerDay =" + wagesPerDay);
                    if (wagesPerDay == null) {
                        System.out.println("employee wagesPerDay =" + wagesPerDay);
                        System.out.println(employee.getLastName() + " Your salary not updated! Please contact to Admin +++++++++++++++++++++++++++");
                        response.addProperty("message", employee.getLastName() + " salary not updated! Please contact to Admin");
                        response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
                        return response;
                    } else {
                        double wagesPerHour = (wagesPerDay / 8.0);
                        double wagesPoint = (wagesPerDay / 100.0);

                        Attendance attendanceExist = attendanceRepository.findByIdAndEmployeeIdAndStatus(attendanceView.getId(), employee.getId(), true);
                        if (attendanceExist != null) {
                            attendanceExist.setWagesPerDay(wagesPerDay);
                            attendanceExist.setWagesPerHour(wagesPerHour);
                            attendanceExist.setWagesPoint(wagesPoint);
                            attendanceExist.setStatus(true);

                            System.out.println("attendanceView.getId() <<<<<<<<<<<<<<<<<<<<<<<<<<<<" + attendanceView.getId());
//                            List<TaskMaster> taskMasters = taskMasterRepository.findByAttendanceIdAndStatus(attendanceView.getId(), true);
                            List<TaskMaster> taskMasters = taskMasterRepository.findByAttendanceIdAndWorkDoneAndStatus(attendanceView.getId(), true, true);
                            System.out.println("taskMaster List size=" + taskMasters.size());
                            for (TaskMaster task : taskMasters) {
                                double wagesPerPoint = (wagesPerDay / 100.0);
                                task.setWagesPerDay(wagesPerDay);
                                task.setWagesPerPoint(wagesPerPoint);
                                task.setEmployeeWagesType(employee.getEmployeeWagesType());

                                double time = task.getActualWorkTime() != null ? task.getActualWorkTime() : 0;
                                if (task.getTaskType() == 3) { // 3=>Setting time
                                    System.out.println("user time in minutes 2=>Setting time" + time);
                                    task.setActualWorkTime(time);
                                } else if (task.getTaskType() == 1) { // 1=>Task
                                    System.out.println("user time in minutes 2=>Task" + time);
                                    System.out.println("user time in minutes 2=>Task task.getTotalPoint()" + task.getTotalPoint());
                                    double wagesPointBasis = wagesPerPoint * (task.getTotalPoint() != null ?
                                            task.getTotalPoint() : 0);
                                    task.setWagesPerPoint(wagesPerPoint);
                                    task.setWagesPointBasis(wagesPointBasis);
                                } else if (task.getTaskType() == 2) { // 2=>Downtime
                                    System.out.println("user time in minutes 2=>Downtime" + time);
                                    double hourWages = 0;
                                    if (task.getWorkDone()) {
                                        task.setActualWorkTime(time);
                                        /*calculate break hour wages for PCS basis employee which breaks are working only*/
                                        double breakHours = time / 60.0;
                                        hourWages = breakHours * wagesPerHour;
                                    }
                                    task.setBreakWages(hourWages);
                                } else if (task.getTaskType() == 4) { // 4=> task without machine
                                    System.out.println("user time in minutes 4=> task without machine " + time);
                                    task.setActualWorkTime(time);
                                }
                                task.setUpdatedAt(LocalDateTime.now());
                                task.setUpdatedBy(users.getId());
                                taskMasterRepository.save(task);
                            }

                            LocalTime totalTime = LocalTime.parse("00:00:00");
                            LocalDateTime firstTaskStartTime = null;
                            LocalDateTime lastTaskEndTime = null;

                            System.out.println("employee.getDesignation().getCode() --->" + employee.getDesignation().getCode());
                            if (employee.getDesignation().getCode().equalsIgnoreCase("l3") ||
                                    employee.getDesignation().getCode().equalsIgnoreCase("l2")) {
                                /*From Task Data*/
                                firstTaskStartTime = taskMasterRepository.getInTime(attendanceExist.getId());
                                System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                                lastTaskEndTime = taskMasterRepository.getOutTime(attendanceExist.getId());
                                System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                                if (firstTaskStartTime != null && !firstTaskStartTime.equals("null") && lastTaskEndTime != null && !lastTaskEndTime.equals("null")) {
//                                    LocalDate fDate = firstTaskStartTime.toLocalDate();
//                                    LocalDate tDate = lastTaskEndTime.toLocalDate();
//                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< fDate " + fDate);
//                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< tDate " + tDate);
//
//                                    if (tDate.compareTo(fDate) > 0) {
//                                        String newdtime = fDate + " " + lastTaskEndTime.toLocalTime();
//                                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<< newdtime " + newdtime);
////                                    LocalDateTime newEndTime = LocalDateTime.parse(df2.format(newdtime));
//                                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<< newdtime " + newdtime);
//                                        lastTaskEndTime = LocalDateTime.of(fDate, lastTaskEndTime.toLocalTime());
//                                    }
//                                    System.out.println("<<<<<<<<<<<<<<<<<<<<<<< lastTaskEndTime " + lastTaskEndTime);

                                    totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                    System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                    attendanceExist.setTotalTime(totalTime);
                                } else {
                                    firstTaskStartTime = LocalDateTime.now();
                                    lastTaskEndTime = firstTaskStartTime;
                                    totalTime = utility.getDateTimeDiffInTime(firstTaskStartTime, lastTaskEndTime);
                                    System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                    attendanceExist.setTotalTime(totalTime);
                                }
                            } else {
                                firstTaskStartTime = attendanceExist.getCheckInTime();
                                lastTaskEndTime = attendanceExist.getCheckOutTime();
                                /* From Attendance Data */
                                totalTime = utility.getDateTimeDiffInTime(attendanceExist.getCheckInTime(), attendanceExist.getCheckOutTime());
                                System.out.println("totalTime =>>>>>>>>>>>>>>>>>>>>>>" + totalTime);
                                attendanceExist.setTotalTime(totalTime);
                            }
                            WorkBreak workBreak = workBreakRepository.findByBreakName(employee.getInstitute().getId());
                            TaskMaster taskMaster = null;
                            double lunchTimeInMin = 0;
                            if (workBreak != null) {
                                lunchTimeInMin = taskMasterRepository.getSumOfLunchTime(attendanceExist.getId(), workBreak.getId(), true, false);
                                /*taskMaster = taskMasterRepository.findByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(
                                        attendanceExist.getId(), workBreak.getId(), true, false);
                                if (taskMaster != null) {
                                    lunchTimeInMin = taskMaster.getTotalTime();
                                }*/
                            }

                            double actualWorkTime = taskMasterRepository.getSumOfActualWorkTime(attendanceExist.getId());
                            attendanceExist.setActualWorkTime(actualWorkTime);
                            lunchTimeInMin = attendanceExist.getLunchTime() != null ? attendanceExist.getLunchTime() :
                                    lunchTimeInMin;

                            double totalAllBreakMinutes = taskRepository.getSumOfAllBreaksWithNotWorking(
                                    attendanceExist.getId(), 0, workBreak.getId());
                            System.out.println("totalAllBreakMinutes >>>>>>>>>>>>>>" + totalAllBreakMinutes);

                            int s = 0;
                            int attendanceSec = 0;
                            double totalMinutes = 0;
                            double attendanceMinutes = 0;
                            if (totalTime != null) {
                                // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                                s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                                attendanceSec = Math.abs(s);
                                System.out.println("attendanceSec " + attendanceSec);
                                attendanceMinutes = ((double) attendanceSec / 60.0);

                                totalMinutes = attendanceMinutes;
                                totalMinutes = attendanceMinutes - lunchTimeInMin - totalAllBreakMinutes;
                                System.out.println("attendanceMinutes " + attendanceMinutes);
                                System.out.println("totalMinutes " + totalMinutes);

                                double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                                double wagesHourBasis = wagesPerHour * workingHours;
                                System.out.println("workingHours " + workingHours);
                                System.out.println("wagesPerHour " + wagesPerHour);
                                System.out.println("wagesHourBasis " + wagesHourBasis);

                                attendanceExist.setWorkingHours(workingHours);
                                attendanceExist.setWagesHourBasis(wagesHourBasis);
                                attendanceExist.setLunchTime(lunchTimeInMin);
                            }

                            try {
                                String taskWagesData = taskRepository.getTaskWagesData(attendanceExist.getId());
                                System.out.println("taskWagesData --> " + taskWagesData);
                                if (taskWagesData != null) {
                                    String[] taskSalaryData = taskWagesData.split(",");

                                    double wagesPcsBasis = Double.valueOf(taskSalaryData[2]);
                                    double breakWages = Double.valueOf(taskSalaryData[6]);
                                    double totalPcsWages = wagesPcsBasis + breakWages;

                                    attendanceExist.setActualProduction(Double.valueOf(taskSalaryData[0]));
                                    attendanceExist.setWagesPointBasis(Double.valueOf(taskSalaryData[1]));
                                    attendanceExist.setWagesPcsBasis(wagesPcsBasis);
                                    attendanceExist.setBreakWages(breakWages);
                                    attendanceExist.setNetPcsWages(totalPcsWages);
                                    attendanceExist.setTotalWorkPoint(Double.valueOf(taskSalaryData[3]));
                                    attendanceExist.setProdWorkingHours(Double.valueOf(taskSalaryData[4]));
                                    attendanceExist.setWorkingHourWithSetting(Double.valueOf(taskSalaryData[5]));
                                }

                                if (attendanceExist.getFinalDaySalaryType() != null && attendanceExist.getFinalDaySalaryType().equalsIgnoreCase("hr")) {
                                    attendanceExist.setFinalDaySalary(attendanceExist.getWagesHourBasis());
                                } else if (attendanceExist.getFinalDaySalaryType() != null && attendanceExist.getFinalDaySalaryType().equalsIgnoreCase("point")) {
                                    attendanceExist.setFinalDaySalary(attendanceExist.getWagesPointBasis());
                                } else if (attendanceExist.getFinalDaySalaryType() != null && attendanceExist.getFinalDaySalaryType().equalsIgnoreCase("day")) {
                                    attendanceExist.setFinalDaySalary(attendanceExist.getWagesPerDay());
                                } else if (attendanceExist.getFinalDaySalaryType() != null && attendanceExist.getFinalDaySalaryType().equalsIgnoreCase("pcs")) {
                                    attendanceExist.setFinalDaySalary(attendanceExist.getNetPcsWages());
                                }

                                Attendance attendance1 = attendanceRepository.save(attendanceExist);
                                System.out.println(employee.getLastName() + " Revise done +++++++++++++++++++++++++++");
                            } catch (Exception e) {
                                attendanceLogger.error("Failed to checkout Exception ===> " + e);
                                e.printStackTrace();
                                System.out.println("Exception " + e.getMessage());
                            }
                        } else {
                            System.out.println(employee.getLastName() + " Please process checkin first +++++++++++++++++++++++++++");
                        }
                    }
                } else {
                    System.out.println("Employee status not true ");
                }
            }
            response.addProperty("message", "Salary revise done");
            response.addProperty("responseStatus", HttpStatus.OK.value());
            return response;
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data ===> " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Data inconsistency, please validate data");
            response.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return response;
    }

    public InputStream exportExcelTodayEmployeeAttendance(String fromDate, String attendanceDate, String employeeId, String attStatus, HttpServletRequest request) {

        try {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            List<AttendanceView> attendanceViewList = new ArrayList<>();
            LocalDate today = LocalDate.now();
            if (!attendanceDate.equalsIgnoreCase("na")) {
                today = LocalDate.parse(attendanceDate);
            }
            if (!fromDate.equalsIgnoreCase("na")) {
                fromDate = String.valueOf(LocalDate.parse(fromDate));
            }

            String query = "SELECT * from attendance_view WHERE status=1 AND institute_id="+user.getInstitute().getId();
            if (fromDate.equalsIgnoreCase("na")) {
                query += " AND attendance_date ='" + today + "'";
            } else if (!fromDate.equalsIgnoreCase("na")) {
                query += " AND attendance_date between '" + fromDate + "' AND '" + today + "'";
            }
            if (!employeeId.equalsIgnoreCase("na")) {
                query += " AND employee_id ='" + employeeId + "'";
            }
            if (!attStatus.equalsIgnoreCase("na")) {
                if (!attStatus.equalsIgnoreCase("pending")) {
                    query += " AND attendance_status ='" + attStatus + "'";
                } else {
                    query += " AND attendance_status IS NULL";
                }
            }
            query += " ORDER BY attendance_date, first_name ASC";
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> query " + query);
            Query q = entityManager.createNativeQuery(query, AttendanceView.class);
            attendanceViewList = q.getResultList();
            System.out.println("attendanceViewList.size() " + attendanceViewList.size());

            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            try (Workbook workbook = new XSSFWorkbook()) {
                String[] empSalaryHEADERs = {"DATE", "EMPLOYEE NAME", "DESIG. LEVEL", "IN", "OUT", "FT-TIME", "ET-TIME", "LT(MIN)", "WH(HR)",
                        "PWH(HR)", "PROD(%)", "PWHWS(HR)", "WAGES PER DAY", "WAGES PER HOUR", "HOUR WAGES", "WAGES PER POINT", "TOTAL POINTS", "POINT WAGES",
                        "PCS WAGES", "BREAK WAGES", "NET PCS WAGES", "ATT. STATUS"};
                Sheet sheet = workbook.createSheet("Emp_Attendance_Summary");

                // Header
                Row headerRow = sheet.createRow(0);

                // Define header cell style
                CellStyle headerCellStyle = workbook.createCellStyle();
                headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (int col = 0; col < empSalaryHEADERs.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(empSalaryHEADERs[col]);
                    cell.setCellStyle(headerCellStyle);
                }

                int rowIdx = 1;
                for (AttendanceView attendanceView : attendanceViewList) {
                    Employee employee = employeeRepository.findById(attendanceView.getEmployeeId()).get();
                    System.out.println("employee.getDesignation().getCode() " + employee.getDesignation().getCode());
                    String empName = employee.getFirstName();
                    if (employee.getLastName() != null)
                        empName = empName + " " + employee.getLastName();

                    Row row = sheet.createRow(rowIdx++);
                    try {
                        row.createCell(0).setCellValue(attendanceView.getAttendanceDate().toString());
                        row.createCell(1).setCellValue(empName);
                        row.createCell(2).setCellValue(employee.getDesignation() != null ? employee.getDesignation().getCode().toUpperCase() : "");
                        row.createCell(3).setCellValue(attendanceView.getCheckInTime().format(myFormatObj));
                        row.createCell(4).setCellValue(attendanceView.getCheckOutTime() != null ?
                                attendanceView.getCheckOutTime().format(myFormatObj) : "");

                        LocalDateTime firstTaskStartTime = taskMasterRepository.getInTime(attendanceView.getId());
                        System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                        LocalDateTime lastTaskEndTime = taskMasterRepository.getOutTime(attendanceView.getId());
                        System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);

                        row.createCell(5).setCellValue(firstTaskStartTime != null ? firstTaskStartTime.format(myFormatObj) : "");
                        row.createCell(6).setCellValue(lastTaskEndTime != null ? lastTaskEndTime.format(myFormatObj) : "");
                        row.createCell(7).setCellValue(attendanceView.getLunchTime() != null ? attendanceView.getLunchTime() : 0);
                        row.createCell(8).setCellValue(attendanceView.getWorkingHours() != null ? attendanceView.getWorkingHours() : 0);
//                        row.createCell(9).setCellValue(attendanceView.getProdWorkingHours() != null ? attendanceView.getProdWorkingHours() : 0);

//                        Double productionPercentage = attendanceView.getWorkingHours() != null && attendanceView.getWorkingHours() != 0 ? ((attendanceView.getProdWorkingHours() / attendanceView.getWorkingHours()) * 100) : 0;
//                        row.createCell(10).setCellValue(productionPercentage);

//                        row.createCell(11).setCellValue(attendanceView.getWorkingHourWithSetting() != null ? attendanceView.getWorkingHourWithSetting() : 0);
                        row.createCell(12).setCellValue(attendanceView.getWagesPerDay() != null ? attendanceView.getWagesPerDay() : 0);

                        double wagesPerHour = (attendanceView.getWagesPerDay() / 8);
                        row.createCell(13).setCellValue(wagesPerHour);
                        row.createCell(14).setCellValue(attendanceView.getWagesHourBasis() != null ? attendanceView.getWagesHourBasis() : 0);
//                        row.createCell(15).setCellValue(attendanceView.getWagesPoint() != null ? attendanceView.getWagesPoint() : 0);
//                        row.createCell(16).setCellValue(attendanceView.getTotalWorkPoint() != null ? attendanceView.getTotalWorkPoint() : 0);
//                        row.createCell(17).setCellValue(attendanceView.getWagesPointBasis() != null ? attendanceView.getWagesPointBasis() : 0);
//                        row.createCell(18).setCellValue(attendanceView.getWagesPcsBasis() != null ? attendanceView.getWagesPcsBasis() : 0);
                        row.createCell(19).setCellValue(attendanceView.getBreakWages() != null ? attendanceView.getBreakWages() : 0);
//                        row.createCell(20).setCellValue(attendanceView.getNetPcsWages() != null ? attendanceView.getNetPcsWages() : 0);
                        row.createCell(21).setCellValue(attendanceView.getAttendanceStatus() != null ? attendanceView.getAttendanceStatus().toUpperCase() : "PENDING");

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exception e");
                    }
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                workbook.write(out);

                byte[] b = new ByteArrayInputStream(out.toByteArray()).readAllBytes();
                if (b.length > 0) {
                    String s = new String(b);
                    System.out.println("data ------> " + s);
                } else {
                    System.out.println("Empty");
                }
                return new ByteArrayInputStream(out.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
            }
        } catch (Exception e) {
            attendanceLogger.error("Data inconsistency, please validate data ===> " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public JsonObject getEmployeeSalaryReport(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        Employee emp = new Employee();
        List<Employee> employees = null;
        try {
            LocalDate fDate = LocalDate.parse(request.getParameter("fromDate"));
            int fromYear = fDate.getYear();
            int fromMonth = fDate.getMonthValue();
            String fromDate = fromYear + "-" + fromMonth;
            System.out.println("fromDate: " + fromDate);
            LocalDate tDate = LocalDate.parse(request.getParameter("toDate"));
            int toYear = tDate.getYear();
            int toMonth = tDate.getMonthValue();
            String toDate = toYear + "-" + toMonth;
            System.out.println("toDate: " + toDate);
            if (!request.getParameter("empId").equalsIgnoreCase("null") && !request.getParameter("empId").equalsIgnoreCase("")) {
                emp = employeeRepository.findByIdAndStatus(Long.parseLong(request.getParameter("empId").toString()), true);
            } else {
                employees = employeeRepository.findByStatusOrderByFirstNameAsc(true);
            }
            double pointSum = 0.0;
            double daySum = 0.0;
            double pieceSum = 0.0;
            double hourSum = 0.0;

            JsonArray empArray = new JsonArray();
            JsonObject sumObj = new JsonObject();
            if (employees != null) {
                for (Employee employee : employees) {
                    JsonObject empObj = new JsonObject();

                    empObj.addProperty("id", employee.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                    EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndDateRange(employee.getId(), fromDate, toDate);
                    if (employeePayroll != null) {
//                        empObj.addProperty("pointSalary", employeePayroll.getNetSalaryInPoints() != null ? employeePayroll.getNetSalaryInPoints() : 0);
//                        empObj.addProperty("pieceSalary", employeePayroll.getNetSalaryInPcs() != null ? employeePayroll.getNetSalaryInPcs() : 0);
                        empObj.addProperty("hourSalary", employeePayroll.getNetSalaryInHours() != null ? employeePayroll.getNetSalaryInHours() : 0);
                        empObj.addProperty("daySalary", employeePayroll.getNetSalaryInDays() != null ? employeePayroll.getNetSalaryInDays() : 0);
                        empArray.add(empObj);
//                        pointSum += employeePayroll.getNetSalaryInPoints();
//                        pieceSum += employeePayroll.getNetSalaryInPcs();
                        hourSum += employeePayroll.getNetSalaryInHours();
                        daySum += employeePayroll.getNetSalaryInDays();
                    }
                }
            } else {
                JsonObject empObj = new JsonObject();

                empObj.addProperty("id", emp.getId());
                empObj.addProperty("employeeName", utility.getEmployeeName(emp));

                EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndDateRange(emp.getId(), fromDate, toDate);
                if (employeePayroll != null) {
//                    empObj.addProperty("pointSalary", employeePayroll.getNetSalaryInPoints() != null ? employeePayroll.getNetSalaryInPoints() : 0);
//                    empObj.addProperty("pieceSalary", employeePayroll.getNetSalaryInPcs() != null ? employeePayroll.getNetSalaryInPcs() : 0);
                    empObj.addProperty("hourSalary", employeePayroll.getNetSalaryInHours() != null ? employeePayroll.getNetSalaryInHours() : 0);
                    empObj.addProperty("daySalary", employeePayroll.getNetSalaryInDays() != null ? employeePayroll.getNetSalaryInDays() : 0);
                    empArray.add(empObj);
//                    pointSum += employeePayroll.getNetSalaryInPoints();
//                    pieceSum += employeePayroll.getNetSalaryInPcs();
                    hourSum += employeePayroll.getNetSalaryInHours();
                    daySum += employeePayroll.getNetSalaryInDays();
                }
            }

            sumObj.addProperty("pieceSum", pieceSum);
            sumObj.addProperty("hourSum", hourSum);
            sumObj.addProperty("pointSum", pointSum);
            sumObj.addProperty("daySum", daySum);
            response.add("empSalaryData", empArray);
            response.add("sumData", sumObj);
            response.addProperty("message", "Data Fetched Successfully");
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            response.addProperty("message", "Failed to retrieve data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject getEmployeeEarningReport(HttpServletRequest request) {
        System.out.println("request: " + request.getParameter("empId"));
        JsonObject response = new JsonObject();
        Employee emp = new Employee();
        List<Employee> employees = null;
        try {
            String fromDate = request.getParameter("fromDate");
            String toDate = request.getParameter("toDate");
            if (!request.getParameter("empId").equalsIgnoreCase("null") && !request.getParameter("empId").equalsIgnoreCase("")) {
                emp = employeeRepository.findByIdAndStatus(Long.parseLong(request.getParameter("empId").toString()), true);
            } else {
                employees = employeeRepository.findByStatusOrderByFirstNameAsc(true);
            }

            double pointSum = 0.0;
            double daySum = 0.0;
            double pieceSum = 0.0;
            double hourSum = 0.0;

            JsonArray empArray = new JsonArray();
            JsonObject sumObj = new JsonObject();
            if (employees != null) {
                for (Employee employee : employees) {
                    JsonObject empObj = new JsonObject();

                    empObj.addProperty("id", employee.getId());
                    empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                    List<Attendance> attendanceList = attendanceRepository.findByEmployeeIdAndSelectedDate(employee.getId(), toDate, fromDate, true);
                    if (attendanceList != null) {
                        Double mPoint = 0.0;
                        Double mPiece = 0.0;
                        Double mHour = 0.0;
                        Double mDay = 0.0;

                        for (Attendance attendance : attendanceList) {
                            mPoint += attendance.getWagesPointBasis() != null ? attendance.getWagesPointBasis() : 0;
                            mPiece += attendance.getWagesPcsBasis() != null ? attendance.getWagesPcsBasis() : 0;
                            mHour += attendance.getWagesHourBasis() != null ? attendance.getWagesHourBasis() : 0;
                            mDay += attendance.getWagesPerDay() != null ? attendance.getWagesPerDay() : 0;
                        }
                        empObj.addProperty("pointSalary", mPoint);
                        empObj.addProperty("pieceSalary", mPiece);
                        empObj.addProperty("hourSalary", mHour);
                        empObj.addProperty("daySalary", mDay);
                        empArray.add(empObj);
                        pointSum += mPoint;
                        pieceSum += mPiece;
                        hourSum += mHour;
                        daySum += mDay;
                    }
                }
            } else {
                JsonObject empObj = new JsonObject();

                empObj.addProperty("id", emp.getId());
                empObj.addProperty("employeeName", utility.getEmployeeName(emp));

                List<Attendance> attendanceList = attendanceRepository.findByEmployeeIdAndSelectedDate(emp.getId(), toDate, fromDate, true);
                if (attendanceList != null) {
                    Double mPoint = 0.0;
                    Double mPiece = 0.0;
                    Double mHour = 0.0;
                    Double mDay = 0.0;

                    for (Attendance attendance : attendanceList) {
                        mPoint += attendance.getWagesPointBasis() != null ? attendance.getWagesPointBasis() : 0;
                        mPiece += attendance.getWagesPcsBasis() != null ? attendance.getWagesPcsBasis() : 0;
                        mHour += attendance.getWagesHourBasis() != null ? attendance.getWagesHourBasis() : 0;
                        mDay += attendance.getWagesPerDay() != null ? attendance.getWagesPerDay() : 0;
                    }
                    empObj.addProperty("pointSalary", mPoint);
                    empObj.addProperty("pieceSalary", mPiece);
                    empObj.addProperty("hourSalary", mHour);
                    empObj.addProperty("daySalary", mDay);
                    empArray.add(empObj);
                    pointSum += mPoint;
                    pieceSum += mPiece;
                    hourSum += mHour;
                    daySum += mDay;
                }
            }
            sumObj.addProperty("pieceSum", pieceSum);
            sumObj.addProperty("hourSum", hourSum);
            sumObj.addProperty("pointSum", pointSum);
            sumObj.addProperty("daySum", daySum);
            response.add("empEarningData", empArray);
            response.add("sumData", sumObj);
            response.addProperty("message", "Data Fetched Successfully");
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            response.addProperty("message", "Failed to retrieve data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public JsonObject getEmployeeYearlyAbsent(Map<String, String> jsonRequest, HttpServletRequest request) throws Exception {
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject responseMessage = new JsonObject();
        try {
            DateFormat df1 = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            String[] fromMonth = jsonRequest.get("fromMonth").split("-");
            int fMonth = Integer.parseInt(fromMonth[1]);
            int fYear = Integer.parseInt(fromMonth[0]);

            String[] toMonth = jsonRequest.get("toMonth").split("-");
            int tMonth = Integer.parseInt(toMonth[1]);
            int tYear = Integer.parseInt(toMonth[0]);

            Date dateFrom = df1.parse(fMonth + "/01/" + fYear);
            Date dateTo = df1.parse(tMonth + "/01/" + tYear);
            final Locale locale = Locale.US;

            DateFormat df2 = new SimpleDateFormat("yyyy-MM", Locale.US);
            List<String> months = getListMonths(dateFrom, dateTo, locale, df2);

            DateFormat df = new SimpleDateFormat("MMM-yy", Locale.US);
            List<String> months1 = getListMonths(dateFrom, dateTo, locale, df);

            JsonArray jsonArray = new JsonArray();
            for (String month : months1) {
                jsonArray.add(month);
            }


            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {
                jsonArray = new JsonArray();

                JsonObject jsonObject = new JsonObject();
                JsonArray absentCntArray = new JsonArray();
                List<Employee> employees = employeeRepository.findByInstituteIdAndStatus(user.getInstitute().getId(), true);
                for (Employee employee : employees) {
                    jsonArray.add(utility.getEmployeeName(employee));

                    double noOfDaysAbsent = 0;
                    for (String month : months) {
                        EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), month);
                        String[] yearMonth = month.split("-");
                        int mMonth = Integer.parseInt(yearMonth[1]);
                        int mYear = Integer.parseInt(yearMonth[0]);
                        YearMonth yearMonthObject = YearMonth.of(mYear, mMonth);
                        int daysInMonth = yearMonthObject.lengthOfMonth();

                        if (employeePayroll != null) {
                            noOfDaysAbsent = daysInMonth - Precision.round(employeePayroll.getTotalDaysInMonth(), 2);
                        }
                        System.out.println("emp ID " + employee.getId() + " month " + month + " noOfDays " + noOfDaysAbsent + " daysInMonth" + daysInMonth);
                        noOfDaysAbsent = +noOfDaysAbsent;
                    }
                    absentCntArray.add(noOfDaysAbsent);
                }
                jsonObject.add("months", jsonArray);
                jsonObject.add("absent", absentCntArray);
                jsonObject.addProperty("employeeName", "All Employees");
                responseMessage.add("responseObject", jsonObject);
            } else {

                Long eId = Long.valueOf(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndInstituteIdAndStatus(eId, user.getInstitute().getId(), true);
                JsonObject empObj = new JsonObject();

                empObj.addProperty("id", employee.getId());
                empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                double totalDaysInYear = 0;
                JsonArray jsonArray1 = new JsonArray();
                for (String month : months) {
                    EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), month);
                    String[] yearMonth = month.split("-");
                    int mMonth = Integer.parseInt(yearMonth[1]);
                    int mYear = Integer.parseInt(yearMonth[0]);
                    YearMonth yearMonthObject = YearMonth.of(mYear, mMonth);
                    int daysInMonth = yearMonthObject.lengthOfMonth();
                    double noOfDaysAbsent = 0;
                    if (employeePayroll != null) {
                        noOfDaysAbsent = daysInMonth - Precision.round(employeePayroll.getTotalDaysInMonth(), 2);
                    }
                    totalDaysInYear = Precision.round(totalDaysInYear + noOfDaysAbsent, 2);
                    System.out.println("emp ID " + employee.getId() + " month " + month + " noOfDays " + noOfDaysAbsent + " daysInMonth" + daysInMonth);
                    jsonArray1.add(noOfDaysAbsent);
                }
                double avg = Precision.round(totalDaysInYear / months.size(), 2);
                empObj.add("absent", jsonArray1);
                empObj.addProperty("avg", avg);
                empObj.addProperty("totalDaysInYear", totalDaysInYear);
                empObj.add("months", jsonArray);
                responseMessage.add("responseObject", empObj);
            }
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            attendanceLogger.error("Failed to load employee year present data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.addProperty("message", "Failed to load data");
        }
        return responseMessage;
    }


    public JsonObject getManualAttendanceReport(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray array = new JsonArray();
        String monthValue = null;
        String yearValue = null;
        try{
            if (!jsonRequest.get("fromMonth").equals("")) {
                String[] fromMonth = jsonRequest.get("fromMonth").split("-");
                int userMonth = Integer.parseInt(fromMonth[1]);
                int userYear = Integer.parseInt(fromMonth[0]);
                monthValue = userMonth < 10 ? "0"+userMonth : String.valueOf(userMonth);
                yearValue = String.valueOf(userYear);
            } else {
                yearValue = String.valueOf(LocalDate.now().getYear());
                monthValue = String.valueOf(LocalDate.now().getMonthValue());
            }
            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {
                List<Attendance> attendanceList = attendanceRepository.getManualAttendanceListOfAll(yearValue.toString(), monthValue);
                for (Attendance attendance : attendanceList) {
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("id", attendance.getId());
                    jsonObj.addProperty("attendanceId", attendance.getId());
                    jsonObj.addProperty("employeeName",attendance.getEmployee().getFirstName()+" "+attendance.getEmployee().getLastName());
                    jsonObj.addProperty("attendanceDate", attendance.getAttendanceDate().toString());
                    jsonObj.addProperty("attendanceStatus", attendance.getAttendanceStatus() != null ?
                            attendance.getAttendanceStatus() : "pending");
                    jsonObj.addProperty("checkInTime", attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    jsonObj.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
//                    jsonObj.addProperty("totalTime", attendance.getTotalTime() != null ? attendance.getTotalTime().toString() : "");

                    if(attendance.getCheckOutTime() != null){
//                String[] timeParts = attendance.getTotalTime().toString().split(":");
//                        LocalTime time = LocalTime.parse(attendance.getTotalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
//                        String[] timeParts = time.toString().split(":");
//                        int hours, minutes, seconds, totalMinutes = 0;
//                        hours = Integer.parseInt(timeParts[0]);
//                        minutes = Integer.parseInt(timeParts[1]);
//                        if(timeParts.length > 2) {
//                            seconds = Integer.parseInt(timeParts[2]);
//                            totalMinutes = hours * 60 + minutes + (seconds / 60);
//
//                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendance.getLunchTime(), 2);
//                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
//                        } else {
//                            totalMinutes = hours * 60 + minutes;
//
//                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendance.getLunchTime(), 2);
//                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
//                        }
                        LocalTime totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                        jsonObj.addProperty("actualWorkingHoursInMinutes", totalTime.toString());
                    } else {
                        jsonObj.addProperty("actualWorkingHoursInMinutes", "0.0");
                    }
                    array.add(jsonObj);
                }
            }else {
                Long employeeId = Long.parseLong(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
                List<Attendance> attendanceList = attendanceRepository.getManualAttendanceList(employee.getId(), yearValue, monthValue);
                for (Attendance attendance : attendanceList) {
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("id", attendance.getId());
                    jsonObj.addProperty("attendanceId", attendance.getId());

                    jsonObj.addProperty("attendanceDate", attendance.getAttendanceDate().toString());
                    jsonObj.addProperty("attendanceStatus", attendance.getAttendanceStatus() != null ?
                            attendance.getAttendanceStatus() : "pending");
                    jsonObj.addProperty("checkInTime", attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    jsonObj.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
//                    jsonObj.addProperty("totalTime", attendance.getTotalTime() != null ? attendance.getTotalTime().toString() : "");

                    if(attendance.getCheckOutTime() != null){
//                String[] timeParts = attendance.getTotalTime().toString().split(":");
//                        LocalTime time = LocalTime.parse(attendance.getTotalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
//                        String[] timeParts = time.toString().split(":");
//                        int hours, minutes, seconds, totalMinutes = 0;
//                        hours = Integer.parseInt(timeParts[0]);
//                        minutes = Integer.parseInt(timeParts[1]);
//                        if(timeParts.length > 2) {
//                            seconds = Integer.parseInt(timeParts[2]);
//                            totalMinutes = hours * 60 + minutes + (seconds / 60);
//
//                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendance.getLunchTime(), 2);
//                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
//                        } else {
//                            totalMinutes = hours * 60 + minutes;
//
//                            double actualWorkingHoursInMinutes = totalMinutes - Precision.round(attendance.getLunchTime(), 2);
//                            jsonObj.addProperty("actualWorkingHoursInMinutes", actualWorkingHoursInMinutes);
//                        }
                        LocalTime totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                        jsonObj.addProperty("actualWorkingHoursInMinutes", totalTime.toString());
                    } else {
                        jsonObj.addProperty("actualWorkingHoursInMinutes", "0.0");
                    }
                    array.add(jsonObj);
                }
            }

            response.add("response", array);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e){
            System.out.println(e);
        }
        return response;
    }

    public JsonObject getLateAttendanceReport(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();

        JsonArray array = new JsonArray();
        String monthValue = null;
        String yearValue = null;

        try{
            if (!jsonRequest.get("fromMonth").equals("")) {
                String[] fromMonth = jsonRequest.get("fromMonth").split("-");
                int userMonth = Integer.parseInt(fromMonth[1]);
                int userYear = Integer.parseInt(fromMonth[0]);
                monthValue = userMonth < 10 ? "0"+userMonth : String.valueOf(userMonth);
                yearValue = String.valueOf(userYear);
            } else {
                yearValue = String.valueOf(LocalDate.now().getYear());
                monthValue = String.valueOf(LocalDate.now().getMonthValue());
            }
            if (jsonRequest.get("employeeId").equalsIgnoreCase("all")) {
                List<Attendance> attendanceList = attendanceRepository.getLateAttendanceListOfAll(yearValue.toString(), monthValue);
                for (Attendance attendance : attendanceList) {
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("id", attendance.getId());
                    jsonObj.addProperty("attendanceId", attendance.getId());
                    jsonObj.addProperty("employeeName", attendance.getEmployee().getFirstName()+" "+attendance.getEmployee().getLastName());
                    jsonObj.addProperty("attendanceDate", attendance.getAttendanceDate().toString());
                    jsonObj.addProperty("attendanceStatus", attendance.getAttendanceStatus() != null ?
                            attendance.getAttendanceStatus() : "pending");
                    jsonObj.addProperty("checkInTime", attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    jsonObj.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
                    LocalTime timeDiff = utility.getTimeDiffFromTimes(attendance.getEmployee().getShift().getThreshold(), LocalTime.parse(attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                    jsonObj.addProperty("lateTime", timeDiff.toString());
//                    jsonObj.addProperty("totalTime", attendance.getTotalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    LocalTime totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                    jsonObj.addProperty("totalTime", totalTime.toString());
                    array.add(jsonObj);
                }
            }else {
                Long employeeId = Long.parseLong(jsonRequest.get("employeeId"));
                Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);
                List<Attendance> attendanceList = attendanceRepository.getLateAttendanceList(employee.getId(), yearValue, monthValue);
                for (Attendance attendance : attendanceList) {
                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("id", attendance.getId());
                    jsonObj.addProperty("attendanceId", attendance.getId());
                    jsonObj.addProperty("attendanceDate", attendance.getAttendanceDate().toString());
                    jsonObj.addProperty("attendanceStatus", attendance.getAttendanceStatus() != null ?
                            attendance.getAttendanceStatus() : "pending");
                    jsonObj.addProperty("checkInTime", attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
                    jsonObj.addProperty("checkOutTime", attendance.getCheckOutTime() != null ? attendance.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : "");
                    LocalTime timeDiff = utility.getTimeDiffFromTimes(employee.getShift().getThreshold(), LocalTime.parse(attendance.getCheckInTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
                    jsonObj.addProperty("lateTime", timeDiff.toString());
//                    jsonObj.addProperty("totalTime", attendance.getTotalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                    LocalTime totalTime = utility.getDateTimeDiffInTime(attendance.getCheckInTime(), attendance.getCheckOutTime());
                    jsonObj.addProperty("totalTime", totalTime.toString());
                    array.add(jsonObj);
                }
            }
            response.add("response", array);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e){
            System.out.println(e);
        }
        return  response;
    }


    //Automatic Mark Late and Half day
    public JsonObject setLateAndHalfDayOfAllEmployees(HttpServletRequest request) {
        Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            System.out.println("jsonRequest " + request.getParameter("month"));
            String[] currentMonth = request.getParameter("month").split("-");
            String userMonth = currentMonth[1];
            String userYear = currentMonth[0];
            String userDay = "01";

            int cYear = LocalDate.now().getYear();
            int uYear = Integer.parseInt(userYear);
            if(cYear != uYear){
                response.addProperty("message", "Invalid Year");
                response.addProperty("responseStatus", HttpStatus.NOT_ACCEPTABLE.value());
                return response;
            }

            String newUserDate = userYear + "-" + userMonth + "-" + userDay;
            LocalDate currentDate = LocalDate.parse(newUserDate);
            LocalDate firstDateOfMonth = currentDate.withDayOfMonth(1);
            LocalDate lastDateOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth()).plusDays(1);

            List<LocalDate> localDates = firstDateOfMonth.datesUntil(lastDateOfMonth).collect(Collectors.toList());
            List<Employee> employees = employeeRepository.findByInstituteIdAndStatusOrderByFirstNameAsc(user.getInstitute().getId(), true);
            for (Employee employee1 : employees) {
                double workedHours = 0.0;
                LocalTime timeToCompare = employee1.getShift().getThreshold();
                for (LocalDate localDate : localDates) {
                    Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee1.getId(), localDate, true);
                    int hours = 0;
                    int minutes = 0;
                    if (attendance != null) {
                        LocalDateTime checkInTime = attendance.getCheckInTime();
                        LocalDateTime checkOutTime = attendance.getCheckOutTime();
                        if(checkOutTime != null){
                            LocalTime timeDiff = utility.getDateTimeDiffInTime(checkInTime, checkOutTime);
                            String[] timeParts = timeDiff.toString().split(":");
                            hours = Integer.parseInt(timeParts[0]);
                            minutes = Integer.parseInt(timeParts[1]);
                            workedHours = utility.getTimeInDouble(hours+":"+minutes);
                            if(checkInTime.toLocalTime().compareTo(timeToCompare) > 0)
                                attendance.setIsLate(true);
                            if(workedHours < 5)
                                attendance.setIsHalfDay(true);
                            attendanceRepository.save(attendance);
                        }
                    }
                }
            }
            response.addProperty("message", "Late and half days marked successfully");
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            response.addProperty("message", "Failed to update salary");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
