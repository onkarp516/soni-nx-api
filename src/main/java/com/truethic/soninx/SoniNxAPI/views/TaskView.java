package com.truethic.soninx.SoniNxAPI.views;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "task_view")
public class TaskView {
    @Id
    private Long id;
    private Long employeeId;
    private Long attendanceId;
    private String employeeName;
    private LocalDate taskDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalTime workingTime; // in HH:MM:SS
    private Double totalTime; // in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Integer taskType; // 1=>Task, 2=>Downtime
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
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
    private String employeeWagesType;
    private Double cycleTime;
    private Double pcsRate;
    private Double averagePerShift;
    private Double perJobPoint;

    /* Task Process Data*/
    private Double jobsPerHour;
    private Double workingHour;
    private Double prodWorkingHour;
    private Double settingTimeInMin;
    private Double settingTimeInHour;
    private Double WorkingHourWithSetting;
    private Double prodPoint;
    private Double settingTimePoint;
    private Double totalPoint;

    private Double requiredProduction;
    private Double actualProduction;
    private Double shortProduction;
    private Double percentageOfTask;

    /*Salary Related Data*/
//    private Double wagesPoint; // start of task
    private Double wagesPerPoint;
    private Double wagesPerDay;
    private Double wagesPerHour;
    private Double wagesPerMinute;

    private Double wagesPointBasis;
    private Double wagesHourBasis;
    private Double wagesMinutesBasis;
    private Double wagesPcsBasis;
    private Double breakWages;

    private Double reworkQty;
    private Double machineRejectQty;
    private Double doubtfulQty;
    private Double unMachinedQty;
    private Double okQty;
    private String adminRemark;
    private String correctiveAction;
    private String preventiveAction;
    private Long instituteId;
    private String endRemark;
    /* Task end */
}
