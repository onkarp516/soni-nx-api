package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Table(name = "master_payhead_tbl")
@Entity
public class MasterPayhead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Double employerPer;
    private Double employeePer;
    private Long companyId;

    @ManyToOne
    @JsonIgnoreProperties(value = {"master_payhead", "hibernateLazyInitializer"})
    @JoinColumn(name = "payhead_id", nullable = false)
    private Payhead payhead;

    private Boolean status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
