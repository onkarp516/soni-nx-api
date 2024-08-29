package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_master_history_tbl")
public class TaskMasterHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private Long employeeId;
    private Long attendanceId;
    private Long taskMasterId;
    private Long updatingUserId;

    private LocalDate taskDate;
    /*private LocalTime startTime;
    private LocalTime endTime;*/
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalTime; // in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Integer taskType; // 1=>Task, 2=>Downtime, 3=>Setting time, 4=> task without machine
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private String remark;
    private String taskStatus; // in-progress, complete
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;

    /* Downtime start */
    private Long workBreakId;

    /* Downtime end */

    /* Task start */
    private Long machineId;
    private Long jobId;
    private Long jobOperationId;

    private Long machineStartCount;
    private Long machineEndCount;
    private Long totalCount;

    /* Job Operation Related Data*/
    private String employeWagesType;
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
    private Double workPoint;
    private Double wagesPoint; // start of task
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

    private String adminRemark;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
    /* Task end */
}
