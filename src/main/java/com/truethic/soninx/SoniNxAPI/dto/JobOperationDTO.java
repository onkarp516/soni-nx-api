package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class JobOperationDTO {
    private Long jobOperationId;
    private String operationName;
    private String operationNo;
    private Double cycleTime;
    private Double salary;
    private Double pcsRate;
    private LocalDate effectiveDate;
    private Double averagePerShift;
    private Double pointPerJob;
    private String operationDiameterType;
    private Integer operationBreakInMin;
    private Long jobId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    private String operationImagePath;
    private String operationImageKey;
    private String procedureSheet;
}
