package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class EmployeeLeaveDTO {
    private Long id;
    private String employeeName;
    private String leaveName;
    private String fromDate;
    private String toDate;
    private Integer totalDays;
    private String reason;
    private String leaveStatus;
    private String leaveApprovedBy;
    private String leaveRemark;
    private Boolean status;
    private Long createdBy;
    private Long updatedBy;
    private String createdAt;
    private String updatedAt;
}
