package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class JobOperationDTDTO {
    private Long jobOperationId;
    private String operationName;
    private String operationNo;
    private Double cycleTime;
    private Double pcsRate;
    private Double averagePerShift;
    private Double pointPerJob;
    private String operationDiameterType;
    private String jobName;
    private String operationImagePath;
    private String operationImageKey;
    private String procedureSheet;
    private String createdAt;
    private Boolean status;


}
