package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "downtime_tbl")
public class Downtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"downtime", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JsonIgnoreProperties(value = {"downtime", "hibernateLazyInitializer"})
    @JoinColumn(name = "work_break_id", nullable = false)
    private WorkBreak workBreak;

    private LocalDate downtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double totalTime; // in minutes
    private String note;
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private Boolean status;

    private Long createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
