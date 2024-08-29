package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class DesignationDTDTO {
    private Long id;
    private String name;
    private String code;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
