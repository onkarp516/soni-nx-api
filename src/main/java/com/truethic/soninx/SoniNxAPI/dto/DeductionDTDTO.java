package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class DeductionDTDTO {
    private Long id;
    private String name;
    private Double amount;
    private Boolean deductionStatus;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Double percentage;
    private Long payheadParentId;
    private String payheadParentName;
}
