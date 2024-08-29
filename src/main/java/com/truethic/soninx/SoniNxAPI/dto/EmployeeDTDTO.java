package com.truethic.soninx.SoniNxAPI.dto;

import lombok.Data;


@Data
public class EmployeeDTDTO {
    private Long employeeId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private Long mobileNumber;
    private String gender;
    private String dob;
    private String employeeType;
    private String desigName;
    private String shiftName;
    private String companyName;
    private Double wagesPerDay;
    private Boolean status;
    private String createdAt;
}
