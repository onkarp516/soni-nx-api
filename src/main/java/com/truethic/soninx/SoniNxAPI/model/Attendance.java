package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Entity
@Table(name = "attendance_tbl")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnoreProperties(value = {"attendance", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeInspection> employeeInspections;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;

    private String punchInImage;
    private String punchOutImage;

    private LocalDate attendanceDate;
    //    private LocalTime checkInTime;
//    private LocalTime checkOutTime;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalTime totalTime;
    private Double workingHours;
    private Double prodWorkingHours;
    private Double WorkingHourWithSetting;
    private Double lunchTime;

    private Double wagesPerDay;
    private Double wagesPerHour;
    private Double wagesPerMin;
    private Double wagesPoint; // wages per point

    private Long totalProdQty; // sum of total_count
    private Double totalWorkTime; // sum of total_time in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Double totalWorkPoint; // sum of work_point

    private Double wagesPointBasis; // sum of wages_point_basis
    private Double wagesHourBasis;  // sum of wages_hour_basis
    private Double wagesMinBasis;  // sum of wages_min_basis
    private Double wagesPcsBasis;
    private Double breakWages;
    private Double netPcsWages;

    private String finalDaySalaryType; // hr=> hour basis, point=> point basis, pcs=> pcs basis, day=> day basis
    private Double finalDaySalary;
    private String attendanceStatus; // approved, pending

    private Double actualProduction;
    private Double reworkQty;
    private Double machineRejectQty;
    private Double doubtfulQty;
    private Double unMachinedQty;
    private Double okQty;

    private Boolean isHalfDay;
    private Boolean isLate;
    private Boolean isManualPunchIn;
    private Boolean isManualPunchOut;
    private  Boolean isAttendanceApproved;

    private String remark;
    private String adminRemark;
    private Boolean status;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
