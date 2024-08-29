package com.truethic.soninx.SoniNxAPI.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "tbl_appVersion")
public class AppVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer versionCode;
    private String versionName;
}
