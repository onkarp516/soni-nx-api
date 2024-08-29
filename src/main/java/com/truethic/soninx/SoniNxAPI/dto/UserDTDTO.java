package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class UserDTDTO {
    private Long id;
    private String username;
    private String password;
    private String userRole;
    private String permissions;
    private String roleName;
    private String plain_password;
    private Boolean isSuperadmin;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
