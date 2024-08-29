package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class LeaveTypeDTO {
    private Long id;
    private String name;
    private Boolean isPaid;
    private Long leavesAllowed;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
