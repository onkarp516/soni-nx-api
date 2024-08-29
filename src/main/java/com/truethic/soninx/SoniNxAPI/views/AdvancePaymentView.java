package com.truethic.soninx.SoniNxAPI.views;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "advance_payment_view")
public class AdvancePaymentView implements Serializable {
    @Id
    private Long id;
    private Long employeeId;
    private String fullName;
    private Integer requestAmount;
    private String reason;
    private LocalDate dateOfRequest;
    private String approvedBy;
    private String remark;
    private String paymentStatus;
    private LocalDate paymentDate;
    private Integer paidAmount;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    private Long instituteId;
}
