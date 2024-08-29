package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class WorkBreakDTDTO {
    private Long id;
    private String breakName;
    private Boolean isBreakPaid;
    private Boolean status;

    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
