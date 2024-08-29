package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolManagementDTO {
    private Long toolManagementId;
    private String block;
    private String offsetNo;
    private String toolHolders;
    private String inserts;
    private Long actionId;
    private String actionName;
    private String frequency;
    private String usedFor;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
}
