package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShiftAssignDTO {
    private Long id;
    private String fromDate;
    private String toDate;
    private String employeeName;
    private String shiftName;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;

}
