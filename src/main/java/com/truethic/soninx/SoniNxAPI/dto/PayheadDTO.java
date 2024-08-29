package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class PayheadDTO {
    private Long id;
    private String name;
    private Double percentage;
    private Double amount;
    private Long payheadParentId;
    private String payheadParentName;
    private Boolean isAdminRecord;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean payheadStatus;
}
