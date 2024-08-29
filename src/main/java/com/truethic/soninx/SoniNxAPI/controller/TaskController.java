package com.truethic.soninx.SoniNxAPI.controller;

import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class TaskController {
    @Autowired
    private TaskService taskService;

    @PostMapping(path = "/mobile/startTask")
    public Object startTask(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        JsonObject result = taskService.startTask(requestParam, request);
        return result.toString();
    }

    @PostMapping(path = "/mobile/endTask")
    public Object endTask(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        return taskService.endTask(requestParam, request);
    }

    @PostMapping(path = "/mobile/getReqProductionCount")
    public Object getReqProductionCount(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        JsonObject result = taskService.getReqProductionCount(requestParam, request);
        return result.toString();
    }

    @PostMapping(path = "/mobile/fetchEmployeeTasks")
    public Object fetchEmployeeTasks(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject result = taskService.fetchEmployeeTasks(requestParam, request);
        return result.toString();
    }

    @PostMapping(path = "/mobile/fetchEmployeeTodaysTaskList")
    public Object fetchEmployeeTodaysTaskList(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject result = taskService.fetchEmployeeTodaysTaskList(requestParam, request);
        return result.toString();
    }

    @PostMapping(path = "/mobile/fetchEmployeeTasksDetail")
    public Object fetchEmployeeTasksDetail(@RequestBody Map<String, String> jsonRequest) {
        JsonObject result = taskService.fetchEmployeeTasksDetail(jsonRequest);
        return result.toString();
    }

    @PostMapping(path = "/mobile/startBreakInTask")
    public Object startBreakInTask(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        return taskService.startBreakInTask(requestParam, request);
    }

    @PostMapping(path = "/mobile/endBreakInTask")
    public Object endBreakInTask(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        JsonObject jsonObject = taskService.endBreakInTask(requestParam, request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/mobile/fetchEmployeeSettingTimes")
    public Object fetchEmployeeSettingTimes(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        JsonObject result = taskService.fetchEmployeeSettingTimes(requestParam, request);
        return result.toString();
    }

    /* Back Office urls start */

    /* Get employee tasks by employee id */
    @PostMapping(path = "/getEmployeeTodayTasks")
    public Object getEmployeeTodayTasks(@RequestBody Map<String, String> request) {
        JsonObject jsonObject = taskService.getEmployeeTodayTasks(request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/getEmployeeOperationView")
    public Object getEmployeeOperationView(@RequestBody Map<String, String> request) {
        return taskService.getEmployeeOperationView(request);
    }

    @PostMapping(path = "/getTaskDetailsForUpdate")
    public Object getTaskDetailsForUpdate(@RequestBody Map<String, String> request) {
        JsonObject object = taskService.getTaskDetailsForUpdate(request);
        return object.toString();
    }

    @PostMapping(path = "/updateTaskDetails")
    public Object updateTaskDetails(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return taskService.updateTaskDetails(jsonRequest, request);
    }
    /* Back Office urls end */


    @PostMapping(path = "/updateTaskData")
    public Object updateTaskData(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return taskService.updateTaskData(jsonRequest, request);
    }

    @PostMapping(path = "/bo/startTask")
    public Object boStartTask(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        System.out.println("Request " + requestParam);
        return taskService.boStartTask(requestParam, request);
    }

    @PostMapping(path = "/bo/deleteTask")
    public Object deleteTask(HttpServletRequest request) {
        return taskService.deleteTask(request).toString();
    }

    @PostMapping(path = "/bo/getRejectionReports")
    public Object getRejectionReports(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return taskService.getRejectionReports(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/getEmployeeTasksByAttendanceId")
    public Object getEmployeeTasksByAttendanceId(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return taskService.getEmployeeTasksByAttendanceId(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/getTasksByTaskId")
    public Object getTasksByTaskId(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return taskService.getTasksByTaskId(jsonRequest, request).toString();
    }

    @PostMapping(path = "/mobile/updateTaskDataBySupervisor")
    public Object updateTaskDataBySupervisor(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request){
        return taskService.updateTaskDataBySupervisor(jsonRequest, request).toString();
    }

    /*PRACTICE URLS*/
    @GetMapping(path = "/getLunchTime")
    public void getLunchTime(HttpServletRequest request) {
        taskService.getLunchTime(request);
    }

    /*PRACTICE URLS*/

}
