package com.truethic.soninx.SoniNxAPI.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_salary_tbl")
public class EmployeeSalary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
/*
    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;*/

    private Long employeeId;
    private LocalDate effectiveDate;
    private Double salary;

    private Long createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
}
