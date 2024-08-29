package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class TaskDTO {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String taskDate;
    private String startTime;
    private String endTime;
    private Double totalTime; // in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Integer taskType; // 1=>Task, 2=>Downtime
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private String remark;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
    private Long workBreakId;
    private String breakName;
    private Long machineId;
    private String machineName;
    private String machineNumber;
    private Long jobId;
    private String jobName;
    private Long jobOperationId;
    private String operationName;

    private Long machineStartCount;
    private Long machineEndCount;
    private Long totalCount;

    /* Job Operation Related Data*/
    private Double cycleTime;
    private Double pcsRate;
    private Double averagePerShift;
    private Double pointPerJob;

    /* Task Process Data*/
    private Double jobsPerHour;
    private Double workHours;

    private Double requiredProduction;
    private Double actualProduction;
    private Double shortProduction;
    private Double percentageOfTask;

    /*Salary Related Data*/
    private Double wagesPoint; // start of task
    private Double workPoint;
    private Double wagesPerDay;
    private Double wagesPerHour;
    private Double wagesPerMinute;

    private Double wagesPointBasis;
    private Double wagesHourBasis;
    private Double wagesMinutesBasis;
    private Double wagesPcsBasis;

    private Double reworkQty;
    private Double machineRejectQty;
    private Double doubtfulQty;
    private Double unMachinedQty;
    private Double okQty;

    private String correctiveAction;
    private String preventiveAction;
    /* Task end */
}
