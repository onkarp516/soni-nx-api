package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "employee_education_tbl")
public class EmployeeEducation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String designationName;
    private String schoolName;
    private String year;
    private String grade;
    private String percentage;
    private String mainSubject;
//
//    @ManyToOne(fetch = FetchType.LAZY,
//            cascade = {CascadeType.ALL})
//    @JsonIgnoreProperties(value = {"employee_education","hibernateLazyInitializer"})
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
