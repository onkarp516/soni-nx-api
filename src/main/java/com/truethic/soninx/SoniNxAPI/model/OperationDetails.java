package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_details_tbl")
public class OperationDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Double cycleTime;
    private Double salary;
    private Double pcsRate;
    private Double averagePerShift;
    private Double pointPerJob;
    private Integer operationBreakInMin;
    private String operationDiameterType;

    private LocalDate effectiveDate;
    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_details", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_operation_id")
    private JobOperation jobOperation;

    private Long createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
