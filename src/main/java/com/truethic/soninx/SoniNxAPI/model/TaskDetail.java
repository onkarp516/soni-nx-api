package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "task_detail_tbl")
public class TaskDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"users", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JsonIgnoreProperties(value = {"users", "hibernateLazyInitializer"})
    @JoinColumn(name = "task_id", nullable = false)
    private TaskMaster taskMaster;

    private LocalDate taskDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double totalTime; // in minutes
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;

    @ManyToOne
    @JsonIgnoreProperties(value = {"downtime", "hibernateLazyInitializer"})
    @JoinColumn(name = "work_break_id", nullable = true)
    private WorkBreak workBreak;

    private Double workPoint;
    private Double wagesHourBasis;
    private Double wagesMinuteBasis;
    private Double wagesPointBasis;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
    /* Task end */
}

