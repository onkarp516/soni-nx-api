package com.truethic.soninx.SoniNxAPI.dto;

import com.truethic.soninx.SoniNxAPI.model.*;
import lombok.Data;

import java.util.List;

@Data
public class EmployeeDTO {
    private Long id;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String address;
    private Long mobileNumber;
    private String gender;
    private String dob;
    private String cast;
    private String reasonToJoin;
    private Integer age;
    private String religion;
    private String marriageStatus;
    private Double height;
    private Double weight;
    private String bloodGroup;
    private Boolean isSpecks;
    private Boolean isDisability;
    private String disabilityDetails;
    private Boolean isInjured;
    private String injureDetails;
    private Double wagesPerDay;

    private Boolean employeeHavePf;
    private Double employerPf;
    private Double employeePf;
    private Boolean employeeHaveEsi;
    private Double employerEsi;
    private Double employeeEsi;
    private Boolean employeeHaveProfTax;
    private Boolean showSalarySheet;

    private Designation designation;
    private Shift shift;
    private Company company;
    private Site site;

    private List<Attendance> attendanceList;
    private List<EmployeeFamily> employeeFamily;
    private List<EmployeeEducation> employeeEducation;
    private List<EmployeeExperienceDetails> employeeExperienceDetails;
    private List<EmployeeDocument> employeeDocuments;
    private List<EmployeeReference> employeeReferences;
    private List<TaskMaster> taskList;
    private List<Downtime> downtimeList;
    private List<EmployeeLeave> employeeLeaveList;
    private List<AdvancePayment> advancePaymentList;
    private List<EmployeeSalaryDTO> employeeSalaryList;

    private String wagesOptions;
    private String employeeWagesType;
    private String weeklyOffDay;
    private String policeCaseDetails;
    private Boolean isExperienceEmployee;
    private Boolean canWeContactPreviousCompany;
    private String hobbies;
    private Double expectedSalary;
    private String doj;
    private Boolean readyToWorkInThreeShift;
    private String readyToWorkInMonths;
    private String bankName;
    private String branchName;
    private String accountNo;
    private String ifscCode;
    private String pfNumber;
    private String esiNumber;
    private String panNumber;
    private String textPassword;
    private String password;
    private String employeeType;

    private Long createdBy;
    private String createdAt;
    private Long updatedBy;
    private String updatedAt;
    private Boolean status;
    private Boolean hasOwnMobileDevice;
}
