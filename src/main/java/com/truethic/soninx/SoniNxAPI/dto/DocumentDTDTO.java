package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class DocumentDTDTO {
    private Long id;
    private String name;
    private Boolean isRequired;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
