package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.repository.*;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import com.truethic.soninx.SoniNxAPI.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Map;

@Service
public class EmployeeInspectionService {

    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    EmployeeInspectionRepository employeeInspectionRepository;

    @Autowired
    JobRepository jobRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    AttendanceRepository attendanceRepository;


    @Autowired
    TaskMasterRepository taskMasterRepository;

    @Autowired
    JobOperationRepository jobOperationRepository;

    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private OperationParameterRepository operationParameterRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private Utility utility;

    private static final Logger inspectionLogger = LoggerFactory.getLogger(EmployeeInspectionService.class);


    public Object createMobileLineInspection(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Employee employee = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            String lrows = jsonRequest.get("empInspectionRows");
            JsonArray jsonArray = new JsonParser().parse(lrows).getAsJsonArray();
            List<EmployeeInspection> employeeInspections = new ArrayList<>();
            try {
                Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("machineId")), true);
                Job job = jobRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("jobId")), true);
                JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("jobOperationId")), true);
                Attendance attendance = attendanceRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("attendanceId")), true);
                TaskMaster taskMaster = taskMasterRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("taskId")), true);

                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject object = jsonArray.get(i).getAsJsonObject();
                    if (object.get("inspectionId").getAsString() != "" && object.get("inspectionId").getAsString() != null) {
                        EmployeeInspection empInspection = new EmployeeInspection();

                        empInspection.setJobNo(jsonRequest.get("jobNo"));
                        empInspection.setInspectionDate(LocalDate.now());
                        empInspection.setInspectionId(object.get("inspectionId").getAsLong());
                        empInspection.setDrawingSize(object.get("drawingSize").getAsString());
                        empInspection.setActualSize(object.get("actualSize").getAsString());
                        empInspection.setResult(object.get("result").getAsBoolean());
                        if (object.has("remark"))
                            empInspection.setRemark(object.get("remark").getAsString());

                        empInspection.setSpecification(object.get("specification").getAsString());
                        empInspection.setFirstParameter(object.get("firstParameter").getAsString());
                        empInspection.setSecondParameter(object.get("secondParameter").getAsString());
                        empInspection.setInstrumentUsed(object.get("instrumentUsed").getAsString());
                        empInspection.setCheckingFrequency(object.get("checkingFrequency").getAsString());
                        empInspection.setControlMethod(object.get("controlMethod").getAsString());

                        empInspection.setEmployee(employee);
                        empInspection.setInstitute(employee.getInstitute());
                        empInspection.setStatus(true);

                        if (machine != null) {
                            empInspection.setMachine(machine);
                        }
                        if (job != null) {
                            empInspection.setJob(job);
                        }
                        if (jobOperation != null) {
                            empInspection.setJobOperation(jobOperation);
                        }
                        if (attendance != null) {
                            empInspection.setAttendance(attendance);
                        }
                        if (taskMaster != null) {
                            empInspection.setTaskMaster(taskMaster);
                        }
                        empInspection.setCreatedBy(employee.getId());
                        empInspection.setCreatedAt(LocalDateTime.now());
                        empInspection.setInstitute(employee.getInstitute());
                        employeeInspections.add(empInspection);
                    }
                }

                employeeInspectionRepository.saveAll(employeeInspections);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                responseMessage.addProperty("message", "Line Inspection Successfully");
            } catch (Exception e) {
                System.out.println("Exception " + e.getMessage());
                e.printStackTrace();
                responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                responseMessage.addProperty("message", "Internal Server Error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.addProperty("message", "Internal Server Error");
        }
        return responseMessage;
    }

    public Object getTaskLineInspectionListForSupervisor(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray empInspectionArray = new JsonArray();
        try {
            Long taskId = Long.valueOf(jsonRequest.get("taskId"));
            Long supervisorTaskId = Long.valueOf(jsonRequest.get("supervisorTaskId"));

            System.out.println("taskId " + taskId);
            TaskMaster taskMaster = taskMasterRepository.findByIdAndStatus(taskId, true);

            System.out.println("taskMaster.getJob().getId() " + taskMaster.getJob().getId());
            System.out.println("taskMaster.getJobOperation().getId() " + taskMaster.getJobOperation().getId());
//        List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperation(
//                taskMaster.getJob().getId(), taskMaster.getJobOperation().getId(), true);
            JsonArray jobNoArray = new JsonArray();

            JsonArray actualSizeArray = new JsonArray();
            JsonArray resultArray = new JsonArray();
            List<Object[]> jobList = employeeInspectionRepository.getJobNosForSupervisor(taskId, supervisorTaskId);
            for (int j = 0; j < jobList.size(); j++) {
                Object[] jobObject = jobList.get(j);

                JsonObject jobObj = null;
                Employee employee = employeeRepository.findById(Long.valueOf(jobObject[1].toString())).get();

                jobObj = new JsonObject();
                jobObj.addProperty("jobNo", jobObject[0].toString());
                jobObj.addProperty("empName", utility.getEmployeeName(employee));
                jobObj.addProperty("createdAt", jobObject[2].toString());
                jobNoArray.add(jobObj);
            }

            List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperationForSupervisor(taskMaster.getId(), supervisorTaskId);
            JsonArray drawingArray = new JsonArray();
            for (int i = 0; i < drawingList.size(); i++) {
                Object[] drawingObject = drawingList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("specification", drawingObject[0].toString());
                jsonObject.addProperty("drawingSize", drawingObject[2].toString());

                drawingArray.add(jsonObject);

                JsonArray sizeInnerArr = new JsonArray();
                for (int k = 0; k < jobList.size(); k++) {

                    String query2 = "SELECT actual_size, result FROM employee_inspection_tbl WHERE job_no='" + jobList.get(k)[0].toString()
                            + "' AND inspection_id='" + drawingObject[1].toString() + "' AND task_id=" + taskId + " ";
                    System.out.println("query2 " + query2);
                    Query q2 = entityManager.createNativeQuery(query2);
                    List<Object[]> employeeInspections = q2.getResultList();
                    System.out.println("employeeInspections.size() " + employeeInspections.size());
                    JsonArray actualArray = new JsonArray();
                    if (employeeInspections.size() == 0) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        sizeInnerArr.add(inspObj);
                    }
                    for (int j = 0; j < employeeInspections.size(); j++) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        if (employeeInspections != null) {
                            inspObj.addProperty("actualSize", employeeInspections.get(j)[0].toString());
                            inspObj.addProperty("sizeResult", employeeInspections.get(j)[1].toString());
                        }
                        sizeInnerArr.add(inspObj);
                    }
                }
                actualSizeArray.add(sizeInnerArr);
            }

            responseMessage.add("actualSizeArray", actualSizeArray);
            responseMessage.add("resultArray", resultArray);
            responseMessage.add("jobNoArray", jobNoArray);
            responseMessage.add("drawingArray", drawingArray);

            /*List<EmployeeInspection> employeeInspectionList = employeeInspectionRepository.findByTaskMasterIdAndStatus(taskId, true);
            for (EmployeeInspection employeeInspection : employeeInspectionList) {
                JsonObject inspectionObj = new JsonObject();
                inspectionObj.addProperty("empInspectionId", employeeInspection.getId());
                inspectionObj.addProperty("jobNo", employeeInspection.getJobNo());
                inspectionObj.addProperty("drawingSize", employeeInspection.getDrawingSize());
                inspectionObj.addProperty("actualSize", employeeInspection.getActualSize());
                inspectionObj.addProperty("remark", employeeInspection.getRemark());
                inspectionObj.addProperty("result", employeeInspection.getResult());

                inspectionObj.addProperty("specification", employeeInspection.getSpecification());
                inspectionObj.addProperty("firstParameter", employeeInspection.getFirstParameter());
                inspectionObj.addProperty("secondParameter", employeeInspection.getSecondParameter());
                inspectionObj.addProperty("instrumentUsed", employeeInspection.getInstrumentUsed());
                inspectionObj.addProperty("checkingFrequency", employeeInspection.getCheckingFrequency());
                inspectionObj.addProperty("controlMethod", employeeInspection.getControlMethod());

                empInspectionArray.add(inspectionObj);
            }
            responseMessage.add("response", empInspectionArray);*/
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
        }
        return responseMessage;
    }

    public Object getTaskLineInspectionList(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray empInspectionArray = new JsonArray();
        try {
            Long taskId = Long.valueOf(jsonRequest.get("taskId"));
//            Long supervisorTaskId = null;
//            if(jsonRequest.containsKey("supervisorTaskId")){
//                supervisorTaskId = Long.valueOf(jsonRequest.get("supervisorTaskId"));
//            }
            System.out.println("taskId " + taskId);
            TaskMaster taskMaster = taskMasterRepository.findByIdAndStatus(taskId, true);

            System.out.println("taskMaster.getJob().getId() " + taskMaster.getJob().getId());
            System.out.println("taskMaster.getJobOperation().getId() " + taskMaster.getJobOperation().getId());
//        List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperation(
//                taskMaster.getJob().getId(), taskMaster.getJobOperation().getId(), true);
            JsonArray jobNoArray = new JsonArray();

            JsonArray actualSizeArray = new JsonArray();
            JsonArray resultArray = new JsonArray();
            List<Object[]> jobList = employeeInspectionRepository.getJobNos(taskId);
            for (int j = 0; j < jobList.size(); j++) {
                Object[] jobObject = jobList.get(j);

                JsonObject jobObj = null;
                Employee employee = employeeRepository.findById(Long.valueOf(jobObject[1].toString())).get();

                jobObj = new JsonObject();
                jobObj.addProperty("jobNo", jobObject[0].toString());
                jobObj.addProperty("empName", utility.getEmployeeName(employee));
                jobObj.addProperty("createdAt", jobObject[2].toString());
                jobObj.addProperty("isSupervisorAdded", jobObject[3] != null ? true : false);
                jobNoArray.add(jobObj);
            }

            List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperation(taskMaster.getId());
            JsonArray drawingArray = new JsonArray();
            for (int i = 0; i < drawingList.size(); i++) {
                Object[] drawingObject = drawingList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("specification", drawingObject[0].toString());
                jsonObject.addProperty("drawingSize", drawingObject[2].toString());

                drawingArray.add(jsonObject);

                JsonArray sizeInnerArr = new JsonArray();
                for (int k = 0; k < jobList.size(); k++) {

                    String query2 = "SELECT actual_size, result FROM employee_inspection_tbl WHERE job_no='" + jobList.get(k)[0].toString()
                            + "' AND inspection_id='" + drawingObject[1].toString() + "' AND task_id=" + taskId + " ";
                    System.out.println("query2 " + query2);
                    Query q2 = entityManager.createNativeQuery(query2);
                    List<Object[]> employeeInspections = q2.getResultList();
                    System.out.println("employeeInspections.size() " + employeeInspections.size());
                    JsonArray actualArray = new JsonArray();
                    if (employeeInspections.size() == 0) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        sizeInnerArr.add(inspObj);
                    }
                    for (int j = 0; j < employeeInspections.size(); j++) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        if (employeeInspections != null) {
                            inspObj.addProperty("actualSize", employeeInspections.get(j)[0].toString());
                            inspObj.addProperty("sizeResult", employeeInspections.get(j)[1].toString());
                        }
                        sizeInnerArr.add(inspObj);
                    }
                }
                actualSizeArray.add(sizeInnerArr);
            }

            responseMessage.add("actualSizeArray", actualSizeArray);
            responseMessage.add("resultArray", resultArray);
            responseMessage.add("jobNoArray", jobNoArray);
            responseMessage.add("drawingArray", drawingArray);

            /*List<EmployeeInspection> employeeInspectionList = employeeInspectionRepository.findByTaskMasterIdAndStatus(taskId, true);
            for (EmployeeInspection employeeInspection : employeeInspectionList) {
                JsonObject inspectionObj = new JsonObject();
                inspectionObj.addProperty("empInspectionId", employeeInspection.getId());
                inspectionObj.addProperty("jobNo", employeeInspection.getJobNo());
                inspectionObj.addProperty("drawingSize", employeeInspection.getDrawingSize());
                inspectionObj.addProperty("actualSize", employeeInspection.getActualSize());
                inspectionObj.addProperty("remark", employeeInspection.getRemark());
                inspectionObj.addProperty("result", employeeInspection.getResult());

                inspectionObj.addProperty("specification", employeeInspection.getSpecification());
                inspectionObj.addProperty("firstParameter", employeeInspection.getFirstParameter());
                inspectionObj.addProperty("secondParameter", employeeInspection.getSecondParameter());
                inspectionObj.addProperty("instrumentUsed", employeeInspection.getInstrumentUsed());
                inspectionObj.addProperty("checkingFrequency", employeeInspection.getCheckingFrequency());
                inspectionObj.addProperty("controlMethod", employeeInspection.getControlMethod());

                empInspectionArray.add(inspectionObj);
            }
            responseMessage.add("response", empInspectionArray);*/
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
        }
        return responseMessage;
    }

    public Object manualCreateLineInspection(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        try {
            Employee supervisor = jwtTokenUtil.getEmployeeDataFromToken(request.getHeader("Authorization").substring(7));

            Employee employee = employeeRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("employeeId")), true);
            if (employee != null) {
                Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDateAndStatus(employee.getId(), LocalDate.now(), true);
                if (attendance != null) {

//                    Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("machineId")), true);
//                    Job job = jobRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("jobId")), true);
//                    Attendance attendance = attendanceRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("attendanceId")), true);
                    TaskMaster superVisorTaskMaster = taskMasterRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("supervisorTaskId")), true);
                    TaskMaster employeeTaskMaster = taskMasterRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("employeeTaskId")), true);
                    JobOperation jobOperation = jobOperationRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("jobOperationId")), true);

                    String lrows = jsonRequest.get("empInspectionRows");
                    JsonArray jsonArray = new JsonParser().parse(lrows).getAsJsonArray();
                    List<EmployeeInspection> employeeInspections = new ArrayList<>();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject object = jsonArray.get(i).getAsJsonObject();
                        if (object.get("inspectionId").getAsString() != "" && object.get("inspectionId").getAsString() != null) {
                            EmployeeInspection empInspection = new EmployeeInspection();

                            empInspection.setInspectionDate(LocalDate.now());
                            empInspection.setJobNo(jsonRequest.get("jobNo"));
                            empInspection.setInspectionId(object.get("inspectionId").getAsLong());
                            empInspection.setDrawingSize(object.get("drawingSize").getAsString());
                            empInspection.setActualSize(object.get("actualSize").getAsString());
                            empInspection.setResult(object.get("result").getAsBoolean());
                            empInspection.setRemark(object.get("remark") != null ? object.get("remark").getAsString():"");

                            empInspection.setSpecification(object.get("specification").getAsString());
                            empInspection.setFirstParameter(object.get("firstParameter").getAsString());
                            empInspection.setSecondParameter(object.get("secondParameter").getAsString());
                            empInspection.setInstrumentUsed(object.get("instrumentUsed").getAsString());
                            empInspection.setCheckingFrequency(object.get("checkingFrequency").getAsString());
                            empInspection.setControlMethod(object.get("controlMethod").getAsString());

                            empInspection.setEmployee(employee);
                            empInspection.setInstitute(employee.getInstitute());
                            empInspection.setStatus(true);
                            empInspection.setInstitute(supervisor.getInstitute());
//                            Machine machine = machineRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("machineId")), true);
//                            if (machine != null) {
//                                empInspection.setMachine(machine);
//                            }
//                            Job job = jobRepository.findByIdAndStatus(Long.parseLong(jsonRequest.get("jobId")), true);
//                            if (job != null) {
//                                empInspection.setJob(job);
//                            }
//                            if (jobOperation != null) {
//                                empInspection.setJobOperation(jobOperation);
//                            }
                            empInspection.setMachine(employeeTaskMaster.getMachine());
                            empInspection.setJob(employeeTaskMaster.getJob());
                            empInspection.setJobOperation(employeeTaskMaster.getJobOperation());
                            empInspection.setAttendance(attendance);
                            empInspection.setTaskMaster(employeeTaskMaster);
                            empInspection.setSupervisorTaskMaster(superVisorTaskMaster);
                            empInspection.setCreatedBy(supervisor.getId());
                            empInspection.setCreatedAt(LocalDateTime.now());

                            employeeInspections.add(empInspection);
                        }
                    }
                    try {
                        employeeInspectionRepository.saveAll(employeeInspections);
                        responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
                        responseMessage.addProperty("message", "Line Inspection Successfully");
                    } catch (Exception e) {
                        System.out.println("Exception " + e.getMessage());
                        e.printStackTrace();
                        responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
                        responseMessage.addProperty("message", "Internal Server Error");
                    }
                } else {
                    responseMessage.addProperty("message", "Employee not check in, Please check in attendance");
                    responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
                }
            } else {
                responseMessage.addProperty("message", "Employee not exists, Please contact to admin");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseMessage.addProperty("message", "Internal Server Error");
        }
        return responseMessage;
    }

    public Object getManualLineInspectionList(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        JsonArray empInspectionArray = new JsonArray();
        try {
            Long employeeId = Long.valueOf(jsonRequest.get("employeeId"));
            Long machineId = Long.valueOf(jsonRequest.get("machineId"));
            Long jobId = Long.valueOf(jsonRequest.get("jobId"));
            Long jobOperationId = Long.valueOf(jsonRequest.get("jobOperationId"));
            System.out.println("employeeId " + employeeId);
            System.out.println("machineId " + machineId);
            System.out.println("jobId " + jobId);
            System.out.println("jobOperationId " + jobOperationId);

//            List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperation(jobId, jobOperationId, true);
            List<Object[]> drawingList = operationParameterRepository.getDrawingListByOperation(jobId);
            JsonArray drawingArray = new JsonArray();
            JsonArray jobNoArray = new JsonArray();
            for (int i = 0; i < drawingList.size(); i++) {
                Object[] drawingObject = drawingList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("specification", drawingObject[1].toString());
                jsonObject.addProperty("drawingSize", drawingObject[2].toString() + "/" + drawingObject[3].toString());

                drawingArray.add(jsonObject);
            }

            JsonArray resultArray = new JsonArray();
            List<Object[]> jobList = employeeInspectionRepository.getJobNosByEmployeeAndOperationAndDate(employeeId, jobOperationId, LocalDate.now());
            for (int j = 0; j < jobList.size(); j++) {
                Object[] jobObject = jobList.get(j);
                JsonObject jobObj = null;
                Employee employee = employeeRepository.findById(Long.valueOf(jobObject[1].toString())).get();

                jobObj = new JsonObject();
                jobObj.addProperty("jobNo", jobObject[0].toString());
                jobObj.addProperty("empName", utility.getEmployeeName(employee));
                jobObj.addProperty("createdAt", jobObject[2].toString());
                jobNoArray.add(jobObj);
//                jobNoArray.add(jobObject[0].toString());


                List<EmployeeInspection> employeeInspections = employeeInspectionRepository.findByJobNoAndEmployeeIdAndInspectionDate(jobObject[0].toString(), employeeId, LocalDate.now());
                JsonArray actualArray = new JsonArray();
                for (EmployeeInspection employeeInspection : employeeInspections) {
                    JsonObject empInspectionObject = new JsonObject();
                    empInspectionObject.addProperty("result", employeeInspection.getResult());
                    empInspectionObject.addProperty("actualSize", employeeInspection.getActualSize());

                    actualArray.add(empInspectionObject);
                }
                resultArray.add(actualArray);
            }

            responseMessage.add("resultArray", resultArray);
            responseMessage.add("jobNoArray", jobNoArray);
            responseMessage.add("drawingArray", drawingArray);

      /*  List<EmployeeInspection> employeeInspectionList = employeeInspectionRepository.findByTaskMasterIdAndStatus(taskId, true);
        for (EmployeeInspection employeeInspection : employeeInspectionList) {
            JsonObject inspectionObj = new JsonObject();
            inspectionObj.addProperty("empInspectionId", employeeInspection.getId());
            inspectionObj.addProperty("jobNo", employeeInspection.getJobNo());
            inspectionObj.addProperty("drawingSize", employeeInspection.getDrawingSize());
            inspectionObj.addProperty("actualSize", employeeInspection.getActualSize());
            inspectionObj.addProperty("remark", employeeInspection.getRemark());
            inspectionObj.addProperty("result", employeeInspection.getResult());

            inspectionObj.addProperty("specification", employeeInspection.getSpecification());
            inspectionObj.addProperty("firstParameter", employeeInspection.getFirstParameter());
            inspectionObj.addProperty("secondParameter", employeeInspection.getSecondParameter());
            inspectionObj.addProperty("instrumentUsed", employeeInspection.getInstrumentUsed());
            inspectionObj.addProperty("checkingFrequency", employeeInspection.getCheckingFrequency());
            inspectionObj.addProperty("controlMethod", employeeInspection.getControlMethod());

            empInspectionArray.add(inspectionObj);
        }*/
            responseMessage.add("response", empInspectionArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception  " + e.getMessage());
        }
        return responseMessage;
    }

    public Object getLineInspectionListWithFilter(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");
            String jobId = jsonRequest.get("jobId");
            String jobOperationId = jsonRequest.get("jobOperationId");
            String employeeId = jsonRequest.get("employeeId");

            List<Object[]> jobNoList = new ArrayList<>();
            String query = "SELECT job_no, employee_id, created_at FROM employee_inspection_tbl WHERE institute_id="+users.getInstitute().getId()+" AND inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' ";

            if (!employeeId.equalsIgnoreCase(""))
                query = query + " AND employee_id=" + employeeId + " ";

            if (!machineId.equalsIgnoreCase(""))
                query = query + " AND machine_id=" + machineId + " ";

            if (!jobId.equalsIgnoreCase(""))
                query = query + " AND job_id=" + jobId + " ";

            if (!jobOperationId.equalsIgnoreCase(""))
                query = query + " AND job_operation_id=" + jobOperationId + " ";

            query = query + " GROUP BY job_no";
            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            jobNoList = q.getResultList();
            System.out.println("jobNoList.size() " + jobNoList.size());

            JsonArray actualSizeArray = new JsonArray();
            JsonArray jobNoArray = new JsonArray();
            JsonArray inspectionResultArray = new JsonArray();
            JsonArray jobResultArray = new JsonArray();
            for (int i = 0; i < jobNoList.size(); i++) {
                Object[] obj = jobNoList.get(i);
                Employee employee = employeeRepository.findById(Long.valueOf(obj[1].toString())).get();

                JsonObject jobObj = new JsonObject();
                jobObj.addProperty("jobNo", obj[0].toString());
                jobObj.addProperty("empName", utility.getEmployeeName(employee));
                jobObj.addProperty("createdAt", obj[2].toString());
                jobNoArray.add(jobObj);
//                jobNoArray.add(String.valueOf(jobNoList.get(i)));
            }

            List<Object[]> specificationList = new ArrayList<>();
            String query1 = "SELECT inspection_id, specification,drawing_size FROM employee_inspection_tbl WHERE inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' ";

            if (!employeeId.equalsIgnoreCase(""))
                query1 = query1 + " AND employee_id=" + employeeId + " ";

            if (!machineId.equalsIgnoreCase(""))
                query1 = query1 + " AND machine_id=" + machineId + " ";

            if (!jobId.equalsIgnoreCase(""))
                query1 = query1 + " AND job_id=" + jobId + " ";

            if (!jobOperationId.equalsIgnoreCase(""))
                query1 = query1 + " AND job_operation_id=" + jobOperationId + " ";

            query1 = query1 + " GROUP BY drawing_size";
            System.out.println("query1 " + query1);
            Query q1 = entityManager.createNativeQuery(query1);
            specificationList = q1.getResultList();
            System.out.println("specificationList.size() " + specificationList.size());

            JsonArray drawingArray = new JsonArray();
            for (int i = 0; i < specificationList.size(); i++) {
                Object[] drawingObject = specificationList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("inspectionId", drawingObject[0].toString());
                jsonObject.addProperty("specification", drawingObject[1].toString());
                jsonObject.addProperty("drawingSize", drawingObject[2].toString());

                drawingArray.add(jsonObject);

                JsonArray sizeInnerArr = new JsonArray();
                for (int k = 0; k < jobNoList.size(); k++) {

//                    Object[] jobObject = jobNoList.get(k);

//                    System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobNoList.get(k) + " INSP ID " + drawingObject[0].toString() + " specification " + drawingObject[1].toString());
                    String query2 = "SELECT actual_size, result FROM employee_inspection_tbl WHERE job_no='" + jobNoList.get(k)[0].toString()
                            + "' AND inspection_id='" + drawingObject[0].toString() + "' AND inspection_date BETWEEN '" + fromDate
                            + "' AND '" + toDate + "' ";

                    if (!employeeId.equalsIgnoreCase(""))
                        query2 = query2 + " AND employee_id=" + employeeId + " ";

                    if (!machineId.equalsIgnoreCase(""))
                        query2 = query2 + " AND machine_id=" + machineId + " ";

                    if (!jobId.equalsIgnoreCase(""))
                        query2 = query2 + " AND job_id=" + jobId + " ";

                    if (!jobOperationId.equalsIgnoreCase(""))
                        query2 = query2 + " AND job_operation_id=" + jobOperationId + " ";

                    System.out.println("query2 " + query2);
                    Query q2 = entityManager.createNativeQuery(query2);
                    List<Object[]> employeeInspections = q2.getResultList();
                    System.out.println("employeeInspections.size() " + employeeInspections.size());
                   /* Object[] employeeInspections = (Object[]) q2.getSingleResult();
                    System.out.println(" <<<<<<<<<<<<<<<<<<<<<< employeeInspections >>>>>>>>>>>> " + employeeInspections);*/
                    if (employeeInspections.size() == 0) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        sizeInnerArr.add(inspObj);
                    }
                    for (int j = 0; j < employeeInspections.size(); j++) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        if (employeeInspections != null) {
                            inspObj.addProperty("actualSize", employeeInspections.get(j)[0].toString());
                            inspObj.addProperty("sizeResult", employeeInspections.get(j)[1].toString());
                        }
                        sizeInnerArr.add(inspObj);
                    }
                }
                actualSizeArray.add(sizeInnerArr);
            }

            System.out.println("+++++++++++++++++++++++++ jobNoArray.size() >>> " + jobNoArray.size());
            System.out.println("+++++++++++++++++++++++++ actualSizeArray.size() >>> " + actualSizeArray.size());
            for (int ai = 0; ai < jobNoArray.size(); ai++) {
//                System.out.println("+++++++++++++++++++++++++ ai >>> " + ai);
                boolean jobResult = true;
                for (int ji = 0; ji < actualSizeArray.size(); ji++) {
                    try {
                        JsonObject jsonObject = actualSizeArray.get(ji).getAsJsonArray().get(ai).getAsJsonObject();
                        if (jsonObject != null) {
//                            System.out.println("jsonObject.get(sizeResult).getAsString() <<<<<<<<<<<<<< " + jsonObject.get("sizeResult").getAsString());
                            if (!jsonObject.get("sizeResult").getAsString().equalsIgnoreCase("") && jsonObject.get("sizeResult").getAsBoolean() == false) {
                                jobResult = false;
                            }
                        }

                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("IndexOutOfBoundsException catched ::::::::::::::::::::::::::: ");
                    }
                }
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("result", jobResult);
                jobResultArray.add(jsonObject1);
            }


            responseMessage.add("jobResultArray", jobResultArray);
            responseMessage.add("resultArray", actualSizeArray);
            responseMessage.add("inspectionResultArray", inspectionResultArray);
            responseMessage.add("jobNoArray", jobNoArray);
            responseMessage.add("drawingArray", drawingArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception  " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

//    public Object getLineInspectionListWithFilterOld(Map<String, String> jsonRequest, HttpServletRequest request) {
//        JsonObject responseMessage = new JsonObject();
//        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
//        try {
//            String fromDate = jsonRequest.get("fromDate");
//            String toDate = jsonRequest.get("toDate");
//            String machineId = jsonRequest.get("machineId");
//            String jobId = jsonRequest.get("jobId");
//            String jobOperationId = jsonRequest.get("jobOperationId");
//            String employeeId = jsonRequest.get("employeeId");
//
//            List<Object[]> jobNoList = new ArrayList<>();
//            if (employeeId.equalsIgnoreCase(""))
//                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndInstituteId(jobId, jobOperationId, fromDate, toDate,users.getInstitute().getId());
//            else
//                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndEmployeeAndInstituteId(jobId, jobOperationId, employeeId, fromDate, toDate,users.getInstitute().getId());
//
//            System.out.println("jobNoList.size() " + jobNoList.size());
//
//            JsonArray actualSizeArray = new JsonArray();
//            JsonArray jobNoArray = new JsonArray();
//            JsonArray inspectionResultArray = new JsonArray();
//            for (int i = 0; i < jobNoList.size(); i++) {
//                Object[] jobObject = jobNoList.get(i);
//
//                jobNoArray.add(jobObject[0].toString());
//            }
//
//            List<Object[]> specificationList = new ArrayList<>();
//            if (employeeId.equalsIgnoreCase(""))
//                specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetails(jobId, jobOperationId, fromDate, toDate);
//            else
//                specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, fromDate, toDate);
//
//            System.out.println("specificationList.size() " + specificationList.size());
//            JsonArray drawingArray = new JsonArray();
//            for (int i = 0; i < specificationList.size(); i++) {
//                Object[] drawingObject = specificationList.get(i);
//
//                JsonObject jsonObject = new JsonObject();
//                jsonObject.addProperty("inspectionId", drawingObject[0].toString());
//                jsonObject.addProperty("specification", drawingObject[1].toString());
//                jsonObject.addProperty("drawingSize", drawingObject[2].toString());
//
//                drawingArray.add(jsonObject);
//
//                JsonArray sizeInnerArr = new JsonArray();
//                for (int k = 0; k < jobNoList.size(); k++) {
//                    Object[] jobObject = jobNoList.get(k);
//
//                    System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobObject[0].toString() + " INSP ID " + drawingObject[0].toString());
//                    String employeeInspections = null;
//                    if (employeeId.equalsIgnoreCase(""))
//                        employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetails(jobId, jobOperationId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
//                    else
//                        employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
//
//                    System.out.println(" <<<<<<<<<<<<<<<<<<<<<< employeeInspections >>>>>>>>>>>> " + employeeInspections);
//                    JsonObject inspObj = new JsonObject();
//                    inspObj.addProperty("actualSize", "-");
//                    inspObj.addProperty("sizeResult", "");
//                    if (employeeInspections != null) {
//                        inspObj.addProperty("actualSize", employeeInspections.split(",")[0]);
//                        inspObj.addProperty("sizeResult", employeeInspections.split(",")[1]);
//
//                    }
//                    sizeInnerArr.add(inspObj);
//                }
//                actualSizeArray.add(sizeInnerArr);
//            }
//
//
//            responseMessage.add("resultArray", actualSizeArray);
//            responseMessage.add("inspectionResultArray", inspectionResultArray);
//            responseMessage.add("jobNoArray", jobNoArray);
//            responseMessage.add("drawingArray", drawingArray);
//            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.out.println("Exception  " + e.getMessage());
//            responseMessage.addProperty("message", "Failed to load data");
//            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
//        }
//        return responseMessage;
//    }


    public InputStream exportExcelEmployeeInspection(Map<String, String> jsonRequest, HttpServletRequest request) {
        try {
            String[] headers = {"Specification", "Drawing Size"};
            String sheetName = "InspectionSheet";

            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");
            String jobId = jsonRequest.get("jobId");
            String jobOperationId = jsonRequest.get("jobOperationId");
            String employeeId = jsonRequest.get("employeeId");

            List<Object[]> jobNoList = new ArrayList<>();
            String query = "SELECT job_no, employee_id, created_at FROM employee_inspection_tbl WHERE inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' ";

            if (!employeeId.equalsIgnoreCase(""))
                query = query + " AND employee_id=" + employeeId + " ";

            if (!machineId.equalsIgnoreCase(""))
                query = query + " AND machine_id=" + machineId + " ";

            if (!jobId.equalsIgnoreCase(""))
                query = query + " AND job_id=" + jobId + " ";

            if (!jobOperationId.equalsIgnoreCase(""))
                query = query + " AND job_operation_id=" + jobOperationId + " ";

            query = query + " GROUP BY job_no";
            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            jobNoList = q.getResultList();
            System.out.println("jobNoList.size() " + jobNoList.size());

            JsonArray jobResultArray = new JsonArray();
            JsonArray actualSizeArray = new JsonArray();
            JsonArray jobNoArray = new JsonArray();
            JsonArray inspectionResultArray = new JsonArray();
            for (int i = 0; i < jobNoList.size(); i++) {
//                jobNoArray.add(String.valueOf(jobNoList.get(i)));
                Object[] obj = jobNoList.get(i);
                Employee employee = employeeRepository.findById(Long.valueOf(obj[1].toString())).get();

                JsonObject jobObj = new JsonObject();
                jobObj.addProperty("jobNo", obj[0].toString());
                jobObj.addProperty("empName", utility.getEmployeeName(employee));
                jobObj.addProperty("createdAt", obj[2].toString());
                jobNoArray.add(jobObj);
            }

            List<Object[]> specificationList = new ArrayList<>();
            String query1 = "SELECT inspection_id, specification,drawing_size FROM employee_inspection_tbl WHERE inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' ";

            if (!employeeId.equalsIgnoreCase(""))
                query1 = query1 + " AND employee_id=" + employeeId + " ";

            if (!machineId.equalsIgnoreCase(""))
                query1 = query1 + " AND machine_id=" + machineId + " ";

            if (!jobId.equalsIgnoreCase(""))
                query1 = query1 + " AND job_id=" + jobId + " ";

            if (!jobOperationId.equalsIgnoreCase(""))
                query1 = query1 + " AND job_operation_id=" + jobOperationId + " ";

            query1 = query1 + " GROUP BY drawing_size";
            System.out.println("query1 " + query1);
            Query q1 = entityManager.createNativeQuery(query1);
            specificationList = q1.getResultList();
            System.out.println("specificationList.size() " + specificationList.size());

            for (int i = 0; i < specificationList.size(); i++) {
                Object[] drawingObject = specificationList.get(i);

                JsonArray sizeInnerArr = new JsonArray();
                for (int k = 0; k < jobNoList.size(); k++) {
//                    Object[] jobObject = jobNoList.get(k);

                    System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobNoList.get(k) + " INSP ID " + drawingObject[0].toString() + " specification " + drawingObject[1].toString());
                    String query2 = "SELECT actual_size, result FROM employee_inspection_tbl WHERE job_no='" + jobNoList.get(k)[0].toString()
                            + "' AND inspection_id='" + drawingObject[0].toString() + "' AND inspection_date BETWEEN '" + fromDate
                            + "' AND '" + toDate + "' ";

                    if (!employeeId.equalsIgnoreCase(""))
                        query2 = query2 + " AND employee_id=" + employeeId + " ";

                    if (!machineId.equalsIgnoreCase(""))
                        query2 = query2 + " AND machine_id=" + machineId + " ";

                    if (!jobId.equalsIgnoreCase(""))
                        query2 = query2 + " AND job_id=" + jobId + " ";

                    if (!jobOperationId.equalsIgnoreCase(""))
                        query2 = query2 + " AND job_operation_id=" + jobOperationId + " ";

                    System.out.println("query2 " + query2);
                    Query q2 = entityManager.createNativeQuery(query2);
                    List<Object[]> employeeInspections = q2.getResultList();
                    System.out.println("employeeInspections.size() " + employeeInspections.size());

                    if (employeeInspections.size() == 0) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        sizeInnerArr.add(inspObj);
                    }
                    for (int j = 0; j < employeeInspections.size(); j++) {
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "-");
                        inspObj.addProperty("sizeResult", "");
                        if (employeeInspections != null) {
                            inspObj.addProperty("actualSize", employeeInspections.get(j)[0].toString());
                            inspObj.addProperty("sizeResult", employeeInspections.get(j)[1].toString());

                        }
                        sizeInnerArr.add(inspObj);
                    }
                }
                actualSizeArray.add(sizeInnerArr);
            }


            System.out.println("+++++++++++++++++++++++++ jobNoArray.size() >>> " + jobNoArray.size());
            System.out.println("+++++++++++++++++++++++++ actualSizeArray.size() >>> " + actualSizeArray.size());
            for (int ai = 0; ai < jobNoArray.size(); ai++) {
                boolean jobResult = true;
                for (int ji = 0; ji < actualSizeArray.size(); ji++) {
                    try {
                        JsonObject jsonObject = actualSizeArray.get(ji).getAsJsonArray().get(ai).getAsJsonObject();
                        if (jsonObject != null) {
//                            System.out.println("jsonObject.get(sizeResult).getAsString() <<<<<<<<<<<<<< " + jsonObject.get("sizeResult").getAsString());
                            if (!jsonObject.get("sizeResult").getAsString().equalsIgnoreCase("") && jsonObject.get("sizeResult").getAsBoolean() == false) {
                                jobResult = false;
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        System.out.println("IndexOutOfBoundsException catched ::::::::::::::::::::::::::: ");
                    }
                }
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("result", jobResult);
                jobResultArray.add(jsonObject1);
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet(sheetName);

                // Header
                Row headerRow = sheet.createRow(0);

                Font font = workbook.createFont();
                font.setBold(true);

                // Define header cell style
                CellStyle headerCellStyle = workbook.createCellStyle();
                headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerCellStyle.setFont(font);

                CellStyle normalCellStyle = workbook.createCellStyle();
                headerCellStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
                normalCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                CellStyle correctCellStyle = workbook.createCellStyle();
                correctCellStyle.setFillPattern(FillPatternType.BIG_SPOTS);
                correctCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());

                CellStyle wrongCellStyle = workbook.createCellStyle();
                wrongCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                wrongCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

                for (int col = 0; col < headers.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(headers[col]);
                    cell.setCellStyle(headerCellStyle);
                }

                // Job Numbers heading looping
                for (int i = 0; i < jobNoList.size(); i++) {
                    Object[] jobObject = jobNoList.get(i);

                    Cell cell = headerRow.createCell(headerRow.getLastCellNum());
                    cell.setCellValue(jobObject[0].toString());
                    cell.setCellStyle(headerCellStyle);
                }

                int rowIdx = 1;

                // Specification & drawing data looping
                for (int i = 0; i < specificationList.size(); i++) {
                    Object[] drawingObject = specificationList.get(i);

                    Row row = sheet.createRow(rowIdx++);
                    try {
                        row.createCell(0).setCellValue(drawingObject[1].toString());
                        row.createCell(1).setCellValue(drawingObject[2].toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exception e");
                    }
                }

                // Job Results data looping
                for (int j = 0; j < actualSizeArray.size(); j++) {
                    JsonArray actualArr = actualSizeArray.get(j).getAsJsonArray();

                    Row updatedRow = sheet.getRow(j + 1);
                    for (JsonElement jsonArray : actualArr) {
                        JsonObject actOb = jsonArray.getAsJsonObject();
                        try {
                            int lidx = updatedRow.getLastCellNum();
                            Cell cell = updatedRow.createCell(lidx);
                            System.out.println("+++++++++++++++++++ lidx" + lidx + " ++++++++ SIZERESULT " + actOb.get("sizeResult").getAsString());
                            if (!actOb.get("sizeResult").getAsString().isEmpty()) {
                                if (actOb.get("sizeResult").getAsBoolean() == true) {
                                    cell.setCellValue(actOb.get("actualSize").getAsString());
                                    System.out.println("+++++++++++++++++++++++++++ TRUE EXECUTED ");
                                    cell.setCellStyle(correctCellStyle);
                                } else if (actOb.get("sizeResult").getAsBoolean() == false) {
                                    cell.setCellValue(actOb.get("actualSize").getAsString());
                                    cell.setCellStyle(wrongCellStyle);
                                }
                            } else {
                                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> EXECUTED ELSE NA ");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Exception e");
                        }
                    }
                }

                // Remark Row
                sheet.addMergedRegion(new CellRangeAddress(11,11,0,1));
                Row remarkRow = sheet.createRow(rowIdx++);
                Cell remarkCell = remarkRow.createCell(0);
                remarkCell.setCellStyle(headerCellStyle);
                remarkCell.setCellValue("REMARK");
                remarkRow.createCell(1).setCellValue("");

                // jobResultArray looping
                for (int i = 0; i < jobResultArray.size(); i++) {
                    JsonObject jsonObject = jobResultArray.get(i).getAsJsonObject();
                    Cell rowCell = remarkRow.createCell(remarkRow.getLastCellNum());
                    if (jsonObject.get("result").getAsBoolean() == true) {
                        rowCell.setCellValue("OK");
                        System.out.println("+++++++++++++++++++++++++++ TRUE EXECUTED ");
                        rowCell.setCellStyle(correctCellStyle);
                    } else if (jsonObject.get("result").getAsBoolean() == false) {
                        rowCell.setCellValue("NOT-OK");
                        rowCell.setCellStyle(wrongCellStyle);
                    }
                }

                // Employee Row
                sheet.addMergedRegion(new CellRangeAddress(10,10,0,1));
                Row empRow = sheet.createRow(rowIdx++);
                Cell cell = empRow.createCell(0);
                cell.setCellStyle(headerCellStyle);
                cell.setCellValue("Employee");
                empRow.createCell(1).setCellValue("");

                // Job Numbers heading looping
                for (int i = 0; i < jobNoArray.size(); i++) {
                    JsonObject jsonObject = jobNoArray.get(i).getAsJsonObject();
                    Cell rowCell = empRow.createCell(empRow.getLastCellNum());
                    rowCell.setCellValue(jsonObject.get("empName").getAsString() + " \n" + jsonObject.get("createdAt").getAsString());
                    CellUtil.setCellStyleProperty(rowCell, CellUtil.WRAP_TEXT, true); // make sure wrap text is set.
                    empRow.setHeightInPoints(30);// set row heigth undefined so it is auto heigth

                    rowCell.setCellStyle(headerCellStyle);
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
            inspectionLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public Object getFinalLineInspectionListWithFilter(Map<String, String> jsonRequest, HttpServletRequest request) {
        JsonObject responseMessage = new JsonObject();
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String jobId = jsonRequest.get("jobId");
            String jobOperationId = jsonRequest.get("jobOperationId");
            String employeeId = jsonRequest.get("employeeId");

            List<Object[]> jobNoList = new ArrayList<>();
            if (employeeId.equalsIgnoreCase(""))
                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndInstituteId(jobId, jobOperationId, fromDate, toDate,users.getInstitute().getId());
            else
                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndEmployeeAndInstituteId(jobId, jobOperationId, employeeId, fromDate, toDate,users.getInstitute().getId());

            System.out.println("jobNoList.size() " + jobNoList.size());
            List<Object[]> specificationList = new ArrayList<>();
            if (employeeId.equalsIgnoreCase(""))
                specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndInstituteId(jobId, jobOperationId, fromDate, toDate,users.getInstitute().getId());
            else
                specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndEmployeeAndInstituteId(jobId, jobOperationId, employeeId, fromDate, toDate,users.getInstitute().getId());

            System.out.println("specificationList.size() " + specificationList.size());
            JsonArray drawingArray = new JsonArray();
            for (int i = 0; i < specificationList.size(); i++) {
                Object[] drawingObject = specificationList.get(i);

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("inspectionId", drawingObject[0].toString());
                jsonObject.addProperty("specification", drawingObject[1].toString());
                jsonObject.addProperty("drawingSize", drawingObject[2].toString());

                drawingArray.add(jsonObject);

            }

            JsonArray actualSizeArray = new JsonArray();
            JsonArray jobNoArray = new JsonArray();
            JsonArray inspectionResultArray = new JsonArray();
            for (int i = 0; i < jobNoList.size(); i++) {
                Object[] jobObject = jobNoList.get(i);

                jobNoArray.add(jobObject[0].toString());

                JsonArray sizeInnerArr = new JsonArray();
                for (int k = 0; k < specificationList.size(); k++) {
                    Object[] drawingObject = specificationList.get(k);

                    System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobObject[0].toString() + " INSP ID " + drawingObject[0].toString());
                    String employeeInspections = null;
                    if (employeeId.equalsIgnoreCase(""))
                        employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetails(jobId, jobOperationId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
                    else
                        employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());

                    System.out.println(" <<<<<<<<<<<<<<<<<<<<<< employeeInspections >>>>>>>>>>>> " + employeeInspections);
                    JsonObject inspObj = new JsonObject();
                    inspObj.addProperty("actualSize", "-");
                    inspObj.addProperty("sizeResult", "");
                    if (employeeInspections != null) {
                        inspObj.addProperty("actualSize", employeeInspections.split(",")[0]);
                        inspObj.addProperty("sizeResult", employeeInspections.split(",")[1]);
                    }
                    sizeInnerArr.add(inspObj);
                }
                actualSizeArray.add(sizeInnerArr);
            }

            responseMessage.add("resultArray", actualSizeArray);
            responseMessage.add("inspectionResultArray", inspectionResultArray);
            responseMessage.add("jobNoArray", jobNoArray);
            responseMessage.add("drawingArray", drawingArray);
            responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception  " + e.getMessage());

            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public InputStream exportExcelFinalEmployeeInspection(Map<String, String> jsonRequest, HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        try {
            String[] headers = {"Job No."};
            String sheetName = "InspectionSheet";

            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");
            String jobId = jsonRequest.get("jobId");
            String jobOperationId = jsonRequest.get("jobOperationId");
            String employeeId = jsonRequest.get("employeeId");

            List<Object[]> jobNoList = new ArrayList<>();
            if (employeeId.equalsIgnoreCase(""))
                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndInstituteId(jobId, jobOperationId, fromDate, toDate, users.getInstitute().getId());
            else
                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndEmployeeAndInstituteId(jobId, jobOperationId, employeeId, fromDate, toDate, users.getInstitute().getId());

            System.out.println("jobNoList.size() " + jobNoList.size());

            if (jobNoList.size() > 0) {
                JsonArray actualSizeArray = new JsonArray();

                List<Object[]> specificationList = new ArrayList<>();
                if (employeeId.equalsIgnoreCase(""))
                    specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndInstituteId(jobId, jobOperationId, fromDate, toDate,users.getInstitute().getId());
                else
                    specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndEmployeeAndInstituteId(jobId, jobOperationId, employeeId, fromDate, toDate,users.getInstitute().getId());

                System.out.println("specificationList.size() " + specificationList.size());
                for (int k = 0; k < jobNoList.size(); k++) {
                    Object[] jobObject = jobNoList.get(k);

                    JsonArray sizeInnerArr = new JsonArray();
                    for (int i = 0; i < specificationList.size(); i++) {
                        Object[] drawingObject = specificationList.get(i);

                        System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobObject[0].toString() + " INSP ID " + drawingObject[0].toString());
                        String employeeInspections = null;
                        if (employeeId.equalsIgnoreCase(""))
                            employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetails(jobId, jobOperationId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
                        else
                            employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());

                        System.out.println(" <<<<<<<<<<<<<<<<<<<<<< employeeInspections >>>>>>>>>>>> " + employeeInspections);
                        JsonObject inspObj = new JsonObject();
                        inspObj.addProperty("actualSize", "");
                        inspObj.addProperty("sizeResult", "");
                        if (employeeInspections != null) {
                            inspObj.addProperty("actualSize", employeeInspections.split(",")[0]);
                            inspObj.addProperty("sizeResult", employeeInspections.split(",")[1]);
                        }
                        sizeInnerArr.add(inspObj);
                    }
                    actualSizeArray.add(sizeInnerArr);
                }

                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet(sheetName);

                    // Header
                    Row headerRow = sheet.createRow(0);

                    Font font = workbook.createFont();
                    font.setBold(true);

                    // Define header cell style
                    CellStyle headerCellStyle = workbook.createCellStyle();
                    headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                    headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerCellStyle.setFont(font);
//                headerCellStyle.setWrapText(true);

                    CellStyle normalCellStyle = workbook.createCellStyle();
                    headerCellStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
                    normalCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    CellStyle correctCellStyle = workbook.createCellStyle();
                    correctCellStyle.setFillPattern(FillPatternType.BIG_SPOTS);
                    correctCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());

                    CellStyle wrongCellStyle = workbook.createCellStyle();
                    wrongCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    wrongCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());

                    for (int col = 0; col < headers.length; col++) {
                        Cell cell = headerRow.createCell(col);
                        cell.setCellValue(headers[col]);
                        cell.setCellStyle(headerCellStyle);
                    }

                    // Job Numbers heading looping
                    for (int i = 0; i < specificationList.size(); i++) {
                        Object[] drawingObject = specificationList.get(i);

                        Cell cell = headerRow.createCell(headerRow.getLastCellNum());
                        cell.setCellValue(drawingObject[1].toString() + " \n " + drawingObject[2].toString());
                        cell.getRow().setHeightInPoints(cell.getSheet().getDefaultRowHeightInPoints() * 2);
                        cell.setCellStyle(headerCellStyle);
                    }

                    int rowIdx = 1;
                    // Specification & drawing data looping
                    for (int i = 0; i < jobNoList.size(); i++) {
                        Object[] jobObject = jobNoList.get(i);

                        Row row = sheet.createRow(rowIdx++);
                        try {
                            row.createCell(0).setCellValue(jobObject[0].toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Exception e");
                        }
                    }

                    // Job Results data looping
                    for (int j = 0; j < actualSizeArray.size(); j++) {
                        JsonArray actualArr = actualSizeArray.get(j).getAsJsonArray();

                        Row updatedRow = sheet.getRow(j + 1);
                        for (JsonElement jsonArray : actualArr) {
                            JsonObject actOb = jsonArray.getAsJsonObject();
                            try {
                                int lidx = updatedRow.getLastCellNum();
                                Cell cell = updatedRow.createCell(lidx);
                                System.out.println("+++++++++++++++++++ lidx" + lidx + " ++++++++ SIZERESULT " + actOb.get("sizeResult").getAsString());
                                if (!actOb.get("sizeResult").getAsString().isEmpty()) {
                                    if (actOb.get("sizeResult").getAsBoolean() == true) {
                                        cell.setCellValue(actOb.get("actualSize").getAsString());
                                        System.out.println("+++++++++++++++++++++++++++ TRUE EXECUTED ");
//                                        cell.setCellStyle(correctCellStyle);
                                    } else if (actOb.get("sizeResult").getAsBoolean() == false) {
                                        cell.setCellValue(actOb.get("actualSize").getAsString());
                                        cell.setCellStyle(wrongCellStyle);
                                    }
                                } else {
                                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> EXECUTED ELSE NA ");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("Exception e");
                            }
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
                    e.printStackTrace();
                    System.out.println("IOException " + e.getMessage());
                    throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
                }
            } else {
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< Data not exists for the input data");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                return new ByteArrayInputStream(out.toByteArray());
            }
        } catch (Exception e) {
            inspectionLogger.error("Failed to load data " + e);
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

//    public InputStream exportExcelFinalEmployeeInspectionOld(Map<String, String> jsonRequest, HttpServletRequest request) {
//        try {
//
//            String[] headers = {"Job No."};
//            String sheetName = "InspectionSheet";
//
//            String fromDate = jsonRequest.get("fromDate");
//            String toDate = jsonRequest.get("toDate");
//            String jobId = jsonRequest.get("jobId");
//            String jobOperationId = jsonRequest.get("jobOperationId");
//            String employeeId = jsonRequest.get("employeeId");
//
//            List<Object[]> jobNoList = new ArrayList<>();
//            if (employeeId.equalsIgnoreCase(""))
//                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetails(jobId, jobOperationId, fromDate, toDate);
//            else
//                jobNoList = employeeInspectionRepository.getJobNosByDateRageAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, fromDate, toDate);
//
//            System.out.println("jobNoList.size() " + jobNoList.size());
//
//            if (jobNoList.size() > 0) {
//                JsonArray actualSizeArray = new JsonArray();
//
//                List<Object[]> specificationList = new ArrayList<>();
//                if (employeeId.equalsIgnoreCase(""))
//                    specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetails(jobId, jobOperationId, fromDate, toDate);
//                else
//                    specificationList = employeeInspectionRepository.getSpecificationByDateRageAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, fromDate, toDate);
//
//                System.out.println("specificationList.size() " + specificationList.size());
//                for (int k = 0; k < jobNoList.size(); k++) {
//                    Object[] jobObject = jobNoList.get(k);
//
//                    JsonArray sizeInnerArr = new JsonArray();
//                    for (int i = 0; i < specificationList.size(); i++) {
//                        Object[] drawingObject = specificationList.get(i);
//
//                        System.out.println("+++++++++++++++++++++++++++ JOB NO " + jobObject[0].toString() + " INSP ID " + drawingObject[0].toString());
//                        String employeeInspections = null;
//                        if (employeeId.equalsIgnoreCase(""))
//                            employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetails(jobId, jobOperationId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
//                        else
//                            employeeInspections = employeeInspectionRepository.getDataByDateRangeAndJobDetailsAndEmployee(jobId, jobOperationId, employeeId, jobObject[0].toString(), fromDate, toDate, drawingObject[0].toString());
//
//                        System.out.println(" <<<<<<<<<<<<<<<<<<<<<< employeeInspections >>>>>>>>>>>> " + employeeInspections);
//                        JsonObject inspObj = new JsonObject();
//                        inspObj.addProperty("actualSize", "");
//                        inspObj.addProperty("sizeResult", "");
//                        if (employeeInspections != null) {
//                            inspObj.addProperty("actualSize", employeeInspections.split(",")[0]);
//                            inspObj.addProperty("sizeResult", employeeInspections.split(",")[1]);
//                        }
//                        sizeInnerArr.add(inspObj);
//                    }
//                    actualSizeArray.add(sizeInnerArr);
//                }
//
//                try (Workbook workbook = new XSSFWorkbook()) {
//                    Sheet sheet = workbook.createSheet(sheetName);
//
//                    // Header
//                    Row headerRow = sheet.createRow(0);
//
//                    Font font = workbook.createFont();
//                    font.setBold(true);
//
//                    // Define header cell style
//                    CellStyle headerCellStyle = workbook.createCellStyle();
//                    headerCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
//                    headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//                    headerCellStyle.setFont(font);
////                headerCellStyle.setWrapText(true);
//
//                    CellStyle normalCellStyle = workbook.createCellStyle();
//                    headerCellStyle.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
//                    normalCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//
//                    CellStyle correctCellStyle = workbook.createCellStyle();
//                    correctCellStyle.setFillPattern(FillPatternType.BIG_SPOTS);
//                    correctCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
//
//                    CellStyle wrongCellStyle = workbook.createCellStyle();
//                    wrongCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
//                    wrongCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
//
//                    for (int col = 0; col < headers.length; col++) {
//                        Cell cell = headerRow.createCell(col);
//                        cell.setCellValue(headers[col]);
//                        cell.setCellStyle(headerCellStyle);
//                    }
//
//                    // Job Numbers heading looping
//                    for (int i = 0; i < specificationList.size(); i++) {
//                        Object[] drawingObject = specificationList.get(i);
//
//                        Cell cell = headerRow.createCell(headerRow.getLastCellNum());
//                        cell.setCellValue(drawingObject[1].toString() + " \n " + drawingObject[2].toString());
//                        cell.getRow().setHeightInPoints(cell.getSheet().getDefaultRowHeightInPoints() * 2);
//                        cell.setCellStyle(headerCellStyle);
//                    }
//
//                    int rowIdx = 1;
//                    // Specification & drawing data looping
//                    for (int i = 0; i < jobNoList.size(); i++) {
//                        Object[] jobObject = jobNoList.get(i);
//
//                        Row row = sheet.createRow(rowIdx++);
//                        try {
//                            row.createCell(0).setCellValue(jobObject[0].toString());
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            System.out.println("Exception e");
//                        }
//                    }
//
//                    // Job Results data looping
//                    for (int j = 0; j < actualSizeArray.size(); j++) {
//                        JsonArray actualArr = actualSizeArray.get(j).getAsJsonArray();
//
//                        Row updatedRow = sheet.getRow(j + 1);
//                        for (JsonElement jsonArray : actualArr) {
//                            JsonObject actOb = jsonArray.getAsJsonObject();
//                            try {
//                                int lidx = updatedRow.getLastCellNum();
//                                Cell cell = updatedRow.createCell(lidx);
//                                System.out.println("+++++++++++++++++++ lidx" + lidx + " ++++++++ SIZERESULT " + actOb.get("sizeResult").getAsString());
//                                if (!actOb.get("sizeResult").getAsString().isEmpty()) {
//                                    if (actOb.get("sizeResult").getAsBoolean() == true) {
//                                        cell.setCellValue(actOb.get("actualSize").getAsString());
//                                        System.out.println("+++++++++++++++++++++++++++ TRUE EXECUTED ");
//                                        cell.setCellStyle(correctCellStyle);
//                                    } else if (actOb.get("sizeResult").getAsBoolean() == false) {
//                                        cell.setCellValue(actOb.get("actualSize").getAsString());
//                                        cell.setCellStyle(wrongCellStyle);
//                                    }
//                                } else {
//                                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>> EXECUTED ELSE NA ");
//                                }
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                System.out.println("Exception e");
//                            }
//                        }
//                    }
//
//                    ByteArrayOutputStream out = new ByteArrayOutputStream();
//                    workbook.write(out);
//
//                    byte[] b = new ByteArrayInputStream(out.toByteArray()).readAllBytes();
//                    if (b.length > 0) {
//                        String s = new String(b);
//                        System.out.println("data ------> " + s);
//                    } else {
//                        System.out.println("Empty");
//                    }
//                    return new ByteArrayInputStream(out.toByteArray());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    System.out.println("IOException " + e.getMessage());
//                    throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
//                }
//            } else {
//                System.out.println("<<<<<<<<<<<<<<<<<<<<<<<< Data not exists for the input data");
//                ByteArrayOutputStream out = new ByteArrayOutputStream();
//                return new ByteArrayInputStream(out.toByteArray());
//            }
//        } catch (Exception e) {
//            inspectionLogger.error("Failed to load data " + e);
//            e.printStackTrace();
//            System.out.println("Exception " + e.getMessage());
//            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
//        }
//    }

    public Object getMachineListFromInspectionData(Map<String, String> jsonRequest, HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String employeeId = jsonRequest.get("employeeId");

            JsonArray machineArray = new JsonArray();
            List<Object[]> employeeInspections = new ArrayList<>();

            String query = "SELECT DISTINCT(machine_id), machine_tbl.name, machine_tbl.number  FROM employee_inspection_tbl LEFT JOIN machine_tbl ON" +
                    " employee_inspection_tbl.machine_id=machine_tbl.id WHERE inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' AND machine_tbl.institute_id="+users.getInstitute().getId();

            if (!employeeId.equalsIgnoreCase(""))
                query = query + " AND employee_id=" + employeeId + " ";

            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            employeeInspections = q.getResultList();
            System.out.println("rows " + employeeInspections.size());

            for (int i = 0; i < employeeInspections.size(); i++) {
                JsonObject machineObj = new JsonObject();
                machineObj.addProperty("machineId", employeeInspections.get(i)[0].toString());
                machineObj.addProperty("machineName", employeeInspections.get(i)[1].toString());
                machineObj.addProperty("machineNo", employeeInspections.get(i)[2].toString());

                machineArray.add(machineObj);
            }

            response.add("response", machineArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getJobListFromInspectionData(Map<String, String> jsonRequest, HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");

            JsonArray jobArray = new JsonArray();
            List<Object[]> employeeInspections = new ArrayList<>();

            String query = "SELECT DISTINCT(job_id), job_tbl.job_name FROM employee_inspection_tbl LEFT JOIN job_tbl ON" +
                    " employee_inspection_tbl.job_id=job_tbl.id WHERE machine_id=" + machineId + " AND inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' AND job_tbl.institute_id="+users.getInstitute().getId();

            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            employeeInspections = q.getResultList();
            System.out.println("Limit total rows " + employeeInspections.size());

            for (int i = 0; i < employeeInspections.size(); i++) {
                JsonObject jobObj = new JsonObject();
                jobObj.addProperty("jobId", employeeInspections.get(i)[0].toString());
                jobObj.addProperty("jobName", employeeInspections.get(i)[1].toString());

                jobArray.add(jobObj);
            }

            response.add("response", jobArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getJobOperationListFromInspectionData(Map<String, String> jsonRequest, HttpServletRequest
            request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");
            String jobId = jsonRequest.get("jobId");

            JsonArray jobOperationArray = new JsonArray();
            List<Object[]> employeeInspections = new ArrayList<>();

            String query = "SELECT DISTINCT(job_operation_id), job_operation_tbl.operation_name FROM employee_inspection_tbl" +
                    " LEFT JOIN job_operation_tbl ON employee_inspection_tbl.job_operation_id=job_operation_tbl.id" +
                    " WHERE employee_inspection_tbl.job_id=" + jobId + " AND inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' AND job_operation_tbl.institute_id="+users.getInstitute().getId();

            if (!machineId.equalsIgnoreCase(""))
                query = query + " AND machine_id=" + machineId + " ";

            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            employeeInspections = q.getResultList();
            System.out.println("Limit total rows " + employeeInspections.size());
            for (int i = 0; i < employeeInspections.size(); i++) {
                JsonObject jobOperationObj = new JsonObject();
                jobOperationObj.addProperty("jobOperationId", employeeInspections.get(i)[0].toString());
                jobOperationObj.addProperty("jobOperationName", employeeInspections.get(i)[1].toString());

                jobOperationArray.add(jobOperationObj);
            }
            response.add("response", jobOperationArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }

    public Object getEmployeeListFromInspectionData(Map<String, String> jsonRequest, HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject response = new JsonObject();
        try {
            String fromDate = jsonRequest.get("fromDate");
            String toDate = jsonRequest.get("toDate");
            String machineId = jsonRequest.get("machineId");

            List<Object[]> employeeInspections = new ArrayList<>();
            JsonArray empArray = new JsonArray();
            String query = "SELECT DISTINCT(employee_id), employee_tbl.first_name FROM employee_inspection_tbl LEFT JOIN" +
                    " employee_tbl ON employee_inspection_tbl.employee_id=employee_tbl.id WHERE inspection_date BETWEEN '" + fromDate + "' AND '" + toDate + "' AND employee_tbl.institute_id="+users.getInstitute().getId();

            if (!machineId.equalsIgnoreCase(""))
                query = query + " AND machine_id=" + machineId + " ";

            System.out.println("query " + query);
            Query q = entityManager.createNativeQuery(query);
            employeeInspections = q.getResultList();
            System.out.println("Limit total rows " + employeeInspections.size());
            for (int i = 0; i < employeeInspections.size(); i++) {
                Employee employee = employeeRepository.findById(Long.parseLong(employeeInspections.get(i)[0].toString())).get();

                JsonObject empObj = new JsonObject();
                empObj.addProperty("employeeId", employeeInspections.get(i)[0].toString());
                empObj.addProperty("employeeName", utility.getEmployeeName(employee));

                empArray.add(empObj);
            }
            response.add("response", empArray);
            response.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            response.addProperty("message", "Failed to load data");
            response.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return response;
    }
}
