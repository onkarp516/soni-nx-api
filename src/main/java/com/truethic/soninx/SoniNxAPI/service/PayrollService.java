package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.EmployeePayroll;
import com.truethic.soninx.SoniNxAPI.model.Payhead;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import org.apache.commons.math3.util.Precision;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PayrollService {
    private static final Logger payrollLogger = LoggerFactory.getLogger(PayrollService.class);

    static String[] empSalaryHEADERs = {"Employee Name", "Company", "Days", "WH(HR)","Days Wages", "Hour Wages", "Point Wages", "Pcs Wages","Net Salary", "BASIC(AMT/%)", "Sp. Allowance",
            "P/F(AMT/%)", "ESI(AMT/%)", "Total Ded.", "Payable", "Advance", "Incentive", "Net Payable"};
    static String empSalarySHEET = "Emp_Salary_Sheet";

    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private EmployeeLeaveRepository employeeLeaveRepository;
    @Autowired
    private PayheadRepository payheadRepository;
    @Autowired
    private MasterPayheadRepository masterPayheadRepository;
    @Autowired
    private EmployeePayrollRepository employeePayrollRepository;
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private TaskMasterRepository taskMasterRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ShiftAssignRepository shiftAssignRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AllowanceRepository allowanceRepository;
    @Autowired
    private DeductionRepository deductionRepository;
    @Autowired
    private AdvancePaymentRepository advancePaymentRepository;
    @Autowired
    private Utility utility;

    public JsonObject getCurrentMonthPayslip(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();

        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            System.out.println("employee.getId() " + employee.getId());

            String yearMonth = null;
            if (!jsonRequest.get("currentMonth").equals("")) {
                System.out.println("jsonRequest " + jsonRequest.get("currentMonth"));
                String[] currentMonth = jsonRequest.get("currentMonth").split("-");
                String userMonth = currentMonth[0];
                String userYear = currentMonth[1];
                String monthValue = Integer.parseInt(userMonth) < 10 ? "0"+userMonth : userMonth;
                yearMonth = userYear + "-" + userMonth;
            } else {
                String monthValue = LocalDate.now().getMonthValue() < 10 ? "0"+LocalDate.now().getMonthValue() : String.valueOf(LocalDate.now().getMonthValue());
                yearMonth = LocalDate.now().getYear() + "-" + monthValue;
            }
            System.out.println("yearMonth " + yearMonth);
            EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(),
                    yearMonth);
            if (employeePayroll != null) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("employeeId", employeePayroll.getEmployee().getId());
                jsonObject.addProperty("employeeName", utility.getEmployeeName(employeePayroll.getEmployee()));
                jsonObject.addProperty("netSalary", employeePayroll.getNetSalary());
                jsonObject.addProperty("pfPercentage", employeePayroll.getPfPer());
                jsonObject.addProperty("pfAmount", employeePayroll.getPf());
                jsonObject.addProperty("esiPercentage", employeePayroll.getEsiPer());
                jsonObject.addProperty("esiAmount", employeePayroll.getEsi());
                jsonObject.addProperty("profTax", employeePayroll.getPfTax());
                jsonObject.addProperty("totalDeduction", employeePayroll.getTotalDeduction());
                jsonObject.addProperty("payableAmount", employeePayroll.getPayableAmount());
                jsonObject.addProperty("advance", employeePayroll.getAdvance());
                jsonObject.addProperty("incentive", employeePayroll.getIncentive());
                jsonObject.addProperty("netPayableAmount", employeePayroll.getNetPayableAmount());
                jsonObject.addProperty("totalHoursInMonth", employeePayroll.getTotalHoursInMonth());

                jsonObject.addProperty("employeeHavePf", employeePayroll.getEmployee().getEmployeeHavePf());
                jsonObject.addProperty("employeeHaveEsi", employeePayroll.getEmployee().getEmployeeHaveEsi());

                jsonObject.addProperty("netSalaryInHours", "NA");
                jsonObject.addProperty("netSalaryInPoints", "NA");
                jsonObject.addProperty("netSalaryInDays", "NA");
                jsonObject.addProperty("netSalaryInPcs", "NA");
                jsonObject.addProperty("totalAmount",employeePayroll.getMonthlyPay());
                jsonObject.addProperty("basic",employeePayroll.getBasic());
                jsonObject.addProperty("grossTotal",employeePayroll.getGrosstotal());
                jsonObject.addProperty("specialAllowance",employeePayroll.getSpecialAllowance());
                jsonObject.addProperty("perDaySalary",Precision.round(employeePayroll.getPerDaySalary(),2));
                jsonObject.addProperty("totalDaysPresent",(employeePayroll.getPresentDays()+employeePayroll.getExtraDays()+employeePayroll.getHalfDays()));

                if (employee.getWagesOptions() != null) {
                    String[] wagesArr = employee.getWagesOptions().split(",");
                    if (wagesArr.length > 0) {
                        if (Arrays.asList(wagesArr).contains("hr")) {
                            jsonObject.addProperty("netSalaryInHours", employeePayroll.getNetSalaryInHours());
                        }
                        if (Arrays.asList(wagesArr).contains("day")) {
                            jsonObject.addProperty("netSalaryInDays", employeePayroll.getNetSalaryInDays());
                        }
//                        if (Arrays.asList(wagesArr).contains("point")) {
//                            jsonObject.addProperty("netSalaryInPoints", employeePayroll.getNetSalaryInPoints());
//                        }
//                        if (Arrays.asList(wagesArr).contains("pcs")) {
//                            jsonObject.addProperty("netSalaryInPcs", employeePayroll.getNetSalaryInPcs());
//                        }
                    }
                }

                responseMessage.add("response", jsonObject);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                responseMessage.addProperty("response", "NA");
                responseMessage.addProperty("message", "Data not found.");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            payrollLogger.error("getCurrentMonthPayslip Exception ===>" + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data.");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return responseMessage;
    }


    public JsonObject getEmpSalaryslip(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        Long empId = Long.parseLong(jsonRequest.get("employeeId"));
        try {
            Employee employee = employeeRepository.findByIdAndStatus(empId, true);
            if(employee != null){
                String yearMonth = null;
                String monthValue = null;
                String yearValue = null;
                if (!jsonRequest.get("fromMonth").equals("")) {
                    String[] fromMonth = jsonRequest.get("fromMonth").split("-");
                    int userMonth = Integer.parseInt(fromMonth[1]);
                    int userYear = Integer.parseInt(fromMonth[0]);
                    monthValue = userMonth < 10 ? "0"+userMonth : String.valueOf(userMonth);
                    yearValue = String.valueOf(userYear);
                    yearMonth = yearValue+"-"+monthValue;
                } else {
                    yearValue = String.valueOf(LocalDate.now().getYear());
                    monthValue = String.valueOf(LocalDate.now().getMonthValue());
                    yearMonth = yearValue+"-"+monthValue;
                }
                System.out.println("yearMonth" + yearMonth);
                EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(),
                        yearMonth);
                if (employeePayroll != null) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("employeeId", employeePayroll.getEmployee().getId());
                    jsonObject.addProperty("employeeName", utility.getEmployeeName(employeePayroll.getEmployee()));
                    jsonObject.addProperty("designation", employeePayroll.getEmployee().getDesignation().getName());
                    jsonObject.addProperty("mobileNo", employeePayroll.getEmployee().getMobileNumber());
                    jsonObject.addProperty("address", employeePayroll.getEmployee().getAddress());
                    jsonObject.addProperty("basic", Precision.round(employeePayroll.getBasic(), 2));
                    jsonObject.addProperty("specialAllowance", Precision.round(employeePayroll.getSpecialAllowance(),2));
                    jsonObject.addProperty("netSalary", Precision.round(employeePayroll.getNetSalary(), 2));
                    jsonObject.addProperty("pfPercentage", Precision.round(employeePayroll.getPfPer(), 2));
                    jsonObject.addProperty("pfAmount", Precision.round(employeePayroll.getPf(), 2));
                    jsonObject.addProperty("esiPercentage", Precision.round(employeePayroll.getEsiPer(), 2));
                    jsonObject.addProperty("esiAmount", Precision.round(employeePayroll.getEsi(), 2));
                    jsonObject.addProperty("profTax", Precision.round(employeePayroll.getPfTax(), 2));
                    jsonObject.addProperty("totalDeduction", Precision.round(employeePayroll.getTotalDeduction(), 2));
                    jsonObject.addProperty("payableAmount", Precision.round(employeePayroll.getPayableAmount(), 2));
                    jsonObject.addProperty("advance", Precision.round(employeePayroll.getAdvance(), 2));
                    jsonObject.addProperty("incentive", Precision.round(employeePayroll.getIncentive(), 2));
                    jsonObject.addProperty("netPayableAmount", Precision.round(employeePayroll.getNetPayableAmount(), 2));
                    jsonObject.addProperty("noDaysPresent", Precision.round(employeePayroll.getNoDaysPresent(), 2));
                    jsonObject.addProperty("totalHoursInMonth", Precision.round(employeePayroll.getTotalHoursInMonth(), 2));
                    jsonObject.addProperty("totalDaysInMonth",employeePayroll.getTotalDays());
                    jsonObject.addProperty("perDaySalary",employeePayroll.getPerDaySalary());
                    jsonObject.addProperty("neySalary",employeePayroll.getNetSalary());
                    jsonObject.addProperty("monthlyPay",employeePayroll.getMonthlyPay());
                    jsonObject.addProperty("totalDays",employeePayroll.getDaysInMonth());
                    jsonObject.addProperty("grossTotal",employeePayroll.getGrosstotal());
                    jsonObject.addProperty("leaveDays",employeePayroll.getLeaveDays());
                    jsonObject.addProperty("presentDays",employeePayroll.getPresentDays());
                    jsonObject.addProperty("totalDaysOfEmployee",Precision.round(employeePayroll.getTotalDaysOfEmployee(),2));
                    jsonObject.addProperty("extraDays",employeePayroll.getExtraDays());
                    jsonObject.addProperty("extraDaysSalary",employeePayroll.getExtraDaysSalary());
                    jsonObject.addProperty("extraHalfDays",employeePayroll.getExtraHalfDays());
                    jsonObject.addProperty("extraHalfDaysSalary",employeePayroll.getExtraHalfDaysSalary());
                    jsonObject.addProperty("halfDays",employeePayroll.getHalfDays());
                    jsonObject.addProperty("halfDaysSalary",employeePayroll.getHalfDaysSalary());
                    jsonObject.addProperty("daysToBeDeducted",employeePayroll.getDaysToBeDeducted());
                    jsonObject.addProperty("lateCount",employeePayroll.getLateCount());
                    jsonObject.addProperty("latePunchDeductionAmount",employeePayroll.getLatePunchDeductionAmt());
                    jsonObject.addProperty("daysToBeDeducted",employeePayroll.getDaysToBeDeducted());
                    jsonObject.addProperty("deductionType",employeePayroll.getDeductionType());
                    jsonObject.addProperty("hoursToBeDeducted",employeePayroll.getHoursToBeDeducted());
                    jsonObject.addProperty("isDayDeduction",employeePayroll.getIsDayDeduction());

                    if (employee.getWagesOptions() != null) {
                        String[] wagesArr = employee.getWagesOptions().split(",");
                        if (wagesArr.length > 0) {
                            jsonObject.addProperty("netSalaryInHours", Precision.round(employeePayroll.getNetSalaryInHours(), 2));
                            jsonObject.addProperty("netSalaryInDays", Precision.round(employeePayroll.getNetSalaryInDays(), 2));
                        }
                    }

                    responseMessage.add("response", jsonObject);
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } else {
                    responseMessage.addProperty("response", "NA");
                    responseMessage.addProperty("message", "Data not found.");
                    responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                }
            }
            else{
                responseMessage.addProperty("response", "NA");
                responseMessage.addProperty("message", "Employee not found.");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            payrollLogger.error("getCurrentMonthPayslip Exception ===>" + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data.");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return responseMessage;
    }


    public JsonObject getDashboardStatistics(HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request .getHeader("Authorization").substring(7));
        LocalDate todayDate = LocalDate.now();
        JsonObject responseMessage = new JsonObject();
        JsonObject result = new JsonObject();
        try {
            String shiftId = request.getParameter("shiftId");
            String attendanceDate = request.getParameter("attendanceDate");

            if(!attendanceDate.equalsIgnoreCase(""))
                todayDate = LocalDate.parse(attendanceDate);

            if (shiftId.equalsIgnoreCase("")) {
                int totalEmployees = employeeRepository.getEmployeeCountOfInstitute(users.getInstitute().getId(), true);
                int presentEmployees = attendanceRepository.getPresentEmployeeCountOfInstitute(users.getInstitute().getId(), todayDate);
                int absentEmployees = totalEmployees - presentEmployees;

                result.addProperty("totalEmployees", Math.abs(totalEmployees));
                result.addProperty("presentEmployees", Math.abs(presentEmployees));
                result.addProperty("absentEmployees", Math.abs(absentEmployees));

                JsonArray machineArray = new JsonArray();
                JsonArray machineRPArray = new JsonArray();
                JsonArray machineAPArray = new JsonArray();

                List<Object[]> machineList = machineRepository.findOnlyWorkingMachines(todayDate, true, users.getInstitute().getId());
                for (int i = 0; i < machineList.size(); i++) {
                    Object[] machineObj = machineList.get(i);

                    List<Object[]> obj = machineRepository.getRequiredAndActualProductionCount(todayDate, Long.valueOf(machineObj[0].toString()), users.getInstitute().getId());
                    Object[] countObj = obj.get(0);

                    if (Double.parseDouble(countObj[0].toString()) > 0 && Double.parseDouble(countObj[1].toString()) > 0) {
                        machineArray.add(machineObj[2].toString());
                        machineRPArray.add(Precision.round(Double.parseDouble(countObj[0].toString()), 2));
                        machineAPArray.add(Precision.round(Double.parseDouble(countObj[1].toString()), 2));
                    }
                }
                result.add("machineList", machineArray);

                JsonArray machineCountsArray = new JsonArray();
                JsonObject rpMachineObj = new JsonObject();
                rpMachineObj.addProperty("name", "Required Production");
                rpMachineObj.add("data", machineRPArray);

                JsonObject apMachineObj = new JsonObject();
                apMachineObj.addProperty("name", "Actual Production");
                apMachineObj.add("data", machineAPArray);

                machineCountsArray.add(rpMachineObj);
                machineCountsArray.add(apMachineObj);

                result.add("machineCountsArray", machineCountsArray);

                JsonArray itemArray = new JsonArray();
                JsonArray itemRJCArray = new JsonArray();
                JsonArray itemRWCArray = new JsonArray();
                JsonArray itemDFCArray = new JsonArray();
                List<Object[]> jobList = jobRepository.findOnlyWorkingItems(todayDate, true, users.getInstitute().getId());
                for (int i = 0; i < jobList.size(); i++) {
                    Object[] itemObj = jobList.get(i);

                    List<Object[]> obj = jobRepository.getItemWiseCounts(todayDate, Long.valueOf(itemObj[0].toString()), users.getInstitute().getId());
                    Object[] countJob = obj.get(0);
                    if (Double.parseDouble(countJob[0].toString()) > 0 || Double.parseDouble(countJob[1].toString()) > 0 ||
                            Double.parseDouble(countJob[2].toString()) > 0) {
                        itemArray.add(itemObj[1].toString());
                        itemRJCArray.add(Precision.round(Double.parseDouble(countJob[0].toString()), 2));
                        itemRWCArray.add(Precision.round(Double.parseDouble(countJob[1].toString()), 2));
                        itemDFCArray.add(Precision.round(Double.parseDouble(countJob[2].toString()), 2));
                    }
                }
                result.add("itemList", itemArray);

                JsonArray itemCountsArray = new JsonArray();
                JsonObject itemRJObj = new JsonObject();
                itemRJObj.addProperty("name", "M/R COUNT");
                itemRJObj.add("data", itemRJCArray);

                JsonObject itemRWObj = new JsonObject();
                itemRWObj.addProperty("name", "R/W COUNT");
                itemRWObj.add("data", itemRWCArray);

                JsonObject itemDFObj = new JsonObject();
                itemDFObj.addProperty("name", "D/F COUNT");
                itemDFObj.add("data", itemDFCArray);

                itemCountsArray.add(itemRJObj);
                itemCountsArray.add(itemRWObj);
                itemCountsArray.add(itemDFObj);

                result.add("itemCountsArray", itemCountsArray);

                JsonArray itemWiseCountArray = new JsonArray();
                List<Object[]> itemWiseCountList = jobRepository.getItemListWithCounts(todayDate, users.getInstitute().getId());
                for (int i = 0; i < itemWiseCountList.size(); i++) {
                    Object[] itemObj = itemWiseCountList.get(i);

                    JsonObject itemOb = new JsonObject();
                    itemOb.addProperty("itemId", itemObj[0].toString());
                    itemOb.addProperty("itemName", itemObj[1].toString());
                    itemOb.addProperty("rejectCount", Precision.round(Double.parseDouble(itemObj[2].toString()), 2));
                    itemOb.addProperty("reworkCount", Precision.round(Double.parseDouble(itemObj[3].toString()), 2));
                    itemOb.addProperty("doubtfulCount", Precision.round(Double.parseDouble(itemObj[4].toString()), 2));

                    JsonArray empItemArray = new JsonArray();
                    List<Object[]> empItemList = jobRepository.getEmpWithItemCounts(todayDate, itemObj[0].toString(), users.getInstitute().getId());
                    for (int j = 0; j < empItemList.size(); j++) {
                        Object[] empItemObj = empItemList.get(j);

                        JsonObject jsonObject = new JsonObject();
                        String empName = empItemObj[5].toString();
                        if (!empItemObj[6].toString().equalsIgnoreCase(""))
                            empName = empName + " " + empItemObj[6].toString();
                        if (!empItemObj[7].toString().equalsIgnoreCase(""))
                            empName = empName + " " + empItemObj[7].toString();

                        jsonObject.addProperty("operationName", empItemObj[2].toString());
                        jsonObject.addProperty("operationNo", empItemObj[3].toString());
                        jsonObject.addProperty("employeeName", empName);
                        jsonObject.addProperty("rejectCount", Precision.round(Double.parseDouble(empItemObj[8].toString()), 2));
                        jsonObject.addProperty("reworkCount", Precision.round(Double.parseDouble(empItemObj[9].toString()), 2));
                        jsonObject.addProperty("doubtfulCount", Precision.round(Double.parseDouble(empItemObj[10].toString()), 2));

                        empItemArray.add(jsonObject);
                    }
                    itemOb.add("empItemArray", empItemArray);
                    itemWiseCountArray.add(itemOb);
                }
                result.add("itemCountList", itemWiseCountArray);
            } else { // shiftId have value
                int totalEmployees = employeeRepository.getEmployeeCountByShiftNew(true, Long.valueOf(shiftId), users.getInstitute().getId());
                int presentEmployees = attendanceRepository.getPresentEmployeeCountByShift(todayDate, Long.valueOf(shiftId), true, users.getInstitute().getId());
                int absentEmployees = totalEmployees - presentEmployees;

                result.addProperty("totalEmployees", Math.abs(totalEmployees));
                result.addProperty("presentEmployees", Math.abs(presentEmployees));
                result.addProperty("absentEmployees", Math.abs(absentEmployees));

                JsonArray machineArray = new JsonArray();
                JsonArray machineRPArray = new JsonArray();
                JsonArray machineAPArray = new JsonArray();

                List<Object[]> machineList = machineRepository.findOnlyWorkingMachinesByShift(todayDate, true, shiftId, users.getInstitute().getId());
                for (int i = 0; i < machineList.size(); i++) {
                    Object[] machineObj = machineList.get(i);

                    List<Object[]> obj = machineRepository.getRequiredAndActualProductionCount(todayDate, Long.valueOf(machineObj[0].toString()), users.getInstitute().getId());
                    Object[] countObj = obj.get(0);

                    if (Double.parseDouble(countObj[0].toString()) > 0 && Double.parseDouble(countObj[1].toString()) > 0) {
                        machineArray.add(machineObj[2].toString());
                        machineRPArray.add(Precision.round(Double.parseDouble(countObj[0].toString()), 2));
                        machineAPArray.add(Precision.round(Double.parseDouble(countObj[1].toString()), 2));
                    }
                }
                result.add("machineList", machineArray);

                JsonArray machineCountsArray = new JsonArray();
                JsonObject rpMachineObj = new JsonObject();
                rpMachineObj.addProperty("name", "Required Production");
                rpMachineObj.add("data", machineRPArray);

                JsonObject apMachineObj = new JsonObject();
                apMachineObj.addProperty("name", "Actual Production");
                apMachineObj.add("data", machineAPArray);

                machineCountsArray.add(rpMachineObj);
                machineCountsArray.add(apMachineObj);

                result.add("machineCountsArray", machineCountsArray);

                JsonArray itemArray = new JsonArray();
                JsonArray itemRJCArray = new JsonArray();
                JsonArray itemRWCArray = new JsonArray();
                JsonArray itemDFCArray = new JsonArray();
                List<Object[]> jobList = jobRepository.findOnlyWorkingItemsByShift(todayDate, true, shiftId, users.getInstitute().getId());
                for (int i = 0; i < jobList.size(); i++) {
                    Object[] itemObj = jobList.get(i);

                    List<Object[]> obj = jobRepository.getItemWiseCounts(todayDate, Long.valueOf(itemObj[0].toString()), users.getInstitute().getId());
                    Object[] countJob = obj.get(0);
                    if (Double.parseDouble(countJob[0].toString()) > 0 || Double.parseDouble(countJob[1].toString()) > 0 ||
                            Double.parseDouble(countJob[2].toString()) > 0) {
                        itemArray.add(itemObj[1].toString());
                        itemRJCArray.add(Precision.round(Double.parseDouble(countJob[0].toString()), 2));
                        itemRWCArray.add(Precision.round(Double.parseDouble(countJob[1].toString()), 2));
                        itemDFCArray.add(Precision.round(Double.parseDouble(countJob[2].toString()), 2));
                    }
                }
                result.add("itemList", itemArray);

                JsonArray itemCountsArray = new JsonArray();
                JsonObject itemRJObj = new JsonObject();
                itemRJObj.addProperty("name", "M/R COUNT");
                itemRJObj.add("data", itemRJCArray);

                JsonObject itemRWObj = new JsonObject();
                itemRWObj.addProperty("name", "R/W COUNT");
                itemRWObj.add("data", itemRWCArray);

                JsonObject itemDFObj = new JsonObject();
                itemDFObj.addProperty("name", "D/F COUNT");
                itemDFObj.add("data", itemDFCArray);

                itemCountsArray.add(itemRJObj);
                itemCountsArray.add(itemRWObj);
                itemCountsArray.add(itemDFObj);

                result.add("itemCountsArray", itemCountsArray);


                JsonArray itemWiseCountArray = new JsonArray();
                List<Object[]> itemWiseCountList = jobRepository.getItemListWithCountsByShift(todayDate, shiftId, users.getInstitute().getId());
                for (int i = 0; i < itemWiseCountList.size(); i++) {
                    Object[] itemObj = itemWiseCountList.get(i);

                    JsonObject itemOb = new JsonObject();
                    itemOb.addProperty("itemId", itemObj[0].toString());
                    itemOb.addProperty("itemName", itemObj[1].toString());
                    itemOb.addProperty("rejectCount", Precision.round(Double.parseDouble(itemObj[2].toString()), 2));
                    itemOb.addProperty("reworkCount", Precision.round(Double.parseDouble(itemObj[3].toString()), 2));
                    itemOb.addProperty("doubtfulCount", Precision.round(Double.parseDouble(itemObj[4].toString()), 2));

                    JsonArray empItemArray = new JsonArray();
                    List<Object[]> empItemList = jobRepository.getEmpWithItemCounts(todayDate, itemObj[0].toString(), users.getInstitute().getId());
                    for (int j = 0; j < empItemList.size(); j++) {
                        Object[] empItemObj = empItemList.get(j);

                        JsonObject jsonObject = new JsonObject();
                        String empName = empItemObj[5].toString();
                        if (!empItemObj[6].toString().equalsIgnoreCase(""))
                            empName = empName + " " + empItemObj[6].toString();
                        if (!empItemObj[7].toString().equalsIgnoreCase(""))
                            empName = empName + " " + empItemObj[7].toString();

                        jsonObject.addProperty("operationName", empItemObj[2].toString());
                        jsonObject.addProperty("operationNo", empItemObj[3].toString());
                        jsonObject.addProperty("employeeName", empName);
                        jsonObject.addProperty("rejectCount", Precision.round(Double.parseDouble(empItemObj[8].toString()), 2));
                        jsonObject.addProperty("reworkCount", Precision.round(Double.parseDouble(empItemObj[9].toString()), 2));
                        jsonObject.addProperty("doubtfulCount", Precision.round(Double.parseDouble(empItemObj[10].toString()), 2));

                        empItemArray.add(jsonObject);
                    }
                    itemOb.add("empItemArray", empItemArray);
                    itemWiseCountArray.add(itemOb);
                }

                /*JsonArray itemWiseCountArray = new JsonArray();
                List<Object[]> itemWiseCountList = jobRepository.getItemListWithCountsByShift(todayDate, shiftId);
                for (int i = 0; i < itemWiseCountList.size(); i++) {
                    Object[] itemObj = itemWiseCountList.get(i);

                    JsonObject itemOb = new JsonObject();
                    itemOb.addProperty("itemId", itemObj[0].toString());
                    itemOb.addProperty("itemName", itemObj[1].toString());
                    itemOb.addProperty("rejectCount", Precision.round(Double.parseDouble(itemObj[2].toString()), 2));
                    itemOb.addProperty("reworkCount", Precision.round(Double.parseDouble(itemObj[3].toString()), 2));
                    itemOb.addProperty("doubtfulCount", Precision.round(Double.parseDouble(itemObj[4].toString()), 2));

                    itemWiseCountArray.add(itemOb);
                }*/
                result.add("itemCountList", itemWiseCountArray);
            }

            responseMessage.add("response", result);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object recalculateEmpSalaryForMonth(Map<String, String> jsonRequest, HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            String employeeId = jsonRequest.get("employeeId");
            String yearMonth = jsonRequest.get("yearMonth");
            System.out.println("yearMonth " + yearMonth);

            int year = Integer.parseInt(yearMonth.split("-")[0]);
            int month = Integer.parseInt(yearMonth.split("-")[1]);
            System.out.println("year " + year);
            System.out.println("month " + month);
            yearMonth = year + "-" + month;
            System.out.println("yearMonth " + yearMonth);

            if (employeeId.equalsIgnoreCase("all")) {
                List<Employee> employeeList = employeeRepository.findByStatus(true);

                for (Employee employee : employeeList) {
                    calculateEmployeeMonthSalary(employee, year, month, yearMonth, users);
                }
            } else {
                Employee employee = employeeRepository.findByIdAndStatus(Long.parseLong(employeeId), true);
                calculateEmployeeMonthSalary(employee, year, month, yearMonth, users);
            }

            response.addProperty("message", "Recalculation done");
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            payrollLogger.error("updateSalaryForDay Exception ===>" + e);
            System.out.println("updateSalaryForDay Exception ===>" + e.getMessage());
            e.printStackTrace();

            response.addProperty("message", "Failed to update data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return response;
    }

    private void calculateEmployeeMonthSalary(Employee employee, int year, int month, String yearMonth, Users users) {
        String wagesType = employee.getEmployeeWagesType();
        System.out.println("wagesType " + wagesType);
        Double wagesPerDaySalary = utility.getEmployeeWages(employee.getId());
        double perDaySalary = 0;
        double perHourSalary = 0;
        if (wagesPerDaySalary != null) {
            perDaySalary = wagesPerDaySalary;
            perHourSalary = Precision.round(perDaySalary / 8.0, 2);

            System.out.println("perDaySalary " + perDaySalary);
            System.out.println("perHourSalary " + perHourSalary);
            double totalDaysInMonth = attendanceRepository.getPresentDaysOfEmployeeOfMonth(year, month, employee.getId(), true, "approve");
            System.out.println("totalDaysInMonth " + totalDaysInMonth);

            double totalHoursInMonth = 0;
            double netSalaryInPoints = 0;
            double netSalaryInPcs = 0;
            double netSalaryInHours = 0;
            double netSalaryInDays = 0;
            double final_day_salary = 0;

            List<Object[]> attendanceList = attendanceRepository.getAttendanceList(year, month, employee.getId(), true, "approve");
            for (int i = 0; i < attendanceList.size(); i++) {
                Object[] attObj = attendanceList.get(i);
                System.out.println("attendance Id=" + attObj[0].toString());

                if (attObj[11].toString().equalsIgnoreCase("pcs"))
                    netSalaryInPcs += Double.parseDouble(attObj[10].toString());
                if (attObj[11].toString().equalsIgnoreCase("point"))
                    netSalaryInPoints += Double.parseDouble(attObj[10].toString());
                if (attObj[11].toString().equalsIgnoreCase("day"))
                    netSalaryInDays += Double.parseDouble(attObj[10].toString());
                if (attObj[11].toString().equalsIgnoreCase("hr"))
                    netSalaryInHours += Double.parseDouble(attObj[10].toString());

                final_day_salary += Double.parseDouble(attObj[10].toString());
                totalHoursInMonth += Double.parseDouble(attObj[34].toString());
            }
            System.out.println("totalHoursInMonth " + totalHoursInMonth);
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

            EmployeePayroll employeePayroll = null;
            employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), yearMonth);
            if (employeePayroll == null) {
                employeePayroll = new EmployeePayroll();
            }

            System.out.println("netSalaryInDays " + netSalaryInDays);
            System.out.println("netSalaryInHours " + netSalaryInHours);
            System.out.println("netSalaryInPoints " + netSalaryInPoints);
            System.out.println("netSalaryInPcs " + netSalaryInPcs);
            System.out.println("final_day_salary " + final_day_salary);

            netSalary = netSalaryInDays + netSalaryInPcs + netSalaryInPoints + netSalaryInHours;
            System.out.println("netSalary " + netSalary);

            String basicQuery = "SELECT * FROM `payhead_tbl` WHERE name LIKE '%basic%' AND status=1 ORDER BY id " + "DESC LIMIT 1";
            Query q = entityManager.createNativeQuery(basicQuery, Payhead.class);
            Payhead payhead = (Payhead) q.getSingleResult();
            if (payhead != null) {
                basicPer = payhead.getPercentage();
                basic = (netSalary * (basicPer / 100.0));
            }
            specialAllowance = netSalary - basic;
            if (employee.getEmployeePf() != null && employee.getEmployeePf() > 0) {
                pfPer = employee.getEmployeePf();
                pf = (basic * (pfPer / 100.0));
            }

            if (employee.getEmployeeEsi() != null && employee.getEmployeeEsi() > 0) {
                esiPer = employee.getEmployeeEsi();
                esi = (netSalary * (esiPer / 100.0));
            }

            if(employee.getEmployeeHaveProfTax() == true) {
                if (netSalary >= 7500 && netSalary < 10000) {
                    pfTax = 175;
                } else if (netSalary >= 10000) {
                    pfTax = 200;
                    if (month == 3) {
                        pfTax = 300;
                    }
                }
            }

            allowanceAmount = allowanceRepository.getSumOfAllowance();
            deductionAmount = deductionRepository.getSumOfDeduction();

            totalDeduction = (pf + esi + pfTax + deductionAmount);
            payableAmount = (netSalary + allowanceAmount - totalDeduction);

            double sumAdvance = advancePaymentRepository.getEmployeeAdvanceOfMonth(employee.getId(), year, month);
            advance = sumAdvance;
            netPayableAmount = (payableAmount - advance + incentive);

            employeePayroll.setEmployee(employee);
            employeePayroll.setWagesType(wagesType);
            employeePayroll.setYearMonth(yearMonth);
            employeePayroll.setDesignation(employee.getDesignation().getName());
            employeePayroll.setPerDaySalary(perDaySalary);
            employeePayroll.setPerHourSalary(perHourSalary);
            employeePayroll.setNoDaysPresent(totalDaysInMonth);
            employeePayroll.setTotalDaysInMonth(totalDaysInMonth);
            employeePayroll.setTotalHoursInMonth(totalHoursInMonth);
            employeePayroll.setNetSalary(netSalary);
            employeePayroll.setNetSalaryInDays(netSalaryInDays);
            employeePayroll.setNetSalaryInHours(netSalaryInHours);
//            employeePayroll.setNetSalaryInPoints(netSalaryInPoints);
//            employeePayroll.setNetSalaryInPcs(netSalaryInPcs);
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
            employeePayroll.setAdvance(advance);
            employeePayroll.setIncentive(incentive);
            employeePayroll.setNetPayableAmount(netPayableAmount);
            employeePayroll.setUpdatedAt(LocalDateTime.now());
            employeePayroll.setUpdatedBy(users.getId());
            employeePayroll.setInstitute(users.getInstitute());

            employeePayrollRepository.save(employeePayroll);
        } else {
            System.out.println("Employee wages not found, Please update & check again=" + employee.getId());
            /*response.addProperty("message", "Employee wages not found, Please update & check again");
            response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            return response;*/
        }
    }

    public JsonObject getEmployeePaymentSheet(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        String yearMonth = jsonRequest.get("yearMonth");
        String companyId = jsonRequest.get("companyId");
        System.out.println("yearMonth " + yearMonth);

        int year = Integer.parseInt(yearMonth.split("-")[0]);
        int month = Integer.parseInt(yearMonth.split("-")[1]);
        yearMonth = year + "-" + month;
        System.out.println("yearMonth " + yearMonth);

        double totalDs = 0;
        double totalWH = 0;
        double totalNSIH = 0;
        double totalBc = 0;
        double totalSA = 0;
        double totalPf = 0;
        double totalEi = 0;
        double totalTD = 0;
        double totalPA = 0;
        double totalAdv = 0;
        double totalIve = 0;
        double totalNPA = 0;
        double totalNS = 0;
        double totalPHS = 0;
        double totalSD = 0;
        double totalSH = 0;
        double totalSPt = 0;
        double totalSPcs = 0;

        JsonArray employeeArray = new JsonArray();
        List<Employee> employees = new ArrayList<>();

        if (companyId.equalsIgnoreCase("all"))
            employees = employeeRepository.findAllByInstituteIdAndStatus(users.getInstitute().getId(), true);
        else
            employees = employeeRepository.findByInstituteIdAndCompanyIdAndStatus(users.getInstitute().getId(), Long.valueOf(companyId), true);
        for (Employee employee : employees) {

            JsonObject empObject = new JsonObject();
            empObject.addProperty("id", employee.getId());
            empObject.addProperty("employeeName", utility.getEmployeeName(employee));
            empObject.addProperty("companyName", employee.getCompany().getCompanyName());

            EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndInstituteIdAndYearMonth(employee.getId(), users.getInstitute().getId(), yearMonth);
            if (employeePayroll != null) {
                empObject.addProperty("days", employeePayroll.getNoDaysPresent());
                empObject.addProperty("workingHour", Precision.round(employeePayroll.getTotalHoursInMonth(), 2));
                empObject.addProperty("netSalaryInHours", Precision.round(employeePayroll.getNetSalaryInHours(), 2));
                empObject.addProperty("basicPer", Precision.round(employeePayroll.getBasicPer(), 2));
                empObject.addProperty("basic", Precision.round(employeePayroll.getBasic(), 2));
                empObject.addProperty("specialAllowance", Precision.round(employeePayroll.getSpecialAllowance(), 2));
                empObject.addProperty("pfPer", Precision.round(employeePayroll.getPfPer(), 2));
                empObject.addProperty("pf", Precision.round(employeePayroll.getPf(), 2));
                empObject.addProperty("esiPer", Precision.round(employeePayroll.getEsiPer(), 2));
                empObject.addProperty("esi", Precision.round(employeePayroll.getEsi(), 2));
                empObject.addProperty("totalDeduction", Precision.round(employeePayroll.getTotalDeduction(), 2));
                empObject.addProperty("payableAmount", Precision.round(employeePayroll.getPayableAmount(), 2));
                empObject.addProperty("advance", Precision.round(employeePayroll.getAdvance(), 2));
                empObject.addProperty("incentive", Precision.round(employeePayroll.getIncentive(), 2));
                empObject.addProperty("netPayableAmount", Precision.round(employeePayroll.getNetPayableAmount(), 2));
                empObject.addProperty("netSalary", Precision.round(employeePayroll.getNetSalary(), 2));
                empObject.addProperty("perHourSalary", Precision.round(employeePayroll.getPerHourSalary(), 2));
                empObject.addProperty("netSalaryInDays", Precision.round(employeePayroll.getNetSalaryInDays(), 2));
                empObject.addProperty("netSalaryInHours", Precision.round(employeePayroll.getNetSalaryInHours(), 2));
//                empObject.addProperty("netSalaryInPoints", Precision.round(employeePayroll.getNetSalaryInPoints(), 2));
//                empObject.addProperty("netSalaryInPcs", Precision.round(employeePayroll.getNetSalaryInPcs(), 2));

                employeeArray.add(empObject);

                totalDs = totalDs + employeePayroll.getNoDaysPresent();
                totalWH = totalWH + employeePayroll.getTotalHoursInMonth();
                totalNSIH = totalNSIH + employeePayroll.getNetSalaryInHours();
                totalBc = totalBc + employeePayroll.getBasic();
                totalSA = totalSA + employeePayroll.getSpecialAllowance();
                totalPf = totalPf + employeePayroll.getPf();
                totalEi = totalEi + employeePayroll.getEsi();
                totalTD = totalTD + employeePayroll.getTotalDeduction();
                totalPA = totalPA + employeePayroll.getPayableAmount();
                totalAdv = totalAdv + employeePayroll.getAdvance();
                totalIve = totalIve + employeePayroll.getIncentive();
                totalNPA = totalNPA + employeePayroll.getNetPayableAmount();
                totalNS = totalNS + employeePayroll.getNetSalary();
                totalPHS = totalPHS + employeePayroll.getPerHourSalary();
                totalSD = totalSD + employeePayroll.getNetSalaryInDays();
                totalSH = totalSH + employeePayroll.getNetSalaryInHours();
//                totalSPt = totalSPt + employeePayroll.getNetSalaryInPoints();
//                totalSPcs = totalSPcs + employeePayroll.getNetSalaryInPcs();
            }
        }

        JsonObject totalObject = new JsonObject();
        totalObject.addProperty("totalDs", Precision.round(totalDs, 2));
        totalObject.addProperty("totalWH", Precision.round(totalWH, 2));
        totalObject.addProperty("totalNSIH", Precision.round(totalNSIH, 2));
        totalObject.addProperty("totalBc", Precision.round(totalBc, 2));
        totalObject.addProperty("totalSA", Precision.round(totalSA, 2));
        totalObject.addProperty("totalPf", Precision.round(totalPf, 2));
        totalObject.addProperty("totalEi", Precision.round(totalEi, 2));
        totalObject.addProperty("totalTD", Precision.round(totalTD, 2));
        totalObject.addProperty("totalPA", Precision.round(totalPA, 2));
        totalObject.addProperty("totalAdv", Precision.round(totalAdv, 2));
        totalObject.addProperty("totalIve", Precision.round(totalIve, 2));
        totalObject.addProperty("totalNPA", Precision.round(totalNPA, 2));
        totalObject.addProperty("totalNS", Precision.round(totalNS, 2));
        totalObject.addProperty("totalPHS", Precision.round(totalPHS, 2));
        totalObject.addProperty("totalSD", Precision.round(totalSD, 2));
        totalObject.addProperty("totalSH", Precision.round(totalSH, 2));
        totalObject.addProperty("totalSPt", Precision.round(totalSPt, 2));
        totalObject.addProperty("totalSPcs", Precision.round(totalSPcs, 2));

        response.add("totalObject", totalObject);
        response.add("response", employeeArray);
        response.addProperty("responseStatus", HttpStatus.OK.value());

        return response;
    }

    public InputStream getExcelEmployeePaymentSheet(String yearMonth, String companyId, HttpServletRequest request) {

        try {
            System.out.println("yearMonth " + yearMonth);

            int year = Integer.parseInt(yearMonth.split("-")[0]);
            int month = Integer.parseInt(yearMonth.split("-")[1]);
            yearMonth = year + "-" + month;
            System.out.println("yearMonth " + yearMonth);

            JsonArray employeeArray = new JsonArray();
            List<Employee> employees = new ArrayList<>();

            if (companyId.equalsIgnoreCase("all"))
                employees = employeeRepository.findByStatus(true);
            else
                employees = employeeRepository.findByCompanyIdAndStatus(Long.valueOf(companyId), true);
            for (Employee employee : employees) {
                JsonObject empObject = new JsonObject();
                empObject.addProperty("id", employee.getId());
                empObject.addProperty("employeeName", utility.getEmployeeName(employee));
                empObject.addProperty("companyName", employee.getCompany().getCompanyName());

                EmployeePayroll employeePayroll = employeePayrollRepository.findByEmployeeIdAndYearMonth(employee.getId(), yearMonth);
                if (employeePayroll != null) {
                    empObject.addProperty("days", employeePayroll.getNoDaysPresent());
                    empObject.addProperty("workingHour", Precision.round(employeePayroll.getTotalHoursInMonth(), 2));
                    empObject.addProperty("netSalaryInHours", Precision.round(employeePayroll.getNetSalaryInHours(), 2));
//                    empObject.addProperty("netSalaryInPoints", Precision.round(employeePayroll.getNetSalaryInPoints(), 2));
                    empObject.addProperty("netSalaryInDays", Precision.round(employeePayroll.getNetSalaryInDays(), 2));
//                    empObject.addProperty("netSalaryInPcs", Precision.round(employeePayroll.getNetSalaryInPcs(), 2));
                    empObject.addProperty("netSalary", Precision.round(employeePayroll.getNetSalary(), 2));
                    empObject.addProperty("basicPer", Precision.round(employeePayroll.getBasicPer(), 2));
                    empObject.addProperty("basic", Precision.round(employeePayroll.getBasic(), 2));
                    empObject.addProperty("specialAllowance", Precision.round(employeePayroll.getSpecialAllowance(), 2));
                    empObject.addProperty("pfPer", Precision.round(employeePayroll.getPfPer(), 2));
                    empObject.addProperty("pf", Precision.round(employeePayroll.getPf(), 2));
                    empObject.addProperty("esiPer", Precision.round(employeePayroll.getEsiPer(), 2));
                    empObject.addProperty("esi", Precision.round(employeePayroll.getEsi(), 2));
                    empObject.addProperty("totalDeduction", Precision.round(employeePayroll.getTotalDeduction(), 2));
                    empObject.addProperty("payableAmount", Precision.round(employeePayroll.getPayableAmount(), 2));
                    empObject.addProperty("advance", Precision.round(employeePayroll.getAdvance(), 2));
                    empObject.addProperty("incentive", Precision.round(employeePayroll.getIncentive(), 2));
                    empObject.addProperty("netPayableAmount", Precision.round(employeePayroll.getNetPayableAmount(), 2));

                    employeeArray.add(empObject);
                }
            }

            ByteArrayInputStream in = convertToExcel(employeeArray);

            return in;
        } catch (Exception e) {
            payrollLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    private ByteArrayInputStream convertToExcel(JsonArray jsonArray) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(empSalarySHEET);

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
            for (JsonElement jsonElement : jsonArray) {
                JsonObject obj = jsonElement.getAsJsonObject();
                Row row = sheet.createRow(rowIdx++);
                try {
                    row.createCell(0).setCellValue(obj.get("employeeName").getAsString());
                    row.createCell(1).setCellValue(obj.get("companyName").getAsString());

                    row.createCell(2).setCellValue(obj.get("days").getAsString());
                    row.createCell(3).setCellValue(obj.get("workingHour").getAsString());
                    row.createCell(4).setCellValue(obj.get("netSalaryInDays").getAsString());
                    row.createCell(5).setCellValue(obj.get("netSalaryInHours").getAsString());
                    row.createCell(6).setCellValue(obj.get("netSalaryInPoints").getAsString());
                    row.createCell(7).setCellValue(obj.get("netSalaryInPcs").getAsString());
                    row.createCell(8).setCellValue(obj.get("netSalary").getAsString());
//                    row.createCell(5).setCellValue(obj.get("basic").getAsString() + " (" + obj.get("basicPer").getAsString() + ")");
                    row.createCell(9).setCellValue(obj.get("basic").getAsString());
                    row.createCell(10).setCellValue(obj.get("specialAllowance").getAsString());
//                    row.createCell(7).setCellValue(obj.get("pf").getAsString() + " (" + obj.get("pfPer").getAsString() + ")");
                    row.createCell(11).setCellValue(obj.get("pf").getAsString());
//                    row.createCell(8).setCellValue(obj.get("esi").getAsString() + " (" + obj.get("esiPer").getAsString() + ")");
                    row.createCell(12).setCellValue(obj.get("esi").getAsString());
                    row.createCell(13).setCellValue(obj.get("totalDeduction").getAsString());
                    row.createCell(14).setCellValue(obj.get("payableAmount").getAsString());
                    row.createCell(15).setCellValue(obj.get("advance").getAsString());
                    row.createCell(16).setCellValue(obj.get("incentive").getAsString());
                    row.createCell(17).setCellValue(obj.get("netPayableAmount").getAsString());
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

}