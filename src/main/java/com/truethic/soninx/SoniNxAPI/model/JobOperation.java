package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name = "job_operation_tbl")
public class JobOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String operationName;
    private String operationNo;
    private Double cycleTime;
    private Double salary;
    private Double pcsRate;
    private Double averagePerShift;
    private Double pointPerJob;
    private Integer operationBreakInMin;
    private String operationDiameterType;
    private String operationImagePath;
    private String operationImageKey;
    private String procedureSheet;

    @ManyToOne
    @JsonIgnoreProperties(value = {"job_operation", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OperationParameter> operationParameters;

    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OperationProcedure> operationProcedures;

    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OperationDetails> operationDetails;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeInspection> employeeInspections;


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
