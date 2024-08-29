package com.truethic.soninx.SoniNxAPI.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "attendance_history_tbl")
public class AttendanceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long attendanceId;
    private Long employeeId;
    private Long shiftId;
    private Long updatingUserId;

    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalTime totalTime;

    private Double wagesPerDay;
    private Double wagesPerHour;
    private Double wagesPerMin;
    private Double wagesPoint;

    private Long totalProdQty; // sum of total_count
    private Double totalWorkTime; // sum of total_time in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Double totalWorkPoint; // sum of work_point

    private Double wagesPointBasis; // sum of wages_point_basis
    private Double wagesHourBasis;  // sum of wages_hour_basis
    private Double wagesMinBasis;  // sum of wages_min_basis
    private Double wagesPcsBasis;

    private String finalDaySalaryType; // hr=> hour basis, point=> point basis, pcs=> pcs basis, day=> day basis
    private Double finalDaySalary;
    private String attendanceStatus; // approved, pending

    private Double actualProduction;
    private Double reworkQty;
    private Double machineRejectQty;
    private Double doubtfulQty;
    private Double unMachinedQty;
    private Double okQty;

    private String remark;
    private String adminRemark;
    private Boolean status;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
