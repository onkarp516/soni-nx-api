package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class SiteDTDTO {
    private Long id;
    private String siteName;
    private String siteHindiName;
    private String siteCode;
    private Double siteLat;
    private Double siteLong;
    private Double siteRadius;
    private Boolean status;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
}
