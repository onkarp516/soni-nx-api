package com.truethic.soninx.SoniNxAPI.views;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "attendance_view")
public class AttendanceView implements Serializable {
    @Id
    private Long id;
    private Long employeeId;
    private Long shiftId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String totalTime;
    private Double workingHours;
    private Double lunchTime;
    private Boolean status;
    private String remark;
    private String adminRemark;

    private String finalDaySalaryType; // hr=> hour basis, point=> point basis, pcs=> pcs basis, day=> day basis
    private Double finalDaySalary;
    private String attendanceStatus; // approved, pending

    private Double totalWorkTime; // sum of total_time in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Double wagesPerDay; // wages_per_day
    private Double wagesHourBasis;  // sum of wages_hour_basis
    private Double breakWages;
    private Boolean isAttendanceApproved;
    private Long instituteId;
}
