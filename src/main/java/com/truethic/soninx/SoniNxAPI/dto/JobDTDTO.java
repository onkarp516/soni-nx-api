package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class JobDTDTO {
    private Long id;
    private String jobName;
    private String jobImagePath;
    private String jobImageKey;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
