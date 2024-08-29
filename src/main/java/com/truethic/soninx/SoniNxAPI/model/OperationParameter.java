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
@Table(name = "operation_parameter_tbl")
public class OperationParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String specification;
    private String firstParameter;
    private String secondParameter;
    /*private String instrumentUsed;
    private String checkingFrequency;
    private String controlMethod;*/


    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "instrument_id", nullable = true)
    private Instrument instrument;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "checking_frequency_id", nullable = true)
    private CheckingFrequency checkingFrequency;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "control_method_id", nullable = true)
    private ControlMethod controlMethod;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "action_id", nullable = true)
    private Action action;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_parameter", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_operation_id", nullable = false)
    private JobOperation jobOperation;

    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OperationProcedure> operationProcedures;

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
