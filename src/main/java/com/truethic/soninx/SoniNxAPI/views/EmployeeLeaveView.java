package com.truethic.soninx.SoniNxAPI.views;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "employee_leave_view")
public class EmployeeLeaveView implements Serializable {
    @Id
    private Long id;
    private Long employeeId;
    private Long leaveTypeId;
    private String fullName;
    private String leaveName;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer totalDays;
    private String reason;
    private String leaveStatus;
    private String leaveApprovedBy;
    private String leaveRemark;
    private Boolean status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Long instituteId;
}
