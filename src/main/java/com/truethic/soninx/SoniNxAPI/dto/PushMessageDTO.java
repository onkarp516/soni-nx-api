package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class PushMessageDTO {
    private Long id;
    private String fromDate;
    private String toDate;
    private String message;
    private Boolean status;
    private String createdAt;
    private Long createdBy;
    private String updatedAt;
    private Long updatedBy;
}
