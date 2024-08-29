package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "employee_experience_details_tbl")
public class EmployeeExperienceDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String companyName;
    private String duration;
    private String designationName;
    private String incomePerMonth;
    private String reasonToResign;

//    @ManyToOne(fetch = FetchType.LAZY,
//            cascade = {CascadeType.ALL})
//    @JsonIgnoreProperties(value = {"employee_experience_Details","hibernateLazyInitializer"})
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
