package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class OperationParameterDTO {
    private Long operationParameterId;
    private String specification;
    private String firstParameter;
    private String secondParameter;
    private Long instrumentUsedId;
    private String instrumentUsed;
    private Long checkingFrequencyId;
    private String checkingFrequency;
    private Long controlMethodId;
    private String controlMethod;
    private Long actionId;
    private String actionName;
    private Long jobId;
    private String jobName;
    private Long jobOperationId;
    private String jobOperationName;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
