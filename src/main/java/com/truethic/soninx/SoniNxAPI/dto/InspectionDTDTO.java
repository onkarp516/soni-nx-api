package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class InspectionDTDTO {

    private Long id;
    private String drawingSize;
    private String jobName;
    private String operationName;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
