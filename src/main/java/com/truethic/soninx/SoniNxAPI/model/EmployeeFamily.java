package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_family_tbl")
public class EmployeeFamily {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String age;
    private String relation;
    private String education;
    private String business;
    private String incomePerMonth;

//    @ManyToOne(fetch = FetchType.LAZY,
//            cascade = {CascadeType.ALL})
//    @JsonIgnoreProperties(value = {"employee_family","hibernateLazyInitializer"})
//    @JoinColumn(name = "employee_id", nullable = false)
//    private Employee employee;

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
