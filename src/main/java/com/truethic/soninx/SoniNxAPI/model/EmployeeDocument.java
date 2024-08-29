package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_document_tbl")
public class EmployeeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"employee_document", "hibernateLazyInitializer"})
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
//
//    @ManyToOne(fetch = FetchType.LAZY,
//            cascade = {CascadeType.ALL})
//    @JsonIgnoreProperties(value = {"employee_document","hibernateLazyInitializer"})
//    @JoinColumn(name = "employee_id", nullable = false)
//    private Employee employee;

    private String imagePath;
    private String imageKey;
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
