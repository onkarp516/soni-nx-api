package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "inspection_tbl")
public class Inspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String drawingSize;
    private Boolean status;

    @ManyToOne
    @JsonIgnoreProperties(value = {"inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JsonIgnoreProperties(value = {"inspection", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_operation_id")
    private JobOperation jobOperation;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
