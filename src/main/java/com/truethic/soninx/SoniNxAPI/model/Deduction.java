package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deduction_tbl")
public class Deduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double amount;
    private Boolean status;
    private Long createdBy;
    private Boolean deductionStatus;
    private Double percentage;
    private String deductionSlug;
    @OneToOne(optional = true)
    @JsonIgnoreProperties(value = {"deduction", "hibernateLazyInitializer"})
    @JoinColumn(name = "percentage_of", nullable = true)
    private Payhead percentageOf;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
