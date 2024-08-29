package com.truethic.soninx.SoniNxAPI.service;

import com.truethic.soninx.SoniNxAPI.repository.DowntimeRepository;
import com.truethic.soninx.SoniNxAPI.repository.TaskMasterRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.dto.DailyWorkDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkService {
    @Autowired
    private TaskMasterRepository taskRepository;
    @Autowired
    private DowntimeRepository downtimeRepository;

    public Object getEmployeesWork() {
        ResponseMessage responseMessage = new ResponseMessage();
        List<DailyWorkDTO> workDTOList = new ArrayList<>();

//        List<Task> tasks = taskRepository.findByStatus(true);
//        if(tasks.size() > 0){
//            for (Task task: tasks){
//                DailyWorkDTO dailyWorkDTO = new DailyWorkDTO();
//                dailyWorkDTO.setTaskId(task.getId());
//                dailyWorkDTO.setEmployeeId(task.getEmployee().getId());
//                dailyWorkDTO.setEmployeeName(task.getEmployee().getFullName());
//                dailyWorkDTO.setMachineId(task.getMachine().getId());
//                dailyWorkDTO.setMachineName(task.getMachine().getName());
//                dailyWorkDTO.setJobId(task.getJob().getId());
//                dailyWorkDTO.setJobName(task.getJob().getJobName());
//                dailyWorkDTO.setJobOperationId(task.getJobOperation().getId());
//                dailyWorkDTO.setJobOperationName(task.getJobOperation().getOperationName());
//                dailyWorkDTO.setCycleTime(task.getCycleTime());
//                dailyWorkDTO.setPcsRate(task.getPcsRate());
//                dailyWorkDTO.setAveragePerShift(task.getAveragePerShift());
//                dailyWorkDTO.setPointPerJob(task.getPointPerJob());
//                dailyWorkDTO.setMachineStartCount(task.getMachineEndCount());
//                dailyWorkDTO.setTotalCount(task.getTotalCount());
//                dailyWorkDTO.setRemark(task.getRemark());
//                dailyWorkDTO.setTaskCreatedAt(task.getCreatedAt());
//
//                workDTOList.add(dailyWorkDTO);
//            }
//        }
//
//        List<Downtime> downtimes = downtimeRepository.findByStatus(true);
//        if(downtimes.size() > 0){
//            for(Downtime downtime: downtimes){
//                DailyWorkDTO dailyWorkDTO = new DailyWorkDTO();
//                dailyWorkDTO.setDowntimeId(downtime.getId());
//                dailyWorkDTO.setWorkBreakId(downtime.getWorkBreak().getId());
//                dailyWorkDTO.setWorkBreakName(downtime.getWorkBreak().getBreakName());
//                dailyWorkDTO.setDowntimeDate(downtime.getDowntimeDate());
//                dailyWorkDTO.setStartTime(downtime.getStartTime());
//                dailyWorkDTO.setEndTime(downtime.getEndTime());
//                dailyWorkDTO.setTotalTime(downtime.getTotalTime());
//                dailyWorkDTO.setNote(downtime.getNote());
//                dailyWorkDTO.setDowntimeCreatedAt(downtime.getCreatedAt());
//
//                workDTOList.add(dailyWorkDTO);
//            }
//        }

        responseMessage.setResponse(workDTOList);
        responseMessage.setResponseStatus(HttpStatus.OK.value());
        return responseMessage;
    }
}
