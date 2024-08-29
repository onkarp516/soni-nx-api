package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_inspection_tbl")
public class EmployeeInspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long inspectionId;
    private String jobNo;
    private String drawingSize;
    private String actualSize;
    private Boolean result;
    private String remark;
    private LocalDate inspectionDate;

    private String specification;
    private String firstParameter;
    private String secondParameter;
    private String instrumentUsed;
    private String checkingFrequency;
    private String controlMethod;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_operation_id", nullable = false)
    private JobOperation jobOperation;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "task_id")
    private TaskMaster taskMaster;

    @OneToOne(optional = true)
    @JsonIgnoreProperties(value = {"employee_inspection", "hibernateLazyInitializer"})
    @JoinColumn(name = "supervisor_task_master_id", nullable = true)
    private TaskMaster supervisorTaskMaster;

    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
