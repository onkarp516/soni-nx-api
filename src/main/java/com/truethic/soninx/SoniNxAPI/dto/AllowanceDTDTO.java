package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class AllowanceDTDTO {
    private Long id;
    private String name;
    private Double amount;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
