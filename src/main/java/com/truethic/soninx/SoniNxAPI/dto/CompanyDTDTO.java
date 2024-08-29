package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class CompanyDTDTO {
    private Long id;
    private String companyName;
    private String description;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
