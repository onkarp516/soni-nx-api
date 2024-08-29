package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Entity
@Table(name = "task_master_tbl")
public class TaskMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"users", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JsonIgnoreProperties(value = {"users", "hibernateLazyInitializer"})
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @OneToOne(optional = true)
    @JsonIgnoreProperties(value = {"payhead", "hibernateLazyInitializer"})
    @JoinColumn(name = "task_master_id", nullable = true)
    private TaskMaster taskMaster;

    private LocalDate taskDate;
    /*private LocalTime startTime;
    private LocalTime endTime;*/
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalTime workingTime; // in HH:MM:SS
    private Double totalTime; // in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Integer taskType; // 1=>Task, 2=>Downtime, 3=>Setting time, 4=> task without machine
    private Boolean workDone; // 1=>Work done, 0=>Work not done
    private String remark; // start task remark
    private String endRemark; // end task remark
    private String taskStatus; // in-progress, complete
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;

    private String correctiveAction;
    private String preventiveAction;


    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeInspection> employeeInspections;
    /* Downtime start */
    @ManyToOne
    @JsonIgnoreProperties(value = {"downtime", "hibernateLazyInitializer"})
    @JoinColumn(name = "work_break_id", nullable = true)
    private WorkBreak workBreak;

    /* Downtime end */

    /* Task start */
    @ManyToOne
    @JsonIgnoreProperties(value = {"task", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "machine_id", nullable = true)
    private Machine machine;

    @ManyToOne
    @JsonIgnoreProperties(value = {"task", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_id", nullable = true)
    private Job job;

    @ManyToOne
    @JsonIgnoreProperties(value = {"task", "hibernateLazyInitializer"})
    @JsonIgnore
    @JoinColumn(name = "job_operation_id", nullable = true)
    private JobOperation jobOperation;

    private Long machineStartCount;
    private Long machineEndCount;
    private Long totalCount;

    /* Job Operation Related Data*/
    private String employeeWagesType;
    private Double cycleTime;
    private Double pcsRate;
    private Double averagePerShift;
    private Double perJobPoint; // per job point

    /* Task Process Data*/
    private Double jobsPerHour;
    private Double workingHour;
    private Double prodWorkingHour;
    private Double settingTimeInMin;
    private Double settingTimeInHour;
    private Double WorkingHourWithSetting;
    private Double prodPoint;
    private Double settingTimePoint;
    private Double totalPoint;

    private Double requiredProduction;
    private Double actualProduction;
    private Double shortProduction;
    private Double percentageOfTask;

    /*Salary Related Data*/
//    private Double workPoint;
    private Double wagesPerPoint; // start of task (wages per point)
    private Double wagesPerDay;
    private Double wagesPerHour;
    private Double wagesPerMinute;

    private Double wagesPointBasis;
    private Double wagesHourBasis;
    private Double wagesMinutesBasis;
    private Double wagesPcsBasis;
    private Double breakWages;

    private Double reworkQty;
    private Double machineRejectQty;
    private Double doubtfulQty;
    private Double unMachinedQty;
    private Double okQty;


    private String adminRemark;
    private String supervisorRemark;
    /* Task end */
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
