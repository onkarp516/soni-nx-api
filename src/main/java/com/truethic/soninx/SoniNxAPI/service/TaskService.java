package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.fileConfig.FileStorageService;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.viewRepository.AttendanceViewRepository;
import com.truethic.soninx.SoniNxAPI.dto.TaskDTO;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import com.truethic.soninx.SoniNxAPI.views.TaskView;
import com.truethic.soninx.SoniNxAPI.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;

@Service
@Slf4j
@Transactional
public class TaskService {
    @Autowired
    private ShiftRepository shiftRepository;
    private static final Logger taskLogger = LoggerFactory.getLogger(TaskService.class);
    @Value("${spring.serversource.url}")
    private String serverUrl;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private TaskMasterRepository taskRepository;
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private JobOperationRepository jobOperationRepository;
    @Autowired
    private WorkBreakRepository workBreakRepository;
    @Autowired
    private TaskViewRepository taskViewRepository;
    @Autowired
    private AttendanceViewRepository attendanceViewRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private TaskDetailRepository taskDetailRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TaskMasterHistoryRepository taskMasterHistoryRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Utility utility;
    @Autowired
    private OperationDetailsRepository operationDetailsRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    public JsonObject startTask(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
        TaskMaster task = new TaskMaster();

        try {
            Integer taskType = Integer.valueOf(requestParam.get("taskType"));
            if (requestParam.containsKey("attendanceId")) {
                Long attendanceId = Long.valueOf(requestParam.get("attendanceId"));
                Attendance attendance = attendanceRepository.findByIdAndStatus(attendanceId, true);
                task.setAttendance(attendance);
            }

            LocalDateTime startTime = LocalDateTime.now();
            task.setTaskDate(LocalDate.now());
            task.setEmployee(employee);

            task.setTaskType(taskType);
            task.setWorkDone(false);
            task.setTaskStatus("in-progress");

            LocalTime shiftTime = employee.getShift().getWorkingHours();
            Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
            System.out.println("shiftMinutes in min " + shiftMinutes);
            Double shiftHours = Double.valueOf((shiftMinutes / 60));
            System.out.println("shiftHours" + shiftHours);

            Double wagesPerDay = utility.getEmployeeWages(employee.getId());
            if (wagesPerDay == null) {

                responseMessage.addProperty("message","Your salary not updated! Please contact to Admin");
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                System.out.println("employee wagesPerDay =" + wagesPerDay);
                double wagesPerHour = (wagesPerDay / shiftHours);

                task.setWagesPerDay(wagesPerDay);
                task.setWagesPerHour(wagesPerHour);
                task.setEmployeeWagesType(employee.getEmployeeWagesType());

                task.setStartTime(startTime);
                if (taskType == 2) {
                    Long breakId = Long.valueOf(requestParam.get("breakId"));
                    WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);
                    task.setWorkBreak(workBreak);
                }
                if (requestParam.containsKey("remark")) {
                    String remark = requestParam.get("remark");
                    if (!remark.equalsIgnoreCase("")) {
                        task.setRemark(remark);
                    }
                }

                task.setCreatedAt(LocalDateTime.now());
                task.setCreatedBy(employee.getId());
                task.setInstitute(employee.getInstitute());
                task.setStatus(true);
                try {
                    TaskMaster savedTaskMaster = taskRepository.save(task);
//                updateEmployeeTaskSummary(employee, attendance);
//                responseMessage.setResponse(savedTaskMaster);
                    responseMessage.addProperty("taskId",savedTaskMaster.getId());
                    responseMessage.addProperty("message","Successfully task started");
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    taskLogger.error("Failed to start task " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.addProperty("message","Failed to start task");
                    responseMessage.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            taskLogger.error("Failed to start task " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message","Failed to start task");
            responseMessage.addProperty("responseStatus",HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    private Boolean checkSameTaskStarting(Employee employee, Map<String, String> requestParam) {
//        in-progress
        Long attendanceId = Long.valueOf(requestParam.get("attendanceId"));
        Long machineId = Long.parseLong(requestParam.get("machineId"));
        Long jobId = Long.parseLong(requestParam.get("jobId"));
        Long jobOperationId = Long.parseLong(requestParam.get("jobOperationId"));
        Integer taskType = Integer.valueOf(requestParam.get("taskType"));

        TaskMaster taskMaster = taskRepository.findByEmployeeIdAndAttendanceIdAndTaskTypeAndMachineIdAndJobIdAndJobOperationIdAndTaskStatusAndStatus(
                employee.getId(), attendanceId, taskType, machineId, jobId, jobOperationId, "in-progress", true);
        return taskMaster != null;
    }

    public Object endTask(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            Long taskId = Long.valueOf(requestParam.get("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);

            double totalActualTime = 0;
            if (task != null) {
                Integer taskType = task.getTaskType();
                task.setTaskStatus("complete");

                LocalDateTime l1 = task.getStartTime();
                LocalDateTime l2 = LocalDateTime.now();
                task.setEndTime(l2);
                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60));
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                double time = totalTime;
                System.out.println("total time in min " + time);
                task.setTotalTime(time);
                task.setActualWorkTime(time);

                LocalTime shiftTime = employee.getShift().getWorkingHours();
                Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
                System.out.println("shiftMinutes in min " + shiftMinutes);
                Double shiftHours = Double.valueOf((shiftMinutes / 60));
                System.out.println("shiftHours" + shiftHours);

                LocalTime workTime = utility.getDateTimeDiffInTime(l1, l2);
                task.setWorkingTime(workTime);
                if (taskType == 2) { // 2=>Downtime
                    System.out.println("user time in minutes 2=>Downtime" + time);
                    if (task.getWorkDone()) {
                        task.setActualWorkTime(time);
                    }
                    /*calculate break hour wages for PCS basis employee which breaks are working only*/
                    task.setBreakWages(calculateBreakData(time, task, shiftHours));
                }

//                if (taskType == 3 || taskType == 4) { // 3=>Setting time
//                    System.out.println("user time in minutes 2=>Setting time" + time);
//                    task.setActualWorkTime(time);
//                    /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                    task.setBreakWages(calculateBreakData(time, task)); // no need to do calculate for breaks & setting times
//                } else if (taskType == 1) { // 1=>Task
//                    System.out.println("user time in minutes 2=>Task" + time);
//
//                    double totalBreakMinutes = taskRepository.getSumOfBreakTime(task.getId());
//                    double actualTaskTime = time - totalBreakMinutes;
//
//                    task.setActualWorkTime(actualTaskTime);
//                    task.setTotalCount(Long.valueOf(requestParam.get("totalCount")));
//                    task.setReworkQty(Double.valueOf(requestParam.get("reworkQty")));
//                    task.setMachineRejectQty(Double.valueOf(requestParam.get("machineRejectQty")));
//                    task.setDoubtfulQty(Double.valueOf(requestParam.get("doubtfulQty")));
//                    task.setUnMachinedQty(Double.valueOf(requestParam.get("unMachinedQty")));
//                    task.setOkQty(Double.valueOf(requestParam.get("okQty")));
//
//                    Long okQty = Long.valueOf(requestParam.get("okQty"));
//
//                    System.out.println("user time in minutes " + time);
//                    System.out.println("user totalActualTime in minutes " + actualTaskTime);
//                    double jobsPerHour = (60.0 / task.getCycleTime());
//                    double workHours = (time / 60.0);
//                    double requiredProduction = (actualTaskTime / task.getCycleTime());
//                    System.out.println("actualProduction " + okQty);
//                    System.out.println("requiredProduction " + requiredProduction);
//                    double actualProduction = okQty;
//                    double shortProduction = (actualProduction - requiredProduction);
//                    double percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
//                    System.out.println("percentageOfTask " + percentageOfTask);
//
////                    if(actualProduction <= requiredProduction) {
//                        task.setJobsPerHour(jobsPerHour);
//                        task.setWorkingHour(workHours);
//                        task.setRequiredProduction(requiredProduction);
//                        task.setActualProduction(actualProduction);
//                        task.setShortProduction(shortProduction);
//                        task.setPercentageOfTask(percentageOfTask);
//
//                        double productionPoint = (double) okQty * task.getPerJobPoint();
//                        double productionWorkingHour = productionPoint / 12.5;
//                        double settingTimeInMinutes = totalBreakMinutes;
//                        double perMinutePoint = 100.0 / 60;
//                        double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
//                        double workingHourWithSetting = productionWorkingHour + settingTimeInHour;
//                        double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
//                        double totalPoint = productionPoint + settingTimePoint;
//
//                        double wagesPerPoint = task.getWagesPerDay() / 100.0;
//                        double wagesPointBasis = wagesPerPoint * totalPoint;
//                        double wagesPerPcs = task.getPcsRate();
//                        double wagesPcsBasis = okQty * wagesPerPcs;
//
//                        task.setProdPoint(productionPoint);
//                        task.setProdWorkingHour(productionWorkingHour);
//                        task.setSettingTimeInMin(settingTimeInMinutes);
//                        task.setSettingTimeInHour(settingTimeInHour);
//                        task.setWorkingHourWithSetting(workingHourWithSetting);
//                        task.setSettingTimePoint(settingTimePoint);
//                        task.setTotalPoint(totalPoint);
//
//                        task.setWagesPerPoint(wagesPerPoint);
//                        task.setWagesPointBasis(wagesPointBasis);
//                        task.setWagesPcsBasis(wagesPcsBasis);
//                        Machine machine = task.getMachine();
//                        if (machine.getIsMachineCount()) {
//                            task.setMachineEndCount(Long.valueOf(requestParam.get("machineEndCount")));
//
//                            machine.setCurrentMachineCount(Long.valueOf(requestParam.get("machineEndCount")));
//                            machineRepository.save(machine);
//                        }
////                    }else{
////                        System.out.println("Actual Production is greater than Req Prod");
////                        responseMessage.setMessage("Actual Production greater than Required Production");
////                        responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
////                        return responseMessage;
////                    }
//                } else if (taskType == 2) { // 2=>Downtime
//                    System.out.println("user time in minutes 2=>Downtime" + time);
//                    if (task.getWorkDone()) {
//                        task.setActualWorkTime(time);
//                    }
//                    /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                    task.setBreakWages(calculateBreakData(time, task));
//                } else if (taskType == 4) { // 4=> task without machine
//                    System.out.println("user time in minutes 4=> task without machine " + time);
////                    calculateBreakData(time, task); // no need to do calculate for breaks & setting times
//
//                    double totalBreakMinutes = taskRepository.getSumOfBreakTime(task.getId());
//                    double actualTaskTime = time - totalBreakMinutes;
//                    task.setActualWorkTime(actualTaskTime);
//                    double workHours = (time / 60.0);
//                    double prodWorkHours = (actualTaskTime / 60.0);
//                    task.setWorkingHour(workHours);
//                    task.setProdWorkingHour(prodWorkHours);
//                    task.setProdWorkingHour(workHours);
//                    double settingTimeInMinutes = totalBreakMinutes;
//                    double perMinutePoint = 100.0 / 60;
//                    double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
//
//                    task.setSettingTimeInMin(settingTimeInMinutes);
//                    task.setSettingTimeInHour(settingTimeInHour);
//                    task.setActualWorkTime(time);
//                }
//
//                if (requestParam.containsKey("correctiveAction")) {
//                    String correctiveAction = requestParam.get("correctiveAction");
//                    if (!correctiveAction.equalsIgnoreCase("")) {
//                        task.setCorrectiveAction(correctiveAction);
//                    }
//                }
//                if (requestParam.containsKey("preventiveAction")) {
//                    String preventiveAction = requestParam.get("preventiveAction");
//                    if (!preventiveAction.equalsIgnoreCase("")) {
//                        task.setPreventiveAction(preventiveAction);
//                    }
//                }

                if (requestParam.containsKey("endRemark")) {
                    String endRemark = requestParam.get("endRemark");
                    if (!endRemark.equalsIgnoreCase("")) {
                        task.setEndRemark(endRemark);
                    }
                }

                task.setUpdatedAt(LocalDateTime.now());
                task.setUpdatedBy(employee.getId());
                task.setInstitute(employee.getInstitute());
                try {
                    TaskMaster savedTaskMaster = taskRepository.save(task);
                    updateEmployeeTaskSummary(task.getAttendance());
                    responseMessage.setMessage("Successfully task finished");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
//                    boolean newTask = Boolean.parseBoolean(requestParam.get("newTask"));
//                    if (newTask && taskType == 1) {
//                        if (copyTask(savedTaskMaster)) {
//                            responseMessage.setMessage("Successfully task finished & task copy done");
//                            responseMessage.setResponseStatus(HttpStatus.OK.value());
//                        } else {
//                            responseMessage.setMessage("Successfully task finished, failed copy task, add manually");
//                            responseMessage.setResponseStatus(HttpStatus.OK.value());
//                        }
//                    } else {
//                        responseMessage.setMessage("Successfully task finished");
//                        responseMessage.setResponseStatus(HttpStatus.OK.value());
//                    }
                } catch (Exception e) {
                    taskLogger.error("Failed to finish task " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to finish task");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            taskLogger.error("failed to finish task" + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to finish task");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject getReqProductionCount(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Long taskId = Long.valueOf(requestParam.get("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);
            if (task != null && task.getTaskType() == 1) {
                LocalDateTime l1 = task.getStartTime();
                LocalDateTime l2 = LocalDateTime.now();
                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60));
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                double totalBreakMinutes = taskRepository.getSumOfBreakTime(task.getId());
                double actualTaskTime = totalTime - totalBreakMinutes;
                double requiredProduction = (actualTaskTime / task.getCycleTime());
                responseMessage.addProperty("requiredProduction", requiredProduction);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                responseMessage.addProperty("message", "Failed to load data");
                responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception e) {
            taskLogger.error("failed to finish task" + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public double getBreaksWagesForPcsWages(TaskMaster task) {
        double breakWages = 0;
        List<TaskMaster> taskMasters = taskRepository.findByTaskMasterIdAndStatusAndWorkDone(task.getId(), true, true);
        for (TaskMaster taskMaster : taskMasters) {
            double breakTime = taskMaster.getActualWorkTime();
            double breakHours = breakTime / 60.0;
            double hourWages = breakHours * (taskMaster.getWagesPerHour() != null ? taskMaster.getWagesPerHour() : 0);
            breakWages = breakWages + hourWages;
        }
        return breakWages;
    }

    public double calculateBreakData(double time, TaskMaster task, Double shiftHours) {
        double hourWages =0;
        if(task.getWorkDone()) {
            double breakHours = time / 60.0;
            double wagesPerHour = task.getWagesPerDay() / shiftHours;
            hourWages = breakHours * wagesPerHour;
        }
        return hourWages;
    }

    private double getActualTaskTime(double time, Long id) {
        double totalBreakMinutes = taskRepository.getSumOfBreakTime(id);
        double actualTaskTime = Precision.round(time - totalBreakMinutes, 2);
        return actualTaskTime;
    }

    private void saveTaskDetail(TaskMaster savedTaskMaster, Employee employee, String taskStatus) {
        try {
            TaskDetail taskDetail = new TaskDetail();
            taskDetail.setTaskMaster(savedTaskMaster);
            taskDetail.setEmployee(employee);
            taskDetail.setTaskDate(LocalDate.now());
            taskDetail.setRemark(savedTaskMaster.getRemark());
            taskDetail.setCreatedAt(LocalDateTime.now());
            taskDetail.setCreatedBy(employee.getId());
            taskDetail.setStatus(true);

            TaskDetail taskDetail1 = taskDetailRepository.save(taskDetail);
            if (taskDetail1 != null) {
                System.out.println("taskdetail saved");
            } else {
                System.out.println("failed to save taskdetail");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("exception " + e.getMessage());
        }
    }

    public JsonObject fetchEmployeeTasks(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            System.out.println("employee.getId() " + employee.getId());

            System.out.println("LocalDate.now() " + LocalDate.now() + " " + requestParam.get("attendanceId"));
            if (!requestParam.get("attendanceId").equalsIgnoreCase("")) {
                Long attendanceId = Long.valueOf(requestParam.get("attendanceId"));
                int taskType = 1;
                if (requestParam.containsKey("taskType")) {
                    taskType = Integer.parseInt(requestParam.get("taskType"));
                }
                List<TaskMaster> taskList =
                        taskRepository.findByEmployeeIdAndAttendanceIdAndTaskTypeAndStatusOrderByIdDesc(employee.getId(),
                                attendanceId, taskType, true);

                System.out.println("taskList.size() " + taskList.size());
                if (taskList.size() > 0) {
                    for (TaskMaster task : taskList) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("taskId", task.getId());
                        jsonObject.addProperty("taskType", task.getTaskType());
                        jsonObject.addProperty("taskDate", task.getTaskDate().toString());
                        jsonObject.addProperty("taskMasterStatus", task.getTaskStatus());
                        jsonObject.addProperty("itemName", "NA");
                        if (task.getJob() != null) {
                            jsonObject.addProperty("itemName", task.getJob().getJobName());
                        }

                        jsonObject.addProperty("jobOperationName", "NA");
                        JsonArray docArray = new JsonArray();
                        jsonObject.add("jobDocument", docArray);
//                        jsonObject.addProperty("procedureSheet", "NA");
                        if (task.getJobOperation() != null) {
                            jsonObject.addProperty("jobOperationName", task.getJobOperation().getOperationName());
                            if (task.getJobOperation().getOperationImagePath() != null) {
//                                jsonObject.addProperty("jobDocument", serverUrl + task.getJobOperation().getOperationImagePath());
                                docArray.add(serverUrl + task.getJobOperation().getOperationImagePath());
                            }
                            if (task.getJobOperation().getProcedureSheet() != null) {
                                docArray.add(serverUrl + task.getJobOperation().getProcedureSheet());
                                /*jsonObject.addProperty("procedureSheet",
                                        serverUrl + task.getJobOperation().getProcedureSheet());*/
                            }
                            jsonObject.add("jobDocument", docArray);
                        }

                        jsonObject.addProperty("machineName", "NA");
                        jsonObject.addProperty("isMachineCount", false);
                        if (task.getMachine() != null) {
                            jsonObject.addProperty("machineName", task.getMachine().getName());
                            jsonObject.addProperty("isMachineCount", task.getMachine().getIsMachineCount());
                        }
                        jsonObject.addProperty("machineStartCount", task.getMachineStartCount());
                        jsonObject.addProperty("machineEndCount", task.getMachineEndCount());
                        jsonObject.addProperty("averagePerShift", task.getAveragePerShift() != null ? Precision.round(task.getAveragePerShift(), 2) : 0);
                        jsonObject.addProperty("pcsRate", task.getPcsRate() != null ? Precision.round(task.getPcsRate(), 2) : 0);
                        jsonObject.addProperty("totalCount", task.getTotalCount());
                        jsonObject.addProperty("jobsPerHour", task.getJobsPerHour() != null ? Precision.round(task.getJobsPerHour(), 2) : 0);
                        jsonObject.addProperty("netJobsInPercentage", task.getPercentageOfTask() != null ? Precision.round(task.getPercentageOfTask(), 2) : 0);

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                        jsonObject.addProperty("startTime",
                                task.getStartTime() != null ? formatter.format(task.getStartTime()) : "");
                        jsonObject.addProperty("endTime", task.getEndTime() != null ? formatter.format(task.getEndTime()) : "");
                        if (task.getPercentageOfTask() != null && task.getPercentageOfTask() >= 100) {
                            jsonObject.addProperty("taskStatus", "Achieved");
                        } else {
                            jsonObject.addProperty("taskStatus", "Not-Achieved");
                        }
                        jsonArray.add(jsonObject);
                    }
                }
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            taskLogger.error("fetchEmployeeTasks " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonObject fetchEmployeeTodaysTaskList(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            List<TaskMaster> taskList = taskRepository.findEmployeeTaskListForToday(Long.parseLong(requestParam.get("employeeId")), LocalDate.now(), 1, "in-progress", true);
            if (taskList.size() > 0) {
                for (TaskMaster task : taskList) {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("taskId", task.getId());
                    jsonObject.addProperty("itemId", "NA");
                    jsonObject.addProperty("itemName", "NA");
                    if (task.getJob() != null) {
                        jsonObject.addProperty("itemName", task.getJob().getJobName());
                        jsonObject.addProperty("itemId", task.getJob().getId());
                    }
                    jsonObject.addProperty("jobOperationName", "NA");
                    jsonObject.addProperty("jobOperationId", "NA");
                    if (task.getJobOperation() != null) {
                        jsonObject.addProperty("jobOperationName", task.getJobOperation().getOperationName());
                        jsonObject.addProperty("jobOperationId", task.getJobOperation().getId());
                    }
                    jsonArray.add(jsonObject);
                }
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
            response.addProperty("message", "Task List fetched Successfully");
        } catch (Exception e) {
            taskLogger.error("fetchEmployeeTasks " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonObject getEmployeeTodayTasks(Map<String, String> request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Long employeeId = Long.valueOf(request.get("employeeId"));
            Long attendanceId = Long.valueOf(request.get("attendanceId"));


            List<Object[]> taskViewList = taskViewRepository.findDataGroupByOperationView(employeeId, attendanceId, "1", true);
            for (int i = 0; i < taskViewList.size(); i++) {

                Object[] taskObj = taskViewList.get(i);
                Double breakdata = taskViewRepository.findBreakTimeByJobOperationId(taskObj[9].toString());

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("machineName", taskObj[0].toString());
                jsonObject.addProperty("jobName", taskObj[1].toString());
                jsonObject.addProperty("operationName", taskObj[2].toString());
                jsonObject.addProperty("cycleTime", taskObj[3].toString());
                jsonObject.addProperty("okQty", taskObj[4].toString());
                jsonObject.addProperty("reworkQty", taskObj[5].toString());
                jsonObject.addProperty("machineRejectQty", taskObj[6].toString());
                jsonObject.addProperty("totalTime", Precision.round(Double.parseDouble(taskObj[7].toString()), 2));
                jsonObject.addProperty("totalCount", taskObj[8].toString());
                jsonObject.addProperty("jobOperationId", taskObj[9].toString());
                jsonObject.addProperty("breakTime", breakdata);
                jsonArray.add(jsonObject);

            }
            responseMessage.add("response", jsonArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());

        } catch (Exception e) {
            taskLogger.error("getEmployeeTodayTasks " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load Data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }


    public Object getEmployeeOperationView(Map<String, String> request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Long jobOperationId = Long.valueOf(request.get("jobOperationId"));
            Long attendanceId = Long.valueOf(request.get("attendanceId"));


            List<TaskView> taskViewList = taskViewRepository.findByAttendanceIdAndJobOperationId(attendanceId, jobOperationId);

            if (taskViewList.size() > 0) {
                List<TaskDTO> taskDTOList = new ArrayList<>();
                for (TaskView taskView : taskViewList) {
                    taskDTOList.add(convertToDTO(taskView));
                }
                responseMessage.setResponse(taskDTOList);
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseMessage.setMessage("No tasks available");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }

        } catch (Exception e) {
            taskLogger.error("getEmployeeTodayTasks " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setResponse("Failed to load Data");
            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public TaskDTO convertToDTO(TaskView taskView) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(taskView.getId());
        taskDTO.setEmployeeId(taskView.getEmployeeId());
        taskDTO.setEmployeeName(taskView.getEmployeeName());
        if (taskView.getTaskDate() != null)
            taskDTO.setTaskDate(String.valueOf(taskView.getTaskDate()));
        if (taskView.getStartTime() != null)
            taskDTO.setStartTime(String.valueOf(taskView.getStartTime()));
        if (taskView.getEndTime() != null)
            taskDTO.setEndTime(String.valueOf(taskView.getEndTime()));
        taskDTO.setTotalTime(taskView.getTotalTime());
        taskDTO.setActualWorkTime(Precision.round(taskView.getActualWorkTime(), 2));
        taskDTO.setTaskType(taskView.getTaskType());
        taskDTO.setWorkDone(taskView.getWorkDone());
        taskDTO.setRemark(taskView.getRemark());
        taskDTO.setCreatedBy(taskView.getCreatedBy());
        taskDTO.setUpdatedBy(taskView.getUpdatedBy());
        if (taskView.getCreatedAt() != null)
            taskDTO.setCreatedAt(String.valueOf(taskView.getCreatedAt()));
        if (taskView.getUpdatedAt() != null)
            taskDTO.setUpdatedAt(String.valueOf(taskView.getUpdatedAt()));
        taskDTO.setStatus(taskView.getStatus());
        taskDTO.setWorkBreakId(taskView.getWorkBreakId());
        taskDTO.setBreakName(taskView.getBreakName());
        taskDTO.setMachineId(taskView.getMachineId());
        taskDTO.setMachineName(taskView.getMachineName());
        taskDTO.setMachineNumber(taskView.getMachineNumber());
        taskDTO.setJobId(taskView.getJobId());
        taskDTO.setJobName(taskView.getJobName());
        taskDTO.setJobOperationId(taskView.getJobOperationId());
        taskDTO.setOperationName(taskView.getOperationName());

        taskDTO.setCycleTime(Precision.round(taskView.getCycleTime(), 2));
        taskDTO.setPcsRate(Precision.round(taskView.getPcsRate(), 2));
        taskDTO.setAveragePerShift(Precision.round(taskView.getAveragePerShift(), 2));
        taskDTO.setPointPerJob(Precision.round(taskView.getPerJobPoint(), 2));

        taskDTO.setMachineStartCount(taskView.getMachineStartCount());
        taskDTO.setMachineEndCount(taskView.getMachineEndCount());
        taskDTO.setTotalCount(taskView.getTotalCount());

        taskDTO.setJobsPerHour(Precision.round(taskView.getJobsPerHour(), 2));
        taskDTO.setWorkHours(Precision.round(taskView.getWorkingHour(), 2));

        taskDTO.setRequiredProduction(Precision.round(taskView.getRequiredProduction(), 2));
        taskDTO.setActualProduction(Precision.round(taskView.getActualProduction(), 2));
        taskDTO.setShortProduction(Precision.round(taskView.getShortProduction(), 2));
        taskDTO.setPercentageOfTask(Precision.round(taskView.getPercentageOfTask(), 2));

        taskDTO.setWagesPoint(taskView.getWagesPerPoint());
        taskDTO.setWorkPoint(taskView.getTotalPoint());
        taskDTO.setWagesPerDay(taskView.getWagesPerDay());
        taskDTO.setWagesPerHour(taskView.getWagesPerHour());
        taskDTO.setWagesPerMinute(taskView.getWagesPerMinute());

        taskDTO.setWagesPointBasis(taskView.getWagesPointBasis() != null ? Precision.round(taskView.getWagesPointBasis(), 2) : 0.0);
        taskDTO.setWagesHourBasis(taskView.getWagesHourBasis() != null ? Precision.round(taskView.getWagesHourBasis(), 2) : 0.0);
        taskDTO.setWagesMinutesBasis(taskView.getWagesMinutesBasis() != null ? Precision.round(taskView.getWagesMinutesBasis(), 2) : 0.0);
        taskDTO.setWagesPcsBasis(taskView.getWagesPcsBasis() != null ? Precision.round(taskView.getWagesPcsBasis(), 2) : 0.0);

        taskDTO.setOkQty(taskView.getOkQty());
        taskDTO.setReworkQty(taskView.getReworkQty());
        taskDTO.setMachineRejectQty(taskView.getMachineRejectQty());
        taskDTO.setDoubtfulQty(taskView.getDoubtfulQty());
        taskDTO.setUnMachinedQty(taskView.getUnMachinedQty());

        taskDTO.setCorrectiveAction(taskView.getCorrectiveAction());
        taskDTO.setPreventiveAction(taskView.getPreventiveAction());
        return taskDTO;
    }

    public JsonObject convertToJSonObj(TaskView taskView) {
        JsonObject taskDTO = new JsonObject();
        taskDTO.addProperty("id", taskView.getId());
        taskDTO.addProperty("employeeId", taskView.getEmployeeId());
        taskDTO.addProperty("employeeName", taskView.getEmployeeName());
        taskDTO.addProperty("taskDate", taskView.getTaskDate() != null ? String.valueOf(taskView.getTaskDate()) : "");
        taskDTO.addProperty("startTime", taskView.getStartTime() != null ? String.valueOf(taskView.getStartTime()) : "");
        taskDTO.addProperty("endTime", taskView.getEndTime() != null ? String.valueOf(taskView.getEndTime()) : "");
        taskDTO.addProperty("totalTime", taskView.getTotalTime() != null ?
                Precision.round(taskView.getTotalTime(), 2) : 0);
        taskDTO.addProperty("actualWorkTime", taskView.getActualWorkTime() != null ?
                Precision.round(taskView.getActualWorkTime(), 2) : 0);
        taskDTO.addProperty("taskType", taskView.getTaskType());
        taskDTO.addProperty("workDone", taskView.getWorkDone());
        taskDTO.addProperty("remark", taskView.getRemark());
        taskDTO.addProperty("createdBy", taskView.getCreatedBy());
        taskDTO.addProperty("updatedBy", taskView.getUpdatedBy());
        if (taskView.getCreatedAt() != null)
            taskDTO.addProperty("createdAt", String.valueOf(taskView.getCreatedAt()));
        if (taskView.getUpdatedAt() != null)
            taskDTO.addProperty("updatedAt", String.valueOf(taskView.getUpdatedAt()));
        taskDTO.addProperty("status", taskView.getStatus());
        taskDTO.addProperty("workBreakId", taskView.getWorkBreakId());
        taskDTO.addProperty("breakName", taskView.getBreakName());
        taskDTO.addProperty("machineId", taskView.getMachineId());
        taskDTO.addProperty("machineName", taskView.getMachineName());
        taskDTO.addProperty("machineNumber", taskView.getMachineNumber());
        taskDTO.addProperty("jobId", taskView.getJobId());
        taskDTO.addProperty("jobName", taskView.getJobName());
        taskDTO.addProperty("jobOperationId", taskView.getJobOperationId());
        taskDTO.addProperty("operationName", taskView.getOperationName());

        taskDTO.addProperty("cycleTime", taskView.getCycleTime());
        taskDTO.addProperty("PcsRate", taskView.getPcsRate());
        taskDTO.addProperty("averagePerShift", taskView.getAveragePerShift());
        taskDTO.addProperty("pointPerJob", taskView.getPerJobPoint());

        taskDTO.addProperty("machineStartCount", taskView.getMachineStartCount());
        taskDTO.addProperty("machineEndCount", taskView.getMachineEndCount());
        taskDTO.addProperty("totalCount", taskView.getTotalCount());

        taskDTO.addProperty("jobsPerHour", taskView.getJobsPerHour());
        taskDTO.addProperty("workHours", taskView.getWorkingHour());

        taskDTO.addProperty("requiredProduction", taskView.getRequiredProduction() != null ? Precision.round(taskView.getRequiredProduction(), 2) : 0);
        taskDTO.addProperty("actualProduction", taskView.getActualProduction() != null ? Precision.round(taskView.getActualProduction(), 2) : 0);
        taskDTO.addProperty("shortProduction", taskView.getShortProduction() != null ? Precision.round(taskView.getShortProduction(), 2) : 0);
        taskDTO.addProperty("percentageOfTask", taskView.getPercentageOfTask() != null ? Precision.round(taskView.getPercentageOfTask(), 2) : 0);

        taskDTO.addProperty("wagesPoint", taskView.getWagesPerPoint());
        taskDTO.addProperty("workPoint", taskView.getTotalPoint());
        taskDTO.addProperty("wagesPerDay", taskView.getWagesPerDay());
        taskDTO.addProperty("wagesPerHour", taskView.getWagesPerHour());
        taskDTO.addProperty("wagesPerMinute", taskView.getWagesPerMinute());

        taskDTO.addProperty("wagesPointBasis", taskView.getWagesPointBasis() != null ? Precision.round(taskView.getWagesPointBasis(), 2) : 0.0);
        taskDTO.addProperty("wagesHourBasis", taskView.getWagesHourBasis() != null ? Precision.round(taskView.getWagesHourBasis(), 2) : 0.0);
        taskDTO.addProperty("wagesMinutesBasis", taskView.getWagesMinutesBasis() != null ? Precision.round(taskView.getWagesMinutesBasis(), 2) : 0.0);
        taskDTO.addProperty("wagesPcsBasis", taskView.getWagesPcsBasis() != null ? Precision.round(taskView.getWagesPcsBasis(), 2) : 0.0);
        taskDTO.addProperty("breakWages", taskView.getBreakWages() != null ? Precision.round(taskView.getBreakWages(), 2) : 0.0);

        taskDTO.addProperty("okQty", taskView.getOkQty());
        taskDTO.addProperty("reworkQty", taskView.getReworkQty());
        taskDTO.addProperty("machineRejectQty", taskView.getMachineRejectQty());
        taskDTO.addProperty("doubtfulQty", taskView.getDoubtfulQty());
        taskDTO.addProperty("unMachinedQty", taskView.getUnMachinedQty());
        return taskDTO;
    }

    public void updateEmployeeTaskSummary(Attendance attendance) {
        try {

            LocalTime totalTime = LocalTime.parse("00:00:00");
            LocalDateTime firstTaskStartTime = null;
            LocalDateTime lastTaskEndTime = null;

            double actualWorkTime = taskRepository.getSumOfActualWorkTime(attendance.getId());
            attendance.setActualWorkTime(actualWorkTime);
            double lunchTimeInMin = 0;
            LocalTime lunchTime = null;

            int s = 0;
            int attendanceSec = 0;
            double totalMinutes = 0;
            double attendanceMinutes = 0;

            if (attendance.getTotalTime() != null) {
                if (attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l3") ||
                        attendance.getEmployee().getDesignation().getCode().equalsIgnoreCase("l2")) {
                    /*From Task Data*/
                    firstTaskStartTime = taskRepository.getInTime(attendance.getId());
                    System.out.println("firstTaskStartTime =>>>>>>>>>>>>>>>>>>>>>>" + firstTaskStartTime);
                    lastTaskEndTime = taskRepository.getOutTime(attendance.getId());
                    System.out.println("lastTaskEndTime =>>>>>>>>>>>>>>>>>>>>>>" + lastTaskEndTime);
                    if (firstTaskStartTime != null && lastTaskEndTime != null && !firstTaskStartTime.equals("null") && !lastTaskEndTime.equals("null")) {
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

                WorkBreak workBreak = workBreakRepository.findByBreakName(attendance.getInstitute().getId());
                TaskMaster taskMaster = null;
                if (workBreak != null) {
                    lunchTimeInMin = taskRepository.getSumOfLunchTime(attendance.getId(), workBreak.getId(), true, false);
                    /*taskMaster = taskRepository.findByAttendanceIdAndWorkBreakIdAndStatusAndWorkDone(attendance.getId(), workBreak.getId(), true, false);
                    if (taskMaster != null) {
                        lunchTimeInMin = taskMaster.getTotalTime();
                        lunchTime = taskMaster.getWorkingTime();
                    }*/
                }
                System.out.println("attendance.getLunchTime() >>>>>>>>>>>>>>" + attendance.getLunchTime());
                lunchTimeInMin = attendance.getLunchTime() > 0 ? attendance.getLunchTime() : lunchTimeInMin;
                System.out.println("lunchTimeInMin >>>>>>>>>>>>>>" + lunchTimeInMin);
                double totalAllBreakMinutes = taskRepository.getSumOfAllBreaksWithNotWorking(attendance.getId(), 0, workBreak.getId());
                System.out.println("totalAllBreakMinutes >>>>>>>>>>>>>>" + totalAllBreakMinutes);

                // OLD CODE s = (int) SECONDS.between(attendance.getCheckInTime(), attendance.getCheckOutTime());
                s = (int) SECONDS.between(firstTaskStartTime, lastTaskEndTime);
                attendanceSec = Math.abs(s);
                System.out.println("attendanceSec " + attendanceSec);
                attendanceMinutes = (attendanceSec / 60.0);
                totalMinutes = attendanceMinutes - lunchTimeInMin - totalAllBreakMinutes;

                System.out.println("attendanceMinutes " + attendanceMinutes);
                System.out.println("totalMinutes " + totalMinutes);

                double workingHours = totalMinutes > 0 ? (totalMinutes / 60.0) : 0;
                double wagesHourBasis = attendance.getWagesPerHour() * workingHours;
                System.out.println("workingHours " + workingHours);
                System.out.println("wagesPerHour " + attendance.getWagesPerHour());
                System.out.println("wagesHourBasis " + wagesHourBasis);

                attendance.setWorkingHours(workingHours);
                attendance.setWagesHourBasis(wagesHourBasis);
                attendance.setLunchTime(lunchTimeInMin);
            }

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
                attendanceRepository.save(attendance);
            }
        } catch (Exception e) {
            taskLogger.error("updateEmployeeTaskSummary " + e);
            System.out.println("e--------> " + e.getMessage());
            e.printStackTrace();
        }

        /* OLD CODE commented*/
        /*AttendanceView attendanceView = attendanceViewRepository.findByEmployeeIdAndId(employee.getId(),
                attendance1.getId());
        System.out.println("attendanceView " + attendanceView.toString());
        if (attendanceView != null) {
        } else {
            AttendanceView oldAttendanceView =
                    attendanceViewRepository.findTop1ByEmployeeIdOrderByIdDesc(employee.getId());
            attendanceView = oldAttendanceView;
        }

        try {

            String taskWagesData = taskRepository.getTaskWagesData(attendance.getId());
//                System.out.println("attendance " + attendance);
            attendance.setTotalProdQty(attendanceView.getTotalProdQty());
            attendance.setTotalWorkTime(Precision.round(attendanceView.getTotalWorkTime(), 2));
            attendance.setTotalWorkPoint(Precision.round(attendanceView.getTotalWorkPoint(), 2));

            attendance.setWagesPointBasis(Precision.round(attendanceView.getWagesPointBasis(), 2));
            attendance.setWagesHourBasis(Precision.round(attendanceView.getWagesHourBasis(), 2));
            attendance.setWagesMinBasis(Precision.round(attendanceView.getWagesMinuteBasis(), 2));
            attendance.setWagesPcsBasis(Precision.round(attendanceView.getWagesPcsBasis(), 2));

            attendance.setActualProduction(attendanceView.getActualProduction());
            attendance.setReworkQty(attendanceView.getReworkQty());
            attendance.setMachineRejectQty(attendanceView.getMachineRejectQty());
            attendance.setDoubtfulQty(attendanceView.getDoubtfulQty());
            attendance.setUnMachinedQty(attendanceView.getUnMachinedQty());
            attendance.setOkQty(attendanceView.getOkQty());
            attendanceRepository.save(attendance);
        } catch (Exception e) {
            taskLogger.error("updateEmployeeTaskSummary " + e);
            System.out.println("e--------> " + e.getMessage());
            e.printStackTrace();
        }*/
    }

    public JsonObject fetchEmployeeTasksDetail(Map<String, String> jsonRequest) {
        JsonObject response = new JsonObject();
        try {
            Long taskId = Long.valueOf(jsonRequest.get("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);

            JsonObject jsonObject = new JsonObject();
            if (task != null) {
                String newTaskDetailId = "0";
                String taskStatus = "";
                String breakStatus = "";
                Boolean breakInTask = false;
//                TaskDetail taskDetailNew =
//                        taskDetailRepository.findTop1ByTaskMasterIdOrderByIdDesc(
//                                task.getId());

                TaskMaster taskInBreakObj = taskRepository.findByEmployeeIdAndAttendanceIdAndTaskTypeAndStartTimeNotNullAndEndTimeNull(
                        task.getEmployee().getId(), task.getAttendance().getId(), 2);

                if (taskInBreakObj != null) {
                    breakInTask = true;
                }

                TaskMaster taskDetailNew =
                        taskRepository.findTop1ByTaskMasterIdOrderByIdDesc(
                                task.getId());
                if (taskDetailNew != null) {
//                    TaskMaster taskDetailNew1 =
//                            taskRepository.findTop1ByTaskMasterIdAndEndTimeNotNullOrderByIdDesc(
//                                    task.getId());
                    if (taskDetailNew.getEndTime() != null) {
                        breakStatus = "end";
                    } else {
                        breakStatus = "start";
                    }
                    newTaskDetailId = taskDetailNew.getId().toString();
                } else {
                    breakStatus = "end";
                }

                jsonObject.addProperty("machineId", 0);
                jsonObject.addProperty("itemId", 0);
                jsonObject.addProperty("taskId", task.getId());
                jsonObject.addProperty("taskDetailId", newTaskDetailId);
                jsonObject.addProperty("taskMasterStatus", task.getTaskStatus());
                jsonObject.addProperty("breakStatus", breakStatus);
                jsonObject.addProperty("breakInTask", breakInTask);

                response.add("response", jsonObject);
                jsonObject.addProperty("itemName", "NA");
                if (task.getJob() != null) {
                    jsonObject.addProperty("itemName", task.getJob().getJobName());
                }
                if (task.getMachine() != null) {
                    jsonObject.addProperty("machineId", task.getMachine().getId());
                }
                if (task.getJob() != null) {
                    jsonObject.addProperty("itemId", task.getJob().getId());
                }

                jsonObject.addProperty("jobOperationName", "NA");
                jsonObject.addProperty("cycleTime", 0);

                JsonArray docArray = new JsonArray();
                jsonObject.add("jobDocument", docArray);
//                jsonObject.addProperty("procedureSheet", "NA");

                if (task.getJobOperation() != null) {
                    jsonObject.addProperty("cycleTime", Precision.round(task.getCycleTime(), 2));
                    jsonObject.addProperty("jobOperationName", task.getJobOperation().getOperationName());
                    jsonObject.addProperty("jobOperationId", task.getJobOperation().getId());
                    if (task.getJobOperation().getOperationImagePath() != null) {
//                                jsonObject.addProperty("jobDocument", serverUrl + task.getJobOperation().getOperationImagePath());
                        docArray.add(serverUrl + task.getJobOperation().getOperationImagePath());
                    }
                    if (task.getJobOperation().getProcedureSheet() != null) {
                        docArray.add(serverUrl + task.getJobOperation().getProcedureSheet());
                                /*jsonObject.addProperty("procedureSheet",
                                        serverUrl + task.getJobOperation().getProcedureSheet());*/
                    }
                    jsonObject.add("jobDocument", docArray);
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

                jsonObject.addProperty("machineName", task.getMachine() != null ? task.getMachine().getName() : "NA");
                jsonObject.addProperty("machineNo", task.getMachine() != null ? task.getMachine().getNumber() : "NA");
                jsonObject.addProperty("startTime", formatter.format(task.getStartTime()));
                jsonObject.addProperty("endTime", task.getEndTime() != null ?
                        formatter.format(task.getEndTime()) : "NA");
                jsonObject.addProperty("machineStartCount", task.getMachineStartCount());
                jsonObject.addProperty("machineEndCount", task.getMachineEndCount());
                jsonObject.addProperty("averagePerShift", task.getAveragePerShift() != null ? Precision.round(task.getAveragePerShift(), 2) : 0);
                jsonObject.addProperty("pcsRate", task.getPcsRate() != null ? Precision.round(task.getPcsRate(), 2) : 0);
                jsonObject.addProperty("totalCount", task.getTotalCount());

                jsonObject.addProperty("okQty", task.getOkQty());
                jsonObject.addProperty("reworkQty", task.getReworkQty());
                jsonObject.addProperty("machineRejectQty", task.getMachineRejectQty());
                jsonObject.addProperty("unMachinedQty", task.getUnMachinedQty());
                jsonObject.addProperty("doubtfulQty", task.getDoubtfulQty());

                jsonObject.addProperty("settingTimeHr", task.getSettingTimeInHour() != null ? Precision.round(task.getSettingTimeInHour(), 2) : 0);
                jsonObject.addProperty("jobsPerHour", task.getJobsPerHour() != null ? Precision.round(task.getJobsPerHour(), 2) : 0);
                jsonObject.addProperty("employeeJobsCount", task.getRequiredProduction() != null ? Precision.round(task.getRequiredProduction(), 2) : 0);
                jsonObject.addProperty("netJobsInPercentage", task.getPercentageOfTask() != null ? Precision.round(task.getPercentageOfTask(), 2) : 0);
                jsonObject.addProperty("percentageOfTask", task.getPercentageOfTask() != null ? Precision.round(task.getPercentageOfTask(), 2) : 0);
                if (task.getPercentageOfTask() != null && task.getPercentageOfTask() >= 100) {
                    jsonObject.addProperty("taskStatus", "Complete");
                } else {
                    jsonObject.addProperty("taskStatus", "Incomplete");
                }
                jsonObject.addProperty("correctiveAction", task.getCorrectiveAction() != null ? task.getCorrectiveAction() : "NA");
                jsonObject.addProperty("preventiveAction", task.getPreventiveAction() != null ? task.getPreventiveAction() : "NA");
                jsonObject.addProperty("remark", task.getRemark() != null ? task.getRemark() : "NA");
                jsonObject.addProperty("endRemark", task.getEndRemark() != null ? task.getEndRemark() : "NA");
                jsonObject.addProperty("adminRemark", task.getAdminRemark() != null ? task.getAdminRemark() : "NA");

                response.add("response", jsonObject);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            }
        } catch (Exception e) {
            taskLogger.error("fetchEmployeeTasksDetail " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object startBreakInTask(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            Long taskMasterId = Long.valueOf(requestParam.get("taskId"));
            TaskMaster taskMaster = taskRepository.findByIdAndStatus(taskMasterId, true);

            if (taskMaster != null) {
                TaskDetail taskDetail = new TaskDetail();

                Long breakId = Long.valueOf(requestParam.get("breakId"));
                WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);

                taskDetail.setWorkBreak(workBreak);
                taskDetail.setTaskMaster(taskMaster);
                taskDetail.setEmployee(employee);
                taskDetail.setTaskDate(LocalDate.now());
                taskDetail.setStartTime(LocalTime.now());
                taskDetail.setCreatedAt(LocalDateTime.now());
                taskDetail.setCreatedBy(employee.getId());
                taskDetail.setInstitute(employee.getInstitute());
                taskDetail.setStatus(true);
                taskDetail.setWorkDone(Boolean.parseBoolean(requestParam.get("workDone")));

                String remark = requestParam.get("remark");
                if (remark != null && !remark.equalsIgnoreCase("")) {
                    taskDetail.setRemark(remark);
                }

                try {
                    TaskDetail savedTaskDetail = taskDetailRepository.save(taskDetail);
                    responseMessage.setResponse(savedTaskDetail.getId());
                    responseMessage.setMessage("Break added successfully");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to add break");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Task not found");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to add task");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject endBreakInTask(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            Long taskDetailId = Long.valueOf(requestParam.get("taskDetailId"));
            TaskDetail savedTaskDetail = taskDetailRepository.findByIdAndStatus(taskDetailId, true);

            if (savedTaskDetail != null) {
                TaskMaster taskMaster = savedTaskDetail.getTaskMaster();
                LocalTime shiftTime = employee.getShift().getWorkingHours();
                System.out.println("shiftTime " + shiftTime);
                Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
                System.out.println("shiftMinutes in min " + shiftMinutes);
                Double shiftHours = Double.valueOf((shiftMinutes / 60));

                LocalTime l1 = savedTaskDetail.getStartTime();
                LocalTime l2 = LocalTime.now();
                savedTaskDetail.setEndTime(l2);

                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                Long time = Long.valueOf(String.format("%.0f", totalTime));
                System.out.println("total time in min " + time);

                savedTaskDetail.setTotalTime(Double.valueOf(time));
                savedTaskDetail.setWorkBreak(savedTaskDetail.getWorkBreak());
                savedTaskDetail.setWorkDone(savedTaskDetail.getWorkDone());

                if (savedTaskDetail.getWorkDone() != null && savedTaskDetail.getWorkDone()) {
                    Double workPoints = ((Double.valueOf("100") / shiftMinutes) * time);
                    System.out.println("workPoints " + workPoints);

                    Double wagesPointBasis = workPoints * workPoints;
                    Double wagesPerMin = (taskMaster.getWagesPerDay() / shiftMinutes);
                    Double wagesHourBasis = (wagesPerMin * time);

                    savedTaskDetail.setWorkPoint(workPoints);
                    savedTaskDetail.setWorkPoint(Precision.round(totalTime, 2));
                    savedTaskDetail.setWagesPointBasis(Precision.round(wagesPointBasis, 2));
                    savedTaskDetail.setWagesHourBasis(Precision.round(wagesHourBasis, 2));
                }
                savedTaskDetail.setUpdatedAt(LocalDateTime.now());
                savedTaskDetail.setUpdatedBy(employee.getId());
                savedTaskDetail.setInstitute(employee.getInstitute());
                try {
                    taskDetailRepository.save(savedTaskDetail);
                    responseMessage.addProperty("message", "Break end successfully");
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.addProperty("message", "Failed to end break");
                    responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.addProperty("message", "Task not found");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to end task");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        System.out.println("Response  " + responseMessage);
        return responseMessage;
    }

    public JsonObject fetchEmployeeSettingTimes(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
            System.out.println("employee.getId() " + employee.getId());

            if (!requestParam.get("attendanceId").equalsIgnoreCase("")) {
                Long attendanceId = Long.valueOf(requestParam.get("attendanceId"));
                List<TaskMaster> taskList =
                        taskRepository.findByEmployeeIdAndAttendanceIdAndTaskTypeNotAndStatusOrderByIdDesc(employee.getId(),
                                attendanceId, 1, true);
                if (taskList.size() > 0) {
                    for (TaskMaster task : taskList) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("taskId", task.getId());
                        jsonObject.addProperty("taskType", task.getTaskType());
                        jsonObject.addProperty("taskMasterStatus", task.getTaskStatus());
                        if(task.getTaskStatus().equalsIgnoreCase("in-progress"))
                            jsonObject.addProperty("totalBreakTime", "0.0");
                        else
                            jsonObject.addProperty("totalBreakTime", task.getTotalTime());
                        if (task.getWorkBreak() != null) {
                            jsonObject.addProperty("workBreakName", task.getWorkBreak().getBreakName());
                            String workBreakPaid = "Un-Paid";
//                        if(task.getWorkBreak().getIsBreakPaid() == true)
                            if (task.getWorkDone())
                                workBreakPaid = "Paid";
                            jsonObject.addProperty("workBreakPaid", workBreakPaid);
                        } else {
                            jsonObject.addProperty("workBreakName", "NA");
                            jsonObject.addProperty("workBreakPaid", "NA");
                        }
                        if (task.getWorkDone() != null) {
                            jsonObject.addProperty("workDone", task.getWorkDone());
                        } else {
                            jsonObject.addProperty("workDone", "NA");
                        }

                        jsonObject.addProperty("startTime", String.valueOf(task.getStartTime()));
                        jsonObject.addProperty("endTime", String.valueOf(task.getEndTime()));
                        LocalTime timeDiff = null;
                        if(task.getEndTime() != null){
                            timeDiff = utility.getDateTimeDiffInTime(task.getStartTime(),task.getEndTime());
                        }
                        jsonObject.addProperty("totalBreak", timeDiff != null ? timeDiff.toString() : "");
                        jsonObject.addProperty("remark", task.getRemark());
                        jsonObject.addProperty("endRemark", task.getEndRemark());
                        jsonObject.addProperty("isBreakInTask", false);
                        if (task.getTaskMaster() != null) {
                            jsonObject.addProperty("isBreakInTask", true);
                        }
                        jsonArray.add(jsonObject);
                    }
                }
            }
            response.add("response", jsonArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            taskLogger.error("fetchEmployeeSettingTimes " + e);
            System.out.println("exception  " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public boolean copyTask(TaskMaster oldTask) {
        try {
            TaskMaster task = new TaskMaster();
            task.setAttendance(oldTask.getAttendance());
            task.setTaskDate(LocalDate.now());
            task.setEmployee(oldTask.getEmployee());
            task.setTaskType(oldTask.getTaskType());
            task.setWorkDone(true);
            task.setTaskStatus("in-progress");

            Double wagesPerDay = utility.getEmployeeWages(task.getEmployee().getId());
            System.out.println("employee wagesPerDay =" + wagesPerDay);
            if (wagesPerDay == null) {
                return false;
            } else {
                double wagesPerPoint = (wagesPerDay / 100.0);
                task.setWagesPerDay(wagesPerDay);
                task.setWagesPerPoint(wagesPerPoint);
                task.setEmployeeWagesType(oldTask.getEmployeeWagesType());

                LocalDateTime startTime = LocalDateTime.now();
                task.setStartTime(startTime);
                // save Task
                task.setMachine(oldTask.getMachine());
                task.setJob(oldTask.getJob());
                task.setJobOperation(oldTask.getJobOperation());
                task.setCycleTime(oldTask.getCycleTime());
                task.setAveragePerShift(oldTask.getAveragePerShift());
                task.setPcsRate(oldTask.getPcsRate());
                task.setPerJobPoint(oldTask.getPerJobPoint());

                if (oldTask.getMachine().getIsMachineCount()) {
                    task.setMachineStartCount(oldTask.getMachine().getCurrentMachineCount());
                }

                task.setCreatedAt(LocalDateTime.now());
                task.setCreatedBy(oldTask.getEmployee().getId());
                task.setInstitute(oldTask.getInstitute());
                task.setStatus(true);
                try {
                    taskRepository.save(task);
                    return true;
                } catch (Exception e) {
                    taskLogger.error("copyTask " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            taskLogger.error("copyTask " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            return false;
        }
    }

    public JsonObject getTaskDetailsForUpdate(Map<String, String> request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Long taskId = Long.valueOf(request.get("taskId"));
            TaskMaster taskMaster = taskRepository.findByIdAndStatusOrderByIdDesc(taskId, true);

            if (taskMaster != null) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("taskId", taskMaster.getId());
                jsonObject.addProperty("taskDate", taskMaster.getTaskDate().toString());
                jsonObject.addProperty("employeeId", taskMaster.getEmployee().getId());
                jsonObject.addProperty("attendanceId", taskMaster.getAttendance().getId());
                jsonObject.addProperty("taskType", taskMaster.getTaskType());
                jsonObject.addProperty("employeeName", utility.getEmployeeName(taskMaster.getEmployee()));
                jsonObject.addProperty("startTime", String.valueOf(taskMaster.getStartTime()));
                jsonObject.addProperty("endTime", taskMaster.getEndTime() != null ? String.valueOf(taskMaster.getEndTime()) : "");
                jsonObject.addProperty("workDone", taskMaster.getWorkDone());
                jsonObject.addProperty("remark", taskMaster.getRemark());
                jsonObject.addProperty("adminRemark", taskMaster.getAdminRemark());
                jsonObject.addProperty("correctiveAction", taskMaster.getCorrectiveAction());
                jsonObject.addProperty("preventiveAction", taskMaster.getPreventiveAction());

                jsonObject.addProperty("workBreakId", 0);
                if (taskMaster.getWorkBreak() != null) {
                    jsonObject.addProperty("workBreakId", taskMaster.getWorkBreak() != null ? taskMaster.getWorkBreak().getId() : 0);
                    jsonObject.addProperty("breakName", taskMaster.getWorkBreak() != null ? taskMaster.getWorkBreak().getBreakName() : "");
                }
                jsonObject.addProperty("machineId", 0);
                jsonObject.addProperty("jobId", 0);
                jsonObject.addProperty("jobOperationId", 0);
                jsonObject.addProperty("machineStartCount", 0);
                jsonObject.addProperty("machineEndCount", 0);
                jsonObject.addProperty("totalCount", 0);
                if (taskMaster.getTaskType() == 1) {
                    jsonObject.addProperty("machineId", taskMaster.getMachine().getId());
                    jsonObject.addProperty("isMachineCount", taskMaster.getMachine().getIsMachineCount());
                    jsonObject.addProperty("jobId", taskMaster.getJob().getId());
                    jsonObject.addProperty("jobOperationId", taskMaster.getJobOperation().getId());
                    jsonObject.addProperty("cycleTime", taskMaster.getCycleTime() != null ? taskMaster.getCycleTime() : 0);
                    jsonObject.addProperty("pcsRate", taskMaster.getPcsRate() != null ? taskMaster.getPcsRate() : 0);
                    jsonObject.addProperty("averagePerShift", taskMaster.getAveragePerShift() != null ? taskMaster.getAveragePerShift() : 0);
                    jsonObject.addProperty("pointPerJob", taskMaster.getPerJobPoint() != null ? taskMaster.getPerJobPoint() : 0);


                    jsonObject.addProperty("machineStartCount", taskMaster.getMachineStartCount());
                    jsonObject.addProperty("machineEndCount", taskMaster.getMachineEndCount() != null ? taskMaster.getMachineEndCount() : 0);
                    jsonObject.addProperty("totalCount", taskMaster.getTotalCount() != null ? taskMaster.getTotalCount() : 0);
                } else if (taskMaster.getTaskType() == 3) {
                    jsonObject.addProperty("machineId", taskMaster.getMachine().getId());
                }

                jsonObject.addProperty("totalQty", taskMaster.getTotalCount());
                jsonObject.addProperty("reworkQty", taskMaster.getReworkQty());
                jsonObject.addProperty("machineRejectQty", taskMaster.getMachineRejectQty());
                jsonObject.addProperty("doubtfulQty", taskMaster.getDoubtfulQty());
                jsonObject.addProperty("unMachinedQty", taskMaster.getUnMachinedQty());
                jsonObject.addProperty("okQty", taskMaster.getOkQty());
                jsonObject.addProperty("settingTimeInMin", taskMaster.getSettingTimeInMin());

                responseMessage.add("response", jsonObject);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                responseMessage.addProperty("message", "No tasks available");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            taskLogger.error("Failed to load data " + e);
            e.printStackTrace();
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public Object updateTaskDetails(Map<String, String> jsonRequest, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Employee employee = null;
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            Long taskId = Long.valueOf(jsonRequest.get("taskId"));

            TaskMaster taskMaster = taskRepository.findByIdAndStatus(taskId, true);
            if (taskMaster != null) {

                if (taskMaster.getTaskType() == 2 && taskMaster.getTaskMaster() != null) {
                    LocalDateTime startTime = LocalDateTime.parse(jsonRequest.get("startTime"), formatter);
                    LocalDateTime endTime = LocalDateTime.parse(jsonRequest.get("endTime"), formatter);

                    if (startTime.isAfter(taskMaster.getTaskMaster().getStartTime()) && endTime.isBefore(taskMaster.getTaskMaster().getEndTime())) {
                        System.out.println("current task startTime >>>> " + startTime + " endTime >>>>>> " + endTime);
                        System.out.println("taskMaster.getTaskMaster() startTime >>>> "
                                + taskMaster.getTaskMaster().getStartTime() + " endTime >>>>>> " + taskMaster.getTaskMaster().getStartTime());
                    } else {
                        responseMessage.setMessage("Task times are invalid, Please check properly!");
                        responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                        return responseMessage;
                    }
                }
                TaskMasterHistory taskMasterHistory = convertToHistory(taskMaster, users);
                if (taskMasterHistory != null) {
                    taskMasterHistoryRepository.save(taskMasterHistory);

                    try {
                        LocalDateTime startTime = LocalDateTime.parse(jsonRequest.get("startTime"), formatter);
//                        LocalDateTime startTime = LocalDateTime.parse(jsonRequest.get("startTime"));
                        Integer taskType = Integer.valueOf(jsonRequest.get("taskType"));
                        taskMaster.setTaskType(taskType);
                        taskMaster.setWorkDone(true);
                        taskMaster.setTaskStatus("in-progress");

                        employee = taskMaster.getEmployee();
                        if (taskType == 3) { // Setting time task
                            Long machineId = Long.parseLong(jsonRequest.get("machineId"));
                            Machine machine = machineRepository.findByIdAndStatus(machineId, true);
                            taskMaster.setMachine(machine);
                            taskMaster.setStartTime(startTime);
                            taskMaster.setWorkDone(Boolean.parseBoolean(jsonRequest.get("workDone")));
                        } else {
                            taskMaster.setStartTime(startTime);
                            if (taskType == 1) { // save Task
                                Long machineId = Long.parseLong(jsonRequest.get("machineId"));
                                Long jobId = Long.parseLong(jsonRequest.get("jobId"));
                                Long jobOperationId = Long.parseLong(jsonRequest.get("jobOperationId"));

                                Machine machine = machineRepository.findByIdAndStatus(machineId, true);
                                Job job = jobRepository.findByIdAndStatus(jobId, true);
                                JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(jobOperationId, true);

                                taskMaster.setMachine(machine);
                                taskMaster.setJob(job);
                                taskMaster.setJobOperation(jobOperation);

                                String operationData = operationDetailsRepository.getOperationDetailsByOperationId(jobOperation.getId(), LocalDate.now());
                                if (operationData != null) {
                                    String[] opData = operationData.split(",");
                                    taskMaster.setCycleTime(Double.valueOf(opData[1]));
                                    taskMaster.setPcsRate(Double.valueOf(opData[4]));

                                    double avgPerShift = Double.valueOf(opData[2]);
                                    double perJobPOint = (100.0 / avgPerShift);
                                    taskMaster.setPerJobPoint(perJobPOint);
                                }
                                if (machine.getIsMachineCount()) {
                                    taskMaster.setMachineStartCount(Long.valueOf(jsonRequest.get("machineStartCount")));
                                }
                            } else if (taskType == 2) {
                                Long breakId = Long.valueOf(jsonRequest.get("breakId"));
                                WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);
                                taskMaster.setWorkBreak(workBreak);
                                taskMaster.setWorkDone(Boolean.parseBoolean(jsonRequest.get("workDone")));
                            }
                        }

                        taskMaster.setPreventiveAction(null);
                        taskMaster.setCorrectiveAction(null);
                        taskMaster.setRemark(null);
                        taskMaster.setAdminRemark(null);
                        if (jsonRequest.containsKey("preventiveAction")) {
                            String preventiveAction = jsonRequest.get("preventiveAction");
                            if (!preventiveAction.equalsIgnoreCase("")) {
                                taskMaster.setPreventiveAction(preventiveAction);
                            }
                        }
                        if (jsonRequest.containsKey("correctiveAction")) {
                            String correctiveAction = jsonRequest.get("correctiveAction");
                            if (!correctiveAction.equalsIgnoreCase("")) {
                                taskMaster.setCorrectiveAction(correctiveAction);
                            }
                        }
                        if (jsonRequest.containsKey("remark")) {
                            String remark = jsonRequest.get("remark");
                            if (!remark.equalsIgnoreCase("")) {
                                taskMaster.setRemark(remark);
                            }
                        }
                        if (jsonRequest.containsKey("adminRemark")) {
                            String adminRemark = jsonRequest.get("adminRemark");
                            if (!adminRemark.equalsIgnoreCase("")) {
                                taskMaster.setAdminRemark(adminRemark);
                            }
                        }

                        taskMaster.setUpdatedAt(LocalDateTime.now());
                        taskMaster.setUpdatedBy(employee.getId());
                        taskMaster.setInstitute(employee.getInstitute());
                        taskMaster.setStatus(true);
                        try {
                            taskRepository.save(taskMaster);
                            System.out.println("Successfully task saved");
                        } catch (Exception e) {
                            taskLogger.error("Failed to start task " + e);
                            e.printStackTrace();
                            System.out.println("Exception " + e.getMessage());
                        }
                    } catch (Exception e) {
                        taskLogger.error("Failed to update task " + e);
                        e.printStackTrace();
                        System.out.println("Exception " + e.getMessage());

                        responseMessage.setMessage("Failed to updated task");
                        responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                        return responseMessage;
                    }

                    taskMaster.setEndTime(null);

                    try {
                        if (!jsonRequest.get("endTime").equalsIgnoreCase("")) {
                            updateTaskDataForEnd(taskMaster, jsonRequest, formatter, employee);
                        } else {
                            System.out.println("end time is empty");
                            if (jsonRequest.get("totalQty") != null)
                                taskMaster.setTotalCount(Long.valueOf(jsonRequest.get("totalQty")));
                            if (jsonRequest.get("machineStartCount") != null)
                                taskMaster.setMachineStartCount(Long.valueOf(jsonRequest.get("machineStartCount")));
                            if (jsonRequest.get("machineEndCount") != null)
                                taskMaster.setMachineEndCount(Long.valueOf(jsonRequest.get("machineEndCount")));
                            if (jsonRequest.get("totalCount") != null)
                                taskMaster.setTotalCount(Long.valueOf(jsonRequest.get("totalCount")));
                            if (jsonRequest.get("machineRejectQty") != null)
                                taskMaster.setMachineRejectQty(Double.valueOf(jsonRequest.get("machineRejectQty")));
                            if (jsonRequest.get("reworkQty") != null)
                                taskMaster.setReworkQty(Double.valueOf(jsonRequest.get("reworkQty")));
                            if (jsonRequest.get("unMachinedQty") != null)
                                taskMaster.setUnMachinedQty(Double.valueOf(jsonRequest.get("unMachinedQty")));
                            if (jsonRequest.get("doubtfulQty") != null)
                                taskMaster.setDoubtfulQty(Double.valueOf(jsonRequest.get("doubtfulQty")));
                            if (jsonRequest.get("okQty") != null)
                                taskMaster.setOkQty(Double.valueOf(jsonRequest.get("okQty")));
                        }

                        TaskMaster savedTaskMaster = null;
                        try {
                            savedTaskMaster = taskRepository.save(taskMaster);
                        } catch (Exception e) {
                            System.out.println("Exception " + e.getMessage());
                            e.printStackTrace();
                        }
                        if (savedTaskMaster.getTaskMaster() != null) {
                            TaskMaster task = savedTaskMaster.getTaskMaster();
                            double totalBreakMinutes = taskRepository.getSumOfBreakTime(savedTaskMaster.getTaskMaster().getId());
                            double actualTaskTime = task.getTotalTime() - totalBreakMinutes;

                            task.setActualWorkTime(actualTaskTime);
                            System.out.println("user time in minutes " + task.getActualWorkTime());
                            System.out.println("user totalActualTime in minutes " + actualTaskTime);
                            double workHours = (task.getActualWorkTime() / 60.0);
                            double requiredProduction = 0, actualProduction = 0, shortProduction = 0, percentageOfTask = 0;
                            double productionPoint = 0, productionWorkingHour = 0, wagesPcsBasis = 0, wagesPerPcs = 0;
                            if (savedTaskMaster.getTaskType() == 2 && task.getTaskType() == 1) {
                                requiredProduction = (actualTaskTime / task.getCycleTime());
                                System.out.println("requiredProduction " + requiredProduction);
                                actualProduction = task.getOkQty() != null ? task.getOkQty() : 0;
                                shortProduction = (actualProduction - requiredProduction);
                                percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
                            }
                            System.out.println("percentageOfTask " + percentageOfTask);

                            task.setWorkingHour(workHours);
                            task.setRequiredProduction(requiredProduction);
                            task.setShortProduction(shortProduction);
                            task.setPercentageOfTask(percentageOfTask);

                            if (savedTaskMaster.getTaskType() == 2 && task.getTaskType() == 1) {
                                productionPoint = (double) (task.getOkQty() != null ? task.getOkQty() : 0) * task.getPerJobPoint();
                                productionWorkingHour = productionPoint / 12.5;
                            }
                            double settingTimeInMinutes = totalBreakMinutes;
                            double perMinutePoint = 100.0 / 60;
                            double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
                            double workingHourWithSetting = productionWorkingHour + settingTimeInHour;
                            double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
                            double totalPoint = productionPoint + settingTimePoint;

                            double wagesPerPoint = task.getWagesPerDay() / 100.0;
                            double wagesPointBasis = wagesPerPoint * totalPoint;

                            if (savedTaskMaster.getTaskType() == 2 && task.getTaskType() == 1) {
                                wagesPerPcs = task.getPcsRate();
                                wagesPcsBasis = (task.getOkQty() != null ? task.getOkQty() : 0) * wagesPerPcs;
                            }

                            task.setProdPoint(productionPoint);
                            task.setProdWorkingHour(productionWorkingHour);
                            task.setSettingTimeInMin(settingTimeInMinutes);
                            task.setSettingTimeInHour(settingTimeInHour);
                            task.setWorkingHourWithSetting(workingHourWithSetting);
                            task.setSettingTimePoint(settingTimePoint);
                            task.setTotalPoint(totalPoint);

                            task.setWagesPerPoint(wagesPerPoint);
                            task.setWagesPointBasis(wagesPointBasis);
                            task.setWagesPcsBasis(wagesPcsBasis);
                            taskRepository.save(task);
                        }

                        updateEmployeeTaskSummary(savedTaskMaster.getAttendance());
                        responseMessage.setMessage("Task updated successfully");
                        responseMessage.setResponseStatus(HttpStatus.OK.value());
                    } catch (Exception e) {
                        System.out.println("Exception " + e.getMessage());
                        e.printStackTrace();

                        responseMessage.setMessage("Failed to updated task");
                        responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                } else {
                    responseMessage.setMessage("Failed to save task history");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            } else {
                responseMessage.setMessage("Failed to load task");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            taskLogger.error("Failed to load task " + e);
            System.out.println("Exception " + e.getMessage());
            e.printStackTrace();
            responseMessage.setMessage("Failed to load task");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
        }
        return responseMessage;
    }

    private void updateTaskDataForEnd(TaskMaster taskMaster, Map<String, String> jsonRequest, DateTimeFormatter formatter, Employee employee) {
        try {
            Integer taskType = taskMaster.getTaskType();
            taskMaster.setTaskStatus("complete");

            LocalDateTime l1 = taskMaster.getStartTime();
            LocalDateTime l2 = LocalDateTime.parse(jsonRequest.get("endTime"), formatter);
            taskMaster.setEndTime(l2);
            System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
            double totalTime = SECONDS.between(l1, l2) / 60.0;
            double time = totalTime;
            System.out.println("total time in min " + time);
            taskMaster.setTotalTime(time);
            taskMaster.setActualWorkTime(time);

            LocalTime shiftTime = employee.getShift().getWorkingHours();
            Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
            System.out.println("shiftMinutes in min " + shiftMinutes);
            Double shiftHours = Double.valueOf((shiftMinutes / 60));
            System.out.println("shiftHours" + shiftHours);

            LocalTime workTime = utility.getDateTimeDiffInTime(l1, l2);
            taskMaster.setWorkingTime(workTime);
            if (taskType == 2) { // 2=>Downtime
                taskMaster.setTotalTime(time);
                if (taskMaster.getWorkDone()) {
                    taskMaster.setActualWorkTime(time);
                }
                /*calculate break hour wages for PCS basis employee which breaks are working only*/
                taskMaster.setBreakWages(calculateBreakData(time, taskMaster, shiftHours));
            }

//            if (taskType == 3) { // 3=>Setting time
////                calculateBreakData(time, taskMaster);
//                taskMaster.setActualWorkTime(time);
//                /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                taskMaster.setBreakWages(calculateBreakData(time, taskMaster, shiftHours));
//            } else if (taskType == 1) { // 1=>Task
//                System.out.println("user time in minutes 2=>Task" + time);
//
//                /*double totalBreakMinutes = taskRepository.getSumOfBreakTime(taskMaster.getId());*/
//                double totalBreakMinutes = Double.parseDouble(jsonRequest.get("settingTimeInMin"));
//                double actualTaskTime = time - totalBreakMinutes;
//                taskMaster.setActualWorkTime(actualTaskTime);
//
//                taskMaster.setTotalCount(Long.valueOf(jsonRequest.get("totalQty")));
//                taskMaster.setReworkQty(Double.valueOf(jsonRequest.get("reworkQty")));
//                taskMaster.setMachineRejectQty(Double.valueOf(jsonRequest.get("machineRejectQty")));
//                taskMaster.setDoubtfulQty(Double.valueOf(jsonRequest.get("doubtfulQty")));
//                taskMaster.setUnMachinedQty(Double.valueOf(jsonRequest.get("unMachinedQty")));
//                taskMaster.setOkQty(Double.valueOf(jsonRequest.get("okQty")));
//
//                Long okQty = Long.valueOf(jsonRequest.get("okQty"));
//
//                System.out.println("user time in minutes " + time);
//                System.out.println("user totalActualTime in minutes " + actualTaskTime);
//                double jobsPerHour = (60.0 / taskMaster.getCycleTime());
//                double workHours = (time / 60.0);
//                double requiredProduction = (actualTaskTime / taskMaster.getCycleTime());
//                System.out.println("requiredProduction " + requiredProduction);
//                double actualProduction = okQty;
//                double shortProduction = (actualProduction - requiredProduction);
//                double percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
//                System.out.println("percentageOfTask " + percentageOfTask);
//
//                taskMaster.setJobsPerHour(jobsPerHour);
//                taskMaster.setWorkingHour(workHours);
//                taskMaster.setRequiredProduction(requiredProduction);
//                taskMaster.setActualProduction(actualProduction);
//                taskMaster.setShortProduction(shortProduction);
//                taskMaster.setPercentageOfTask(percentageOfTask);
//
//                double productionPoint = (double) okQty * taskMaster.getPerJobPoint();
//                double productionWorkingHour = productionPoint / 12.5;
//                double settingTimeInMinutes = totalBreakMinutes;
//                double perMinutePoint = 100.0 / 60;
//                double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
//                double workingHourWithSetting = productionWorkingHour + settingTimeInHour;
//                double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
//                double totalPoint = productionPoint + settingTimePoint;
//
//                double wagesPerPoint = taskMaster.getWagesPerDay() / 100.0;
//                double wagesPointBasis = wagesPerPoint * totalPoint;
//                double wagesPerPcs = taskMaster.getPcsRate();
//                double wagesPcsBasis = okQty * wagesPerPcs;
//
//                taskMaster.setProdPoint(productionPoint);
//                taskMaster.setProdWorkingHour(productionWorkingHour);
//                taskMaster.setSettingTimeInMin(settingTimeInMinutes);
//                taskMaster.setSettingTimeInHour(settingTimeInHour);
//                taskMaster.setWorkingHourWithSetting(workingHourWithSetting);
//                taskMaster.setSettingTimePoint(settingTimePoint);
//                taskMaster.setTotalPoint(totalPoint);
//
//                taskMaster.setWagesPerPoint(wagesPerPoint);
//                taskMaster.setWagesPointBasis(wagesPointBasis);
//                taskMaster.setWagesPcsBasis(wagesPcsBasis);
//
//                Machine machine = taskMaster.getMachine();
//                if (machine.getIsMachineCount()) {
//                    taskMaster.setMachineStartCount(Long.valueOf(jsonRequest.get("machineStartCount")));
//                    taskMaster.setMachineEndCount(Long.valueOf(jsonRequest.get("machineEndCount")));
//
////                    machine.setCurrentMachineCount(Long.valueOf(jsonRequest.get("machineEndCount")));
////                    machineRepository.save(machine);
//                }
//            } else if (taskType == 2) { // 2=>Downtime
//                taskMaster.setTotalTime(time);
//                if (taskMaster.getWorkDone()) {
//                    taskMaster.setActualWorkTime(time);
//                }
//                /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                taskMaster.setBreakWages(calculateBreakData(time, taskMaster));
//            } else if (taskType == 4) { // 4=> task without machine
//                System.out.println("user time in minutes " + time);
////                calculateBreakData(time, taskMaster);
//
////                double totalBreakMinutes = taskRepository.getSumOfBreakTime(taskMaster.getId());
//                double totalBreakMinutes = Double.parseDouble(jsonRequest.get("settingTimeInMin"));
//                totalBreakMinutes = totalBreakMinutes > 0 ? totalBreakMinutes :
//                        taskRepository.getSumOfBreakTime(taskMaster.getId());
//                System.out.println("totalBreakMinutes " + totalBreakMinutes);
//                double actualTaskTime = time - totalBreakMinutes;
//                taskMaster.setActualWorkTime(actualTaskTime);
//                double workHours = (time / 60.0);
//                double prodWorkHours = (actualTaskTime / 60.0);
//                taskMaster.setWorkingHour(workHours);
//                taskMaster.setProdWorkingHour(prodWorkHours);
//                double settingTimeInMinutes = totalBreakMinutes;
//                double perMinutePoint = 100.0 / 60;
//                double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
//
//                taskMaster.setSettingTimeInMin(settingTimeInMinutes);
//                taskMaster.setSettingTimeInHour(settingTimeInHour);
//
//            }
            taskMaster.setUpdatedAt(LocalDateTime.now());
            taskMaster.setUpdatedBy(taskMaster.getEmployee().getId());
            taskMaster.setInstitute(taskMaster.getInstitute());
        } catch (Exception e) {
            taskLogger.error("Update task details exception " + e);
            System.out.println("Update task details exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    private TaskMasterHistory convertToHistory(TaskMaster taskMaster, Users users) {
        TaskMasterHistory taskMasterHistory = new TaskMasterHistory();

        taskMasterHistory.setTaskId(taskMaster.getId());
        taskMasterHistory.setEmployeeId(taskMaster.getEmployee().getId());
        taskMasterHistory.setAttendanceId(taskMaster.getAttendance().getId());
        taskMasterHistory.setUpdatingUserId(users.getId());
        taskMasterHistory.setInstitute(users.getInstitute());

        if (taskMaster.getTaskMaster() != null) {
            taskMasterHistory.setTaskMasterId(taskMaster.getTaskMaster().getId());
        }
        taskMasterHistory.setTaskDate(taskMaster.getTaskDate());
        taskMasterHistory.setStartTime(taskMaster.getStartTime());

        if (taskMaster.getEndTime() != null) {
            taskMasterHistory.setEndTime(taskMaster.getEndTime());
        }
        taskMasterHistory.setTotalTime(taskMaster.getTotalTime());
        taskMasterHistory.setActualWorkTime(taskMaster.getActualWorkTime());
        taskMasterHistory.setTaskType(taskMaster.getTaskType());


        if (taskMaster.getMachine() != null) {
            taskMasterHistory.setMachineId(taskMaster.getMachine().getId());
        }
        if (taskMaster.getJob() != null) {
            taskMasterHistory.setJobId(taskMaster.getJob().getId());
        }
        if (taskMaster.getJobOperation() != null) {
            taskMasterHistory.setJobOperationId(taskMaster.getJobOperation().getId());
        }
        if (taskMaster.getWorkBreak() != null) {
            taskMasterHistory.setWorkBreakId(taskMaster.getWorkBreak().getId());
        }

        taskMasterHistory.setRemark(taskMaster.getRemark());
        taskMasterHistory.setAdminRemark(taskMaster.getAdminRemark());
        taskMasterHistory.setCreatedAt(taskMaster.getCreatedAt());
        taskMasterHistory.setCreatedBy(taskMaster.getCreatedBy());
        taskMasterHistory.setUpdatedAt(taskMaster.getUpdatedAt());
        taskMasterHistory.setUpdatedBy(taskMaster.getUpdatedBy());

        return taskMasterHistory;
    }

    public void getLunchTime(HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        try {

//        LocalTime l1 = LocalTime.parse("07:04:51");
            LocalTime l1 = LocalTime.parse("20:35:00");
            System.out.println("L1 " + l1);
//        LocalTime l2 = LocalTime.parse("12:22:59");
            LocalTime l2 = LocalTime.parse("08:45:32");
            System.out.println("l2 " + l2);

            System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
            double totalTime = SECONDS.between(l1, l2) / 60.0;
            System.out.println("total time in min " + totalTime);

            double conversion = Precision.round(100.0 / 60.0, 2);
            double convertHours = totalTime * conversion / 100;

            System.out.println("" + conversion + " - " + convertHours);


            int s = (int) SECONDS.between(l1, l2);

            int sec = Math.abs(s % 60);
            int min = Math.abs((s / 60) % 60);
            int hours = Math.abs((s / 60) / 60);

            String strSec = (sec < 10) ? "0" + sec : Integer.toString(sec);
            String strmin = (min < 10) ? "0" + min : Integer.toString(min);
            String strHours = (hours < 10) ? "0" + hours : Integer.toString(hours);

            System.out.println("------------------->----------------------------------- ");
            System.out.println("-------------------> " + strHours + ":" + strmin + ":" + strSec);

//            LocalTime et = LocalTime.parse(strHours + ":" + strmin + ":" + strSec);
            LocalTime et = utility.getTimeDiffFromTimes(l1, l2);

            System.out.println("-------------------> " + et);
            System.out.println("------------------->----------------------------------- ");
        } catch (Exception e) {
            taskLogger.error("Update task details exception " + e);
            System.out.println("Update task details exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Object updateTaskData(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            Long taskId = Long.valueOf(requestParam.get("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);

            double totalActualTime = 0;
            if (task != null) {
                Integer taskType = task.getTaskType();
                task.setTaskStatus("complete");

                LocalDateTime l1 = LocalDateTime.parse(requestParam.get("startTime"));
                LocalDateTime l2 = LocalDateTime.parse(requestParam.get("endTime"));
                task.setStartTime(l1);
                task.setEndTime(l2);
                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60));
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                double time = totalTime;
                System.out.println("total time in min " + time);
                task.setTotalTime(time);
                task.setActualWorkTime(time);

                LocalTime workTime = utility.getTimeDiffFromTimes(l1.toLocalTime(), l2.toLocalTime());
                task.setWorkingTime(workTime);

                Employee employee = employeeRepository.findByIdAndStatus(task.getEmployee().getId(), true);
                LocalTime shiftTime = employee.getShift().getWorkingHours();
                Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
                System.out.println("shiftMinutes in min " + shiftMinutes);
                Double shiftHours = Double.valueOf((shiftMinutes / 60));
                System.out.println("shiftHours" + shiftHours);


                if (taskType == 2) { // 2=>Downtime
                    System.out.println("user time in minutes 2=>Downtime" + time);
                    if (task.getWorkDone()) {
                        task.setActualWorkTime(time);
                    }
                    /*calculate break hour wages for PCS basis employee which breaks are working only*/
                    task.setBreakWages(calculateBreakData(time, task, shiftHours));
                }

//                if (taskType == 3) { // 3=>Setting time
//                    System.out.println("user time in minutes 2=>Setting time" + time);
//                    task.setActualWorkTime(time);
//                } else if (taskType == 1) { // 1=>Task
//                    System.out.println("user time in minutes 2=>Task" + time);
//
//                    /*OLD CODE below*/
//                    /*double totalBreakMinutes = taskRepository.getSumOfBreakTime(task.getId());*/
//                    double totalBreakMinutes = Double.parseDouble(requestParam.get("settingTimeInMin"));
//                    double actualTaskTime = time - totalBreakMinutes;
//
//                    task.setActualWorkTime(actualTaskTime);
//                    task.setTotalCount(Long.valueOf(requestParam.get("totalCount")));
//                    task.setReworkQty(Double.valueOf(requestParam.get("reworkQty")));
//                    task.setMachineRejectQty(Double.valueOf(requestParam.get("machineRejectQty")));
//                    task.setDoubtfulQty(Double.valueOf(requestParam.get("doubtfulQty")));
//                    task.setUnMachinedQty(Double.valueOf(requestParam.get("unMachinedQty")));
//                    task.setOkQty(Double.valueOf(requestParam.get("okQty")));
//                    task.setSettingTimeInMin(Double.valueOf(requestParam.get("settingTimeInMin")));
//
//                    Long okQty = Long.valueOf(requestParam.get("okQty"));
//
//                    System.out.println("user time in minutes " + time);
//                    System.out.println("user totalActualTime in minutes " + actualTaskTime);
//                    double jobsPerHour = (60.0 / task.getCycleTime());
//                    double workHours = (time / 60.0);
//                    double requiredProduction = (time / task.getCycleTime());
//                    System.out.println("requiredProduction " + requiredProduction);
//                    double actualProduction = okQty;
//                    double shortProduction = (actualProduction - requiredProduction);
//                    double percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
//                    System.out.println("percentageOfTask " + percentageOfTask);
//
//                    task.setJobsPerHour(jobsPerHour);
//                    task.setWorkingHour(workHours);
//                    task.setRequiredProduction(requiredProduction);
//                    task.setActualProduction(actualProduction);
//                    task.setShortProduction(shortProduction);
//                    task.setPercentageOfTask(percentageOfTask);
//
//                    double productionPoint = (double) okQty * task.getPerJobPoint();
//                    double productionWorkingHour = productionPoint / 12.5;
//                    double settingTimeInMinutes = totalBreakMinutes;
//                    double perMinutePoint = 100.0 / 60;
//                    double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
//                    double workingHourWithSetting = productionWorkingHour + settingTimeInHour;
//                    double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
//                    double totalPoint = productionPoint + settingTimePoint;
//
////                    double wagesPerDay = task.getEmployee().getWagesPerDay();
//                    double wagesPerPoint = task.getWagesPerDay() / 100.0;
//                    double wagesPointBasis = wagesPerPoint * totalPoint;
//                    double wagesPerPcs = Precision.round(task.getPcsRate(), 2);
//                    double wagesPcsBasis = Precision.round(okQty * wagesPerPcs, 2);
//
//                    task.setProdPoint(productionPoint);
//                    task.setProdWorkingHour(productionWorkingHour);
//                    task.setSettingTimeInMin(settingTimeInMinutes);
//                    task.setSettingTimeInHour(settingTimeInHour);
//                    task.setWorkingHourWithSetting(workingHourWithSetting);
//                    task.setSettingTimePoint(settingTimePoint);
//                    task.setTotalPoint(totalPoint);
//
//                    task.setWagesPerPoint(wagesPerPoint);
//                    task.setWagesPointBasis(wagesPointBasis);
//                    task.setWagesPcsBasis(wagesPcsBasis);
//
//                    Machine machine = task.getMachine();
//                    if (machine.getIsMachineCount()) {
//                        task.setMachineEndCount(Long.valueOf(requestParam.get("machineEndCount")));
//
//                        machine.setCurrentMachineCount(Long.valueOf(requestParam.get("machineEndCount")));
//                        machineRepository.save(machine);
//                    }
//                } else if (taskType == 2) { // 2=>Downtime
//                    System.out.println("user time in minutes 2=>Downtime" + time);
//                    if (task.getWorkDone()) {
//                        task.setActualWorkTime(time);
//                    }
//                    /*calculate break hour wages for PCS basis employee which breaks are working only*/
//                    task.setBreakWages(calculateBreakData(time, task));
//                } else if (taskType == 4) { // 4=> task without machine
//                    System.out.println("user time in minutes 4=> task without machine " + time);
////                    calculateBreakData(time, task); // no need to do calculate for breaks & setting times
//                    task.setActualWorkTime(time);
//                }

                if (requestParam.containsKey("remark")) {
                    String remark = requestParam.get("remark");
                    if (!remark.equalsIgnoreCase("")) {
                        task.setRemark(remark);
                    }
                }
                if (requestParam.containsKey("preventiveAction")) {
                    String preventiveAction = requestParam.get("preventiveAction");
                    if (!preventiveAction.equalsIgnoreCase("")) {
                        task.setPreventiveAction(preventiveAction);
                    }
                }
                if (requestParam.containsKey("correctiveAction")) {
                    String correctiveAction = requestParam.get("correctiveAction");
                    if (!correctiveAction.equalsIgnoreCase("")) {
                        task.setCorrectiveAction(correctiveAction);
                    }
                }

                task.setUpdatedAt(LocalDateTime.now());
                task.setUpdatedBy(task.getEmployee().getId());
                task.setInstitute(task.getInstitute());
                try {
                    TaskMaster savedTaskMaster = taskRepository.save(task);
                    updateEmployeeTaskSummary(task.getAttendance());

                    responseMessage.setMessage("Successfully task finished");
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } catch (Exception e) {
                    taskLogger.error("Failed to finish task " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.setMessage("Failed to finish task");
                    responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            taskLogger.error("failed to finish task" + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to finish task");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }

        return responseMessage;
    }

    public Object boStartTask(Map<String, String> requestParam, HttpServletRequest request) {
        ResponseMessage responseMessage = new ResponseMessage();
        TaskMaster task = new TaskMaster();
        try {
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Long employeeId = Long.valueOf(requestParam.get("employeeId"));
            Employee employee = employeeRepository.findByIdAndStatus(employeeId, true);

            Integer taskType = Integer.valueOf(requestParam.get("taskType"));
            if (employee.getDesignation().getCode().equalsIgnoreCase("l3")) {
                if (taskType == 4) {
                    responseMessage.setMessage("Employee not allowed to start this task");
                    responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());

                    return responseMessage;
                }
            } else if (employee.getDesignation().getCode().equalsIgnoreCase("l2")) {
                if (taskType == 1) {
                    responseMessage.setMessage("Employee not allowed to start this task");
                    responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());

                    return responseMessage;
                }
            } else if (employee.getDesignation().getCode().equalsIgnoreCase("l1")) {
                if (taskType == 1) {
                    responseMessage.setMessage("Employee not allowed to start this task");
                    responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());

                    return responseMessage;
                }
            }

            LocalDate taskDate = LocalDate.parse(requestParam.get("taskDate"));
            Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employeeId, taskDate, true);
            if (attendance != null) {
                task.setAttendance(attendance);
                String taskStartTime = requestParam.get("startTime");
                System.out.println("taskStartTime " + taskStartTime);
                LocalDateTime startTime = LocalDateTime.parse(taskStartTime, myFormatObj);
                System.out.println("startTime " + startTime);
                task.setTaskDate(taskDate);
//                task.setTaskDate(LocalDate.now());
                task.setEmployee(employee);
                task.setTaskType(taskType);
                task.setWorkDone(true);
                task.setTaskStatus("in-progress");

                Integer shiftMinutes = 480;
                System.out.println("shiftMinutes in min " + shiftMinutes);

                Double wagesPerDay = utility.getEmployeeWages(employee.getId());
                if (wagesPerDay == null) {
                    System.out.println("employee wagesPerDay =" + wagesPerDay);
                    responseMessage.setMessage("Your salary not updated! Please contact to Admin");
                    responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
                } else {
                    double wagesPerHour = (wagesPerDay / 8.0);
                    double wagesPerPoint = (wagesPerDay / 100.0);

                    task.setWagesPerDay(wagesPerDay);
                    task.setWagesPerPoint(wagesPerPoint);
                    task.setEmployeeWagesType(employee.getEmployeeWagesType());

                    task.setStartTime(startTime);
                    if (taskType == 3) { // Setting time task
                        Long machineId = Long.parseLong(requestParam.get("machineId"));
                        Machine machine = machineRepository.findByIdAndStatus(machineId, true);
                        task.setMachine(machine);
                        task.setWorkDone(true);
                    } else {
                        if (taskType == 1) { // save Task
                            requestParam.putIfAbsent("attendanceId", attendance.getId().toString());
                            Boolean result = checkSameTaskStarting(employee, requestParam);
                            if (!result) {
                                Long machineId = Long.parseLong(requestParam.get("machineId"));
                                Long jobId = Long.parseLong(requestParam.get("jobId"));
                                Long jobOperationId = Long.parseLong(requestParam.get("jobOperationId"));

                                Machine machine = machineRepository.findByIdAndStatus(machineId, true);
                                Job job = jobRepository.findByIdAndStatus(jobId, true);
                                JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(jobOperationId, true);

                                task.setMachine(machine);
                                task.setJob(job);
                                task.setJobOperation(jobOperation);

                                String operationData = operationDetailsRepository.getOperationDetailsByOperationId(jobOperation.getId(), LocalDate.now());
                                if (operationData != null) {
                                    String[] opData = operationData.split(",");

                                    task.setCycleTime(Double.valueOf(opData[1]));
                                    task.setPcsRate(Double.valueOf(opData[4]));
                                    double avgPerShift = Double.parseDouble(opData[2]);
                                    System.out.println("avgPerShift " + avgPerShift);
                                    task.setAveragePerShift(avgPerShift);
                                    double perJobPOint = (100.0 / avgPerShift);
                                    task.setPerJobPoint(perJobPOint);
                                    if (machine.getIsMachineCount()) {
                                        task.setMachineStartCount(Long.valueOf(requestParam.get("machineStartCount")));
                                    }
                                }
                                task.setWorkDone(true);
                            } else {
                                System.out.println("Same as previous task starting ");
                                responseMessage.setMessage("Not allowed to start task as previous task");
                                responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());

                                return responseMessage;
                            }
                        } else if (taskType == 2) {
                            if (requestParam.containsKey("taskId")) {
                                Long taskMasterId = Long.valueOf(requestParam.get("taskId"));
                                TaskMaster taskMaster = taskRepository.findByIdAndStatus(taskMasterId, true);
                                task.setTaskMaster(taskMaster);
                                task.setAttendance(taskMaster.getAttendance());
                            }

                            Long breakId = Long.valueOf(requestParam.get("breakId"));
                            WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);
                            task.setWorkBreak(workBreak);
                            task.setWorkDone(Boolean.parseBoolean(requestParam.get("workDone")));
                        }
                    }

                    if (requestParam.containsKey("remark")) {
                        String remark = requestParam.get("remark");
                        if (!remark.equalsIgnoreCase("")) {
                            task.setRemark(remark);
                        }
                    }

                    task.setCreatedAt(LocalDateTime.now());
                    task.setCreatedBy(employee.getId());
                    task.setInstitute(employee.getInstitute());
                    task.setStatus(true);
                    try {
                        TaskMaster savedTaskMaster = taskRepository.save(task);
                        responseMessage.setMessage("Successfully task started");
                        responseMessage.setResponseStatus(HttpStatus.OK.value());
                    } catch (Exception e) {
                        taskLogger.error("Failed to start task " + e);
                        e.printStackTrace();
                        System.out.println("Exception " + e.getMessage());
                        responseMessage.setMessage("Failed to start task");
                        responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                    }
                }
            } else {
                responseMessage.setMessage("Please checkin attendance");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            taskLogger.error("Failed to start task " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.setMessage("Failed to start task");
            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }

    public JsonObject deleteTask(HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            Long taskId = Long.valueOf(request.getParameter("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);
            if (task != null) {
                try {
                    if (task.getTaskType() == 1) {
                        List<TaskMaster> taskMasterList = taskRepository.findByTaskMasterIdAndStatus(taskId, true);
                        if (taskMasterList.size() > 0)
                            taskRepository.updateTaskBreaksStatus(taskId, false);
                    }

                    task.setStatus(false);
                    task.setUpdatedBy(users.getId());
                    task.setInstitute(users.getInstitute());
                    task.setUpdatedAt(LocalDateTime.now());
                    task.setAdminRemark("Task Deleted by " + users.getUsername() + " - " + users.getId());
                    taskRepository.save(task);


                    updateEmployeeTaskSummary(task.getAttendance());
                    response.addProperty("message", "Task deleted successfully");
                    response.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    taskLogger.error("Exception => deleteTask " + e);

                    response.addProperty("message", "Failed to delete task");
                    response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
                return response;
            } else {
                response.addProperty("message", "Task not exists");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            taskLogger.error("Exception => deleteTask " + e);

            response.addProperty("message", "Failed to find task");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getRejectionReports(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            String fromDate = jsonRequest.get("fromYearMonth");
            String toDate = jsonRequest.get("toYearMonth");
            String reportType = jsonRequest.get("reportType");

//            String fromDate = fromYearMonth.split("-")[0] + "-" + fromYearMonth.split("-")[1] + "-01"; // month/date/year
//            String toDate = toYearMonth.split("-")[0] + "-" + toYearMonth.split("-")[1] + "-12";

            JsonArray machineNamesArray = new JsonArray();
            JsonArray itemNamesArray = new JsonArray();
            JsonArray operatorNamesArray = new JsonArray();

            double rejectionSum = 0.0;
            double reworkSum = 0.0;
            double doubtfulSum = 0.0;
            double unMachinedSum = 0.0;
            JsonObject sumObj = new JsonObject();

            if (reportType.equals("operator-name")) {
                List<Object[]> operatorList = taskRepository.getRejectionOperatorListBetweenDates(fromDate, toDate);
                System.out.println("operatorList>>>"+operatorList.size());
                for (int i = 0; i < operatorList.size(); i++) {

                    Object[] obj = operatorList.get(i);

                    JsonObject operatorObj = new JsonObject();
                    Employee employee = employeeRepository.findById(Long.parseLong(obj[0].toString())).get();
                    System.out.println(utility.getEmployeeName(employee));
                    operatorObj.addProperty("operatorName", utility.getEmployeeName(employee));
                    operatorObj.addProperty("JobName", obj[1].toString());
                    operatorObj.addProperty("operationName", obj[2] != null ? obj[2].toString() : "");
                    operatorObj.addProperty("rejectQty", obj[3] != null ? obj[3].toString() : "");
                    operatorObj.addProperty("reworkQty", obj[4] != null ? obj[4].toString() : "");
                    operatorObj.addProperty("doubtQty", obj[5] != null ? obj[5].toString() : "");
                    operatorObj.addProperty("unMachinedQty", obj[6] != null ? obj[6].toString() : "");

                    operatorNamesArray.add(operatorObj);
                    rejectionSum += obj[3] != null ? Double.parseDouble(obj[3].toString()) : 0.0;
                    reworkSum = obj[4] != null ? Double.parseDouble(obj[4].toString()) : 0.0;
                    doubtfulSum = obj[5] != null ? Double.parseDouble(obj[5].toString()) : 0.0;
                    unMachinedSum = obj[6] != null ? Double.parseDouble(obj[6].toString()) : 0.0;
                }
                response.add("namesArray", operatorNamesArray);
                sumObj.addProperty("rejectionSum", rejectionSum);
                sumObj.addProperty("reworkSum", reworkSum);
                sumObj.addProperty("doubtfulSum", doubtfulSum);
                sumObj.addProperty("unMachinedSum", unMachinedSum);
                response.add("sumData", sumObj);
            } else if (reportType.equals("item-name")) {
                List<Object[]> itemList = taskRepository.getRejectionItemListBetweenDates(fromDate, toDate);
                for (int i = 0; i < itemList.size(); i++) {
                    Object[] obj = itemList.get(i);
                    System.out.println("ItemID -------------------------------------" + obj[0].toString());
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("itemName", obj[0].toString());
                    itemObj.addProperty("operationName", obj[1] != null ? obj[1].toString() : "");
                    itemObj.addProperty("machineName", obj[2] != null ? obj[2].toString() : "");
                    itemObj.addProperty("rejectQty", obj[3] != null ? obj[3].toString() : "");
                    itemObj.addProperty("reworkQty", obj[4] != null ? obj[4].toString() : "");
                    itemObj.addProperty("doubtQty", obj[5] != null ? obj[5].toString() : "");
                    itemObj.addProperty("unMachinedQty", obj[6] != null ? obj[6].toString() : "");

                    itemNamesArray.add(itemObj);
                    rejectionSum += obj[3] != null ? Double.parseDouble(obj[3].toString()) : 0.0;
                    reworkSum = obj[4] != null ? Double.parseDouble(obj[4].toString()) : 0.0;
                    doubtfulSum = obj[5] != null ? Double.parseDouble(obj[5].toString()) : 0.0;
                    unMachinedSum = obj[6] != null ? Double.parseDouble(obj[6].toString()) : 0.0;
                }
                response.add("namesArray", itemNamesArray);
                sumObj.addProperty("rejectionSum", rejectionSum);
                sumObj.addProperty("reworkSum", reworkSum);
                sumObj.addProperty("doubtfulSum", doubtfulSum);
                sumObj.addProperty("unMachinedSum", unMachinedSum);
                response.add("sumData", sumObj);
            } else if (reportType.equals("machine-number")) {
                List<Object[]> machineList = taskRepository.getRejectionMachineListBetweenDates(fromDate, toDate);
                for (int i = 0; i < machineList.size(); i++) {
                    Object[] obj = machineList.get(i);
                    System.out.println("MachineID -------------------------------------" + obj[0].toString());
                    JsonObject machineObj = new JsonObject();
                    machineObj.addProperty("machineName", obj[0].toString());
                    machineObj.addProperty("itemName", obj[1] != null ? obj[1].toString() : "");
                    machineObj.addProperty("operationName", obj[2] != null ? obj[2].toString() : "");
                    machineObj.addProperty("rejectQty", obj[3] != null ? obj[3].toString() : "");
                    machineObj.addProperty("reworkQty", obj[4] != null ? obj[4].toString() : "");
                    machineObj.addProperty("doubtQty", obj[5] != null ? obj[5].toString() : "");
                    machineObj.addProperty("unMachinedQty", obj[6] != null ? obj[6].toString() : "");

                    machineNamesArray.add(machineObj);
                    rejectionSum += obj[3] != null ? Double.parseDouble(obj[3].toString()) : 0.0;
                    reworkSum = obj[4] != null ? Double.parseDouble(obj[4].toString()) : 0.0;
                    doubtfulSum = obj[5] != null ? Double.parseDouble(obj[5].toString()) : 0.0;
                    unMachinedSum = obj[6] != null ? Double.parseDouble(obj[6].toString()) : 0.0;
                }
                response.add("namesArray", machineNamesArray);
                sumObj.addProperty("rejectionSum", rejectionSum);
                sumObj.addProperty("reworkSum", reworkSum);
                sumObj.addProperty("doubtfulSum", doubtfulSum);
                sumObj.addProperty("unMachinedSum", unMachinedSum);
                response.add("sumData", sumObj);
            }
            response.addProperty("reportType", reportType);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            taskLogger.error("Exception => getRejectionReports " + e);
            response.addProperty("message", "Failed to load report");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }


    public Object getMonthInYear(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            String year = jsonRequest.get("year");
            String fromYearMonth = jsonRequest.get("fromYearMonth");
            String toYearMonth = jsonRequest.get("toYearMonth");

            System.out.println("fromYearMonth M: " + fromYearMonth.split("-")[1] + " -> Y: " + fromYearMonth.split("-")[0]);
            System.out.println("toYearMonth M: " + toYearMonth.split("-")[1] + " -> Y: " + toYearMonth.split("-")[0]);

            String date1 = fromYearMonth.split("-")[1] + "/01/" + fromYearMonth.split("-")[0]; // month/date/year
            String date2 = toYearMonth.split("-")[1] + "/12/" + toYearMonth.split("-")[0];

            DateFormat formater = new SimpleDateFormat("MM/dd/yyyy");

            Calendar beginCalendar = Calendar.getInstance();
            Calendar finishCalendar = Calendar.getInstance();

            try {
                beginCalendar.setTime(formater.parse(date1));
                finishCalendar.setTime(formater.parse(date2));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            DateFormat formaterYd = new SimpleDateFormat("01-MMM-YYYY");
            while (beginCalendar.before(finishCalendar)) {
                // add one month to date per loop
                String date = formaterYd.format(beginCalendar.getTime()).toUpperCase();
                System.out.println(date);
                System.out.println("Month >>>>>>>>>>>>>>> " + beginCalendar.getTime().getMonth());
                beginCalendar.add(Calendar.MONTH, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    public Object getEmployeeTasksByAttendanceId(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long attendanceId = Long.valueOf(jsonRequest.get("attendanceId"));

            JsonArray taskArray = new JsonArray();
            List<TaskMaster> taskMasters = taskRepository.findByAttendanceIdAndTaskTypeAndStatusAndTaskStatus(attendanceId, 1, true, "complete");
            for (TaskMaster taskMaster : taskMasters) {
                JsonObject taskObject = new JsonObject();
                taskObject.addProperty("id", taskMaster.getId());
                taskObject.addProperty("jobId", taskMaster.getJob().getId());
                taskObject.addProperty("jobName", taskMaster.getJob().getJobName());
                taskObject.addProperty("jobOperationId", taskMaster.getJobOperation().getId());
                taskObject.addProperty("jobOperationName", taskMaster.getJobOperation().getOperationName());
                taskObject.addProperty("taskStartTime", taskMaster.getStartTime().toString());
                taskObject.addProperty("taskEndTime", taskMaster.getEndTime() != null ?
                        taskMaster.getEndTime().toString() : "");
                taskArray.add(taskObject);
            }
            response.add("response", taskArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            taskLogger.error("Exception => getEmployeeTasksByAttendanceId " + e);
            response.addProperty("message", "Failed to load tasks");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getTasksByTaskId(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject response = new JsonObject();
        try {
            Long taskId = Long.valueOf(jsonRequest.get("taskId"));

            TaskMaster taskMaster = taskRepository.findByIdAndStatus(taskId, true);
            if (taskMaster != null) {
                JsonObject taskObject = new JsonObject();
                taskObject.addProperty("id", taskMaster.getId());
                taskObject.addProperty("employeeName", utility.getEmployeeName(taskMaster.getEmployee()));
                taskObject.addProperty("jobName", taskMaster.getJob().getJobName());
                taskObject.addProperty("jobOperationName", taskMaster.getJobOperation().getOperationName());
                taskObject.addProperty("isMachineCount", taskMaster.getMachine().getIsMachineCount());
                taskObject.addProperty("startCount", taskMaster.getMachineStartCount() != null ?
                        taskMaster.getMachineStartCount() : 0);
                taskObject.addProperty("endCount", taskMaster.getMachineEndCount() != null ?
                        taskMaster.getMachineEndCount() : 0);
                taskObject.addProperty("totalCount", taskMaster.getTotalCount() != null ?
                        taskMaster.getTotalCount() : 0);
                taskObject.addProperty("reworkQty", taskMaster.getReworkQty() != null ?
                        taskMaster.getReworkQty() : 0);
                taskObject.addProperty("machineRejectQty", taskMaster.getMachineRejectQty() != null ?
                        taskMaster.getMachineRejectQty() : 0);
                taskObject.addProperty("doubtfulQty", taskMaster.getDoubtfulQty() != null ?
                        taskMaster.getDoubtfulQty() : 0);
                taskObject.addProperty("unMachinedQty", taskMaster.getUnMachinedQty() != null ?
                        taskMaster.getUnMachinedQty() : 0);
                taskObject.addProperty("okQty", taskMaster.getOkQty() != null ? taskMaster.getOkQty() : 0);
                taskObject.addProperty("startRemark", taskMaster.getRemark() != null ? taskMaster.getRemark() : "");
                taskObject.addProperty("endRemark", taskMaster.getEndRemark() != null ? taskMaster.getEndRemark() : "");
                taskObject.addProperty("preventiveAction", taskMaster.getPreventiveAction() != null ?
                        taskMaster.getPreventiveAction() : "");
                taskObject.addProperty("correctiveAction", taskMaster.getCorrectiveAction() != null ?
                        taskMaster.getCorrectiveAction() : "");

                response.add("response", taskObject);
                response.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                response.addProperty("message", "Task not found, Please contact to admin");
                response.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            taskLogger.error("Exception => getTasksByTaskId " + e);
            response.addProperty("message", "Failed to load tasks");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object updateTaskDataBySupervisor(Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

        try {
            Long taskId = Long.valueOf(requestParam.get("taskId"));
            TaskMaster task = taskRepository.findByIdAndStatus(taskId, true);

            if (task != null) {
                task.setTaskStatus("complete");

                LocalDateTime l1 = task.getStartTime();
                LocalDateTime l2 = task.getEndTime();
                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60));
                double totalTime = SECONDS.between(l1, l2) / 60.0;
                double time = totalTime;
                System.out.println("total time in min " + time);
                double actualTaskTime = task.getActualWorkTime();
                task.setTotalCount(Long.valueOf(requestParam.get("totalCount")));
                task.setReworkQty(Double.valueOf(requestParam.get("reworkQty")));
                task.setMachineRejectQty(Double.valueOf(requestParam.get("machineRejectQty")));
                task.setDoubtfulQty(Double.valueOf(requestParam.get("doubtfulQty")));
                task.setUnMachinedQty(Double.valueOf(requestParam.get("unMachinedQty")));
                task.setOkQty(Double.valueOf(requestParam.get("okQty")));

                Long okQty = Long.valueOf(requestParam.get("okQty"));
                System.out.println("user totalActualTime in minutes " + actualTaskTime);
                double requiredProduction = (actualTaskTime / task.getCycleTime());
                System.out.println("requiredProduction " + requiredProduction);
                double actualProduction = okQty;
                double shortProduction = (actualProduction - requiredProduction);
                double percentageOfTask = ((actualProduction / requiredProduction) * 100.0);
                System.out.println("percentageOfTask " + percentageOfTask);

                task.setActualProduction(actualProduction);
                task.setShortProduction(shortProduction);
                task.setPercentageOfTask(percentageOfTask);

                double productionPoint = (double) okQty * task.getPerJobPoint();
                double productionWorkingHour = productionPoint / 12.5;
                double settingTimeInMinutes = task.getSettingTimeInMin();
                double perMinutePoint = 100.0 / 60;
                double settingTimeInHour = perMinutePoint * settingTimeInMinutes / 100.0;
                double settingTimePoint = 100.0 / 480.0 * settingTimeInMinutes;
                double totalPoint = productionPoint + settingTimePoint;

                double wagesPointBasis = task.getWagesPerPoint() * totalPoint;
                double wagesPcsBasis = okQty * task.getPcsRate();

                task.setProdPoint(productionPoint);
                task.setProdWorkingHour(productionWorkingHour);
                task.setTotalPoint(totalPoint);

                task.setWagesPointBasis(wagesPointBasis);
                task.setWagesPcsBasis(wagesPcsBasis);

                if (requestParam.containsKey("correctiveAction")) {
                    String correctiveAction = requestParam.get("correctiveAction");
                    if (!correctiveAction.equalsIgnoreCase("")) {
                        task.setCorrectiveAction(correctiveAction);
                    }
                }
                if (requestParam.containsKey("preventiveAction")) {
                    String preventiveAction = requestParam.get("preventiveAction");
                    if (!preventiveAction.equalsIgnoreCase("")) {
                        task.setPreventiveAction(preventiveAction);
                    }
                }
                if (requestParam.containsKey("supervisorRemark")) {
                    String supervisorRemark = requestParam.get("supervisorRemark");
                    if (!supervisorRemark.equalsIgnoreCase("")) {
                        task.setSupervisorRemark(supervisorRemark);
                    }
                }
                task.setUpdatedAt(LocalDateTime.now());
                task.setUpdatedBy(employee.getId());
                task.setInstitute(employee.getInstitute());
                try {
                    TaskMaster savedTaskMaster = taskRepository.save(task);
                    updateEmployeeTaskSummary(task.getAttendance());
                    responseMessage.addProperty("message", "Successfully task updated");
                    responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                } catch (Exception e) {
                    taskLogger.error("Failed to update task " + e);
                    e.printStackTrace();
                    System.out.println("Exception " + e.getMessage());
                    responseMessage.addProperty("message", "Failed to update task");
                    responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
            }
        } catch (Exception e) {
            taskLogger.error("failed to update task" + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to update task");
            responseMessage.addProperty("responseStatus", HttpStatus.BAD_REQUEST.value());
        }
        return responseMessage;
    }
}
