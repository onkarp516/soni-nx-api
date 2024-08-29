package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class AdvancePaymentDTO {
    private Long id;
    private String employeeName;
    private Integer requestAmount;
    private String reason;
    private String dateOfRequest;
    private String approvedBy;
    private String remark;
    private String paymentStatus;
    private String paymentDate;
    private Integer paidAmount;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
