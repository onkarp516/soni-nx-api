package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class DailyWorkDTO {
    private Long taskId;
    private Long employeeId;
    private String employeeName;
    private Long machineId;
    private String machineName;
    private Long jobId;
    private String jobName;
    private Long jobOperationId;
    private String jobOperationName;
    private Double cycleTime;
    private Double pcsRate;
    private Double averagePerShift;
    private Double pointPerJob;
    private Long machineStartCount;
    private Long machineEndCount;
    private Long totalCount;
    private String remark;
    private LocalDateTime taskCreatedAt;

    private Long downtimeId;
    private Long workBreakId;
    private String workBreakName;
    private LocalDate downtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double totalTime; // in minutes
    private String note;
    private LocalDateTime downtimeCreatedAt;
}
