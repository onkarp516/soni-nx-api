//package com.truethic.renukaEngg.RenukaEnggAPI.service;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.truethic.renukaEngg.RenukaEnggAPI.model.*;
//import com.truethic.renukaEngg.RenukaEnggAPI.repository.*;
//import com.truethic.renukaEngg.RenukaEnggAPI.response.ResponseMessage;
//import com.truethic.renukaEngg.RenukaEnggAPI.util.JwtTokenUtil;
//import com.truethic.renukaEngg.RenukaEnggAPI.viewRepository.AttendanceViewRepository;
//import com.truethic.renukaEngg.RenukaEnggAPI.views.AttendanceView;
//import com.truethic.renukaEngg.RenukaEnggAPI.views.TaskView;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import javax.servlet.http.HttpServletRequest;
//import java.text.SimpleDateFormat;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//
//import static java.time.temporal.ChronoUnit.SECONDS;
//
//@Service
//@Slf4j
//public class TaskService_bkp {
//    @Autowired
//    private JwtTokenUtil jwtTokenUtil;
//    @Autowired
//    private TaskMasterRepository taskRepository;
//    @Autowired
//    private MachineRepository machineRepository;
//    @Autowired
//    private JobRepository jobRepository;
//    @Autowired
//    private JobOperationRepository jobOperationRepository;
//    @Autowired
//    private WorkBreakRepository workBreakRepository;
//    @Autowired
//    private TaskViewRepository taskViewRepository;
//    @Autowired
//    private AttendanceViewRepository attendanceViewRepository;
//    @Autowired
//    private AttendanceRepository attendanceRepository;
//
//    public Object saveTask(Map<String, String> requestParam, HttpServletRequest request) {
//        ResponseMessage responseMessage = new ResponseMessage();
//        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
//        TaskMaster task = new TaskMaster();
//
//        try {
//            task.setTaskDate(LocalDate.now());
//            task.setEmployee(employee);
//            Integer taskType = Integer.valueOf(requestParam.get("taskType"));
//            task.setTaskType(taskType);
//
//            LocalTime shiftTime = employee.getShift().getWorkingHours();
//            System.out.println("shiftTime " + shiftTime);
//            Integer shiftMinutes = ((shiftTime.getHour() * 60) + shiftTime.getMinute());
//            System.out.println("shiftMinutes in min " + shiftMinutes);
//            Double shiftHours = Double.valueOf((shiftMinutes / 60));
//
//            Double wagesPerDay = employee.getWagesPerDay();
//            task.setWagesPerDay(wagesPerDay);
//            if (taskType == 3) {
//                Long machineId = Long.parseLong(requestParam.get("machineId"));
//                Machine machine = machineRepository.findByIdAndStatus(machineId, true);
//                task.setMachine(machine);
//
//                Integer settingMinutes = Integer.valueOf((requestParam.get("totalTime")));
//                task.setTotalTime(Double.valueOf(requestParam.get("totalTime")));
//
//                Double result1 = (Double.parseDouble("100") / shiftMinutes);
//                Double workPoints = Double.valueOf(result1 * settingMinutes);
//                System.out.println("workPoints " + workPoints);
//
//                Double wagesPerPoint = (wagesPerDay / Double.valueOf(100));
//                Double wagesPointBasis = wagesPerPoint * workPoints;
//
//                Double wagesPerMin = (wagesPerDay / shiftMinutes);
//                Double wagesHourBasis = (wagesPerMin * settingMinutes);
//
//                task.setWorkPoint(Double.valueOf(String.format("%.2f", workPoints)));
//                task.setWagesPerPoint(Double.valueOf(String.format("%.2f", wagesPerPoint)));
//                task.setWagesPointBasis(Double.valueOf(String.format("%.2f", wagesPointBasis)));
//                task.setWagesHourBasis(Double.valueOf(String.format("%.2f", wagesHourBasis)));
//            } else {
//                LocalTime l1 = LocalTime.parse(requestParam.get("startTime"));
//                LocalTime l2 = LocalTime.parse(requestParam.get("endTime"));
//                task.setStartTime(l1);
//                task.setEndTime(l2);
//                System.out.println("SECONDS To MINUTES " + (SECONDS.between(l1, l2) / 60.0));
//                double totalTime = SECONDS.between(l1, l2) / 60.0;
//                Long time = Long.valueOf(String.format("%.0f", totalTime));
//                System.out.println("total time in min " + time);
//                task.setTotalTime(Double.valueOf(time));
//
//                if (taskType == 1) {
//                    Long machineId = Long.parseLong(requestParam.get("machineId"));
//                    Long jobId = Long.parseLong(requestParam.get("jobId"));
//                    Long jobOperationId = Long.parseLong(requestParam.get("jobOperationId"));
//
//                    Machine machine = machineRepository.findByIdAndStatus(machineId, true);
//                    Job job = jobRepository.findByIdAndStatus(jobId, true);
//                    JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(jobOperationId, true);
//
//                    task.setMachine(machine);
//
//                    Integer operationBreakMinutes = jobOperation.getOperationBreakInMin();
//                    System.out.println("operationBreakMinutes " + operationBreakMinutes);
//                    Integer remainingMinutes = shiftMinutes - operationBreakMinutes;
//                    System.out.println("remainingMinutes " + remainingMinutes);
//                    System.out.println("jobOperation.getCycleTime() " + jobOperation.getCycleTime());
//                    Double jobPerShift = Double.valueOf(remainingMinutes) / jobOperation.getCycleTime();
//                    System.out.println("jobPerShift " + jobPerShift);
//                    System.out.println("shiftHours " + shiftHours);
//                    Double jobsPerHour = jobPerShift / shiftHours;
//                    System.out.println("jobsPerHour " + jobsPerHour);
//                    Double jobsPerMin = jobsPerHour / 60;
//                    System.out.println("jobsPerMin " + jobsPerMin);
//
//                    System.out.println("user time in minutes " + time);
//                    Double totalJobsInUserMinutes = jobsPerMin * time;
//                    System.out.println("totalJobsInUserMinutes " + totalJobsInUserMinutes);
//                    Long userJobCount = Long.valueOf(requestParam.get("totalCount"));
//                    System.out.println("userJobCount " + userJobCount);
//                    Double netJobs = userJobCount / totalJobsInUserMinutes;
//                    System.out.println("netJobs " + netJobs);
//                    Double netJobsInPercentage = netJobs * 100;
//                    System.out.println("netJobsInPercentage " + netJobsInPercentage);
//
//                    task.setJobsInEmployeeMinutes(Double.valueOf(String.format("%.2f",totalJobsInUserMinutes)));
//                    task.setNetJobsInPercentage(Double.valueOf(String.format("%.2f",netJobsInPercentage)));
//
//                    task.setAveragePerShift(Double.valueOf(String.format("%.2f",jobPerShift)));
//                    task.setPointPerJob(jobOperation.getPointPerJob());
//                    task.setJob(job);
//                    task.setPcsRate(jobOperation.getPcsRate());
//                    task.setJobOperation(jobOperation);
//                    task.setCycleTime(jobOperation.getCycleTime());
//                    if (machine.getIsMachineCount()) {
//                        task.setMachineStartCount(Long.valueOf(requestParam.get("machineStartCount")));
//                        task.setMachineEndCount(Long.valueOf(requestParam.get("machineEndCount")));
//
//                        machine.setCurrentMachineCount(Long.valueOf(requestParam.get("machineEndCount")));
//                        machineRepository.save(machine);
//                    }
//                    task.setTotalCount(Long.valueOf(requestParam.get("totalCount")));
//
//                    Long productQty = Long.valueOf(requestParam.get("totalCount"));
//                    Double perJobPoint = jobOperation.getPointPerJob();
//                    long workPoints = (long) (productQty * perJobPoint);
//                    System.out.println("workPoints " + workPoints);
//
//                    Double wagesPerPoint = (wagesPerDay / Double.valueOf(100));
//                    Double wagesPointBasis = wagesPerPoint * workPoints;
//
//                    Double wagesPerMin = (wagesPerDay / shiftMinutes);
//                    Double wagesHourBasis = (wagesPerMin * time);
//
//                    task.setWorkPoint(Double.valueOf(workPoints));
//                    task.setWagesPerPoint(Double.valueOf(String.format("%.2f", wagesPerPoint)));
//                    task.setWagesPointBasis(Double.valueOf(String.format("%.2f", wagesPointBasis)));
//                    task.setWagesHourBasis(Double.valueOf(String.format("%.2f", wagesHourBasis)));
//                } else if (taskType == 2) {
//                    Long breakId = Long.valueOf(requestParam.get("breakId"));
//                    WorkBreak workBreak = workBreakRepository.findByIdAndStatus(breakId, true);
//                    task.setWorkBreak(workBreak);
//                    task.setWorkDone(Boolean.parseBoolean(requestParam.get("workDone")));
//
//                    if (workBreak.getIsBreakPaid() == true) {
//                        Double workPoints = ((Double.valueOf("100") / shiftMinutes) * time);
//                        System.out.println("workPoints " + workPoints);
//
//                        Double wagesPointBasis = workPoints * workPoints;
//
//                        Double wagesPerMin = (wagesPerDay / shiftMinutes);
//                        Double wagesHourBasis = (wagesPerMin * time);
//
//                        task.setWorkPoint(Double.valueOf(String.format("%.2f", totalTime)));
//                        task.setWagesPointBasis(Double.valueOf(String.format("%.2f", wagesPointBasis)));
//                        task.setWagesHourBasis(Double.valueOf(String.format("%.2f", wagesHourBasis)));
//                    }
//                }
//
//                String remark = requestParam.get("remark");
//                if (!remark.equalsIgnoreCase("")) {
//                    task.setRemark(remark);
//                }
//            }
//
//            task.setCreatedAt(LocalDateTime.now());
//            task.setCreatedBy(employee.getId());
//            task.setStatus(true);
//            try {
//                taskRepository.save(task);
//                updateEmployeeTaskSummary(employee, LocalDate.now());
//                responseMessage.setMessage("Successfully saved task");
//                responseMessage.setResponseStatus(HttpStatus.OK.value());
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println("Exception " + e.getMessage());
//                responseMessage.setMessage("Failed to save task");
//                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("Exception " + e.getMessage());
//            responseMessage.setMessage("Failed to save task");
//            responseMessage.setResponseStatus(HttpStatus.BAD_REQUEST.value());
//        }
//        return responseMessage;
//    }
//
//    public JsonObject fetchEmployeeTasks(HttpServletRequest request) {
//        JsonObject response = new JsonObject();
//        JsonArray jsonArray = new JsonArray();
//        Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));
//
//        List<Task> taskList = taskRepository.findEmployeeTodaysTask(employee.getId(), true, LocalDate.now(), 1);
//        if (taskList.size() > 0) {
//            for (Task task : taskList) {
//                JsonObject jsonObject = new JsonObject();
//                jsonObject.addProperty("taskId", task.getId());
//                jsonObject.addProperty("taskType", task.getTaskType());
//                if (task.getJob() != null) {
//                    jsonObject.addProperty("jobName", task.getJob().getJobName());
//                } else {
//                    jsonObject.addProperty("jobName", "NA");
//                }
//                if (task.getJobOperation() != null) {
//                    jsonObject.addProperty("jobOperationName", task.getJobOperation().getOperationName());
//                } else {
//                    jsonObject.addProperty("jobOperationName", "NA");
//                }
//                jsonObject.addProperty("machineName", task.getMachine().getName());
//                jsonObject.addProperty("machineStartCount", task.getMachineStartCount());
//                jsonObject.addProperty("machineEndCount", task.getMachineEndCount());
//                jsonObject.addProperty("averagePerShift", task.getAveragePerShift()) ;
//                jsonObject.addProperty("pcsRate", task.getPcsRate());
//                jsonObject.addProperty("totalCount", task.getTotalCount());
//                jsonObject.addProperty("jobsInEmployeeMinutes", task.getJobsInEmployeeMinutes());
//                jsonObject.addProperty("netJobsInPercentage", task.getNetJobsInPercentage());
//                if(task.getNetJobsInPercentage() >= 100) {
//                    jsonObject.addProperty("taskStatus", "Complete");
//                }else{
//                    jsonObject.addProperty("taskStatus", "Incomplete");
//                }
//
//                jsonArray.add(jsonObject);
//            }
//        }
//        response.add("response", jsonArray);
//        response.addProperty("responseStatus", HttpStatus.OK.value());
//        return response;
//    }
//
//    public Object getEmployeeTodayTasks(Map<String, String> request) {
//        ResponseMessage responseMessage = new ResponseMessage();
//        try {
//            Long employeeId = Long.valueOf(request.get("employeeId"));
//            List<TaskView> taskViewList = taskViewRepository.findByEmployeeIdAndTaskDateOrderById(employeeId,
//                    LocalDate.now());
//
//            if(taskViewList.size() > 0) {
//                responseMessage.setResponse(taskViewList);
//                responseMessage.setResponseStatus(HttpStatus.OK.value());
//            }else{
//                responseMessage.setMessage("No tasks available");
//                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            responseMessage.setMessage("Failed to load data");
//            responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//        return responseMessage;
//    }
//
//    public void updateEmployeeTaskSummary(Employee employee, LocalDate localDate){
//        AttendanceView attendanceView = attendanceViewRepository.findByEmployeeIdAndAttendanceDate(employee.getId(),
//                localDate);
//        System.out.println("attendanceView "+attendanceView.toString());
//        if(attendanceView != null) {
//            try {
//                Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), localDate);
////                System.out.println("attendance " + attendance);
//                attendance.setWagesPerDay(employee.getWagesPerDay());
//                Double wagesPerPoint = employee.getWagesPerDay() / Double.parseDouble("100");
//                attendance.setWagesPerPoint(Double.valueOf(String.format("%.2f", wagesPerPoint)));
//                attendance.setTotalProdQty(attendanceView.getTotalProdQty());
//                attendance.setTotalWorkTime(attendanceView.getTotalWorkTime());
//                attendance.setTotalWorkPoint(attendanceView.getTotalWorkPoint());
//                attendance.setWagesPointBasis(Double.valueOf(String.format("%.2f",
//                        attendanceView.getWagesPointBasis())));
//                attendance.setWagesHourBasis(Double.valueOf(String.format("%.2f",attendanceView.getWagesHourBasis())));
//                attendanceRepository.save(attendance);
//            }catch (Exception e){
//                System.out.println("e--------> "+e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
//
//    @Scheduled(cron = "0 0/10 * * * *")
//    public void reportCurrentTime() {
//        log.info("The time is now {}", dateFormat.format(new Date()));
//    }
//
//    public JsonObject fetchEmployeeTasksDetail(Map<String, String> jsonRequest) {
//        JsonObject response = new JsonObject();
//        try {
//            Long taskId = Long.valueOf(jsonRequest.get("taskId"));
//            Task task = taskRepository.findByIdAndStatus(taskId, true);
//
//            JsonObject jsonObject = new JsonObject();
//            if (task != null) {
//                jsonObject.addProperty("taskId", task.getId());
//                jsonObject.addProperty("taskType", task.getTaskType());
//                if (task.getJobOperation() != null) {
//                    jsonObject.addProperty("jobOperationName", task.getJobOperation().getOperationName());
//                } else {
//                    jsonObject.addProperty("jobOperationName", "NA");
//                }
//                jsonObject.addProperty("machineName", task.getMachine().getName());
//                jsonObject.addProperty("machineStartCount", task.getMachineStartCount());
//                jsonObject.addProperty("machineEndCount", task.getMachineEndCount());
//                jsonObject.addProperty("averagePerShift", task.getAveragePerShift());
//                jsonObject.addProperty("pcsRate", task.getPcsRate());
//                jsonObject.addProperty("totalCount", task.getTotalCount());
//                jsonObject.addProperty("jobsInEmployeeMinutes", task.getJobsInEmployeeMinutes());
//                jsonObject.addProperty("netJobsInPercentage", task.getNetJobsInPercentage());
//                if (task.getNetJobsInPercentage() >= 100) {
//                    jsonObject.addProperty("taskStatus", "Complete");
//                } else {
//                    jsonObject.addProperty("taskStatus", "Incomplete");
//                }
//                response.add("response",jsonObject);
//                response.addProperty("responseStatus",HttpStatus.OK.value());
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//            System.out.println("Exception "+e.getMessage());
//            response.addProperty("message","Failed to load data");
//            response.addProperty("responseStatus",HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//
//        return response;
//    }
//}
