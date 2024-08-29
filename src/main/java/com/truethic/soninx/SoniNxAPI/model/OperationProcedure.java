package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "operation_procedure_tbl")
public class OperationProcedure {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_procedure", "hibernateLazyInitializer"})
    @JoinColumn(name = "job_operation_id", nullable = false)
    private JobOperation jobOperation;

    private Long prevJobOperation;
    private Long nextJobOperation;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_procedure", "hibernateLazyInitializer"})
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_procedure", "hibernateLazyInitializer"})
    @JoinColumn(name = "operation_parameters_id", nullable = false)
    private OperationParameter operationParameter;

    @ManyToOne
    @JsonIgnoreProperties(value = {"operation_procedure", "hibernateLazyInitializer"})
    @JoinColumn(name = "tool_mgmt_id", nullable = false)
    private ToolManagement toolManagement;

    private Long custDrgNo;
    private String partName;
    private Long partNumber;
    private Long revNo;
    private LocalDate changeLevelDate;
    private String operationProcedure;
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
