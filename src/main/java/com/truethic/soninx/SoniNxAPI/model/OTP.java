package com.truethic.soninx.SoniNxAPI.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "otp_tbl")
public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private Long mobileNo;
    private String otp;
    private Boolean status;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
