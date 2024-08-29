package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "employee_payroll_tbl")
public class EmployeePayroll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"attendance", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private String designation;
    private String wagesType;
    private String yearMonth;

    private Double perDaySalary;
    private Double perHourSalary;
    private Double noDaysPresent;
    private Double totalDaysInMonth;
    private Double totalHoursInMonth;
    private Double netSalary;
    private Double netSalaryInHours;
    private Double netSalaryInDays;

    private Double basicPer;
    private Double basic;
    private Double specialAllowance;
    private Double pfPer;
    private Double pf;
    private Double esiPer;
    private Double esi;
    private Double pfTax;
    private Double allowanceAmount;
    private Double deductionAmount;
    private Double totalDeduction;
    private Double payableAmount;
    private Double advance;
    private Double incentive;
    private Double netPayableAmount;

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    private  Double absentDaysSalary;
    private  Double totalDaysOfEmployee;
    private  Double presentDays;
    private  Double leaveDays;
    private  Double absentDays;
    private  Double halfDays;
    private  Double extraDays;
    private  Double extraHalfDays;
    private  Double extraDaysSalary;
    private  Double halfDaysSalary;
    private  Double extraHalfDaysSalary;
    private  Long lateCount = 0L;
    private  Double daysToBeDeducted = 0.0;
    private Double hoursToBeDeducted = 0.0;
    private  Boolean isDayDeduction;
    private String deductionType;
    private  double latePunchDeductionAmt = 0.0;
    private int totalDays;
    private Double monthlyPay;
    private int daysInMonth;
    private Double grosstotal;

    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
