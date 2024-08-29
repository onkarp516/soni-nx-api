package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

import java.util.List;

@Data
public class AttendanceDTDTO {
    private Long id;
    private Long employeeId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String attendanceDate;
    private String checkInTime;
    private String checkOutTime;
    private String totalTime;
    private Boolean status;
    private String remark;
    private String adminRemark;

    private String finalDaySalaryType; // hr=> hour basis, point=> point basis, pcs=> pcs basis, day=> day basis
    private Double finalDaySalary;
    private String attendanceStatus; // approved, pending

    private Long totalProdQty; // sum of total_count
    private Double totalWorkTime; // sum of total_time in minutes
    private Double actualWorkTime; // in minutes (total task time - break time)
    private Double totalWorkPoint; // sum of work_point
    private Double wagesHourBasis;  // sum of wages_hour_basis
    private Double wagesPointBasis; // sum of wages_point_basis
    private Double wagesPcsBasis;  // sum of wages_pcs_basis
    private Double wagesPerDay;  // wages_per_day

    private List<TaskDTO> taskDTOList;
}
