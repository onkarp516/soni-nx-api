package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "salary_slip_payheads_tbl")
public class SalarySlipPayheads {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "employee_payroll_id")
    private EmployeePayroll employeePayroll;

    private String payheadType; // incentive, deduction, allowance, advance
    private String payheadName;
    private Double payheadAmount;
    private Boolean status;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
