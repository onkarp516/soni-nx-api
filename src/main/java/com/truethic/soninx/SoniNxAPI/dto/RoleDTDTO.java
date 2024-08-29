package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class RoleDTDTO {
    private Long id;
    private String roleName;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
