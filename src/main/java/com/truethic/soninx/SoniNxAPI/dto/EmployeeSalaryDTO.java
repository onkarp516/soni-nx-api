package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class EmployeeSalaryDTO {
    private Long id;
    private Long employeeId;
    private String effectiveDate;
    private Double salary;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
