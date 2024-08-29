package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;

@Data
public class ShiftDTDTO {
    private Long id;
    private String name;
    private String fromTime;
    private String toTime;
    private String threshold;
    private Integer lunchTime;
    private String workingHours;
    private Boolean isNightShift;
    private Long considerationCount;   //Late Count to be Considered for deduction
    private Boolean isDayDeduction;
    private String dayValueOfDeduction;
    private double hourValueOfDeduction;
    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
}
