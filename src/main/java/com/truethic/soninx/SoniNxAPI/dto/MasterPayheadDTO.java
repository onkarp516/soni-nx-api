package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class MasterPayheadDTO {
    private Long Id;
    private String name;
    private Double employerPer;
    private Double employeePer;
    private Long companyId;
    private String companyName;
    private String payheadId;
    private String payheadName;

    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
