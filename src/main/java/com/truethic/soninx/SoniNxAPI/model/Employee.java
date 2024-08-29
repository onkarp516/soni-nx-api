package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "employee_tbl")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String address;
    private String deviceId;
    @Column(unique = true)
    private Long mobileNumber;
    private String gender;
    private LocalDate dob;
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

    private LocalDate effectedDate;

    private Boolean employeeHavePf;
    private Double employerPf;
    private Double employeePf;
    private Boolean employeeHaveEsi;
    private Double employerEsi;
    private Double employeeEsi;
    private Boolean employeeHaveProfTax;
    private Boolean showSalarySheet;

    @ManyToOne
//    @JsonIgnoreProperties(value = {"employee","hibernateLazyInitializer"})
    @JsonManagedReference
    @JoinColumn(name = "designation_id", nullable = false)
    private Designation designation;

    @ManyToOne
//    @JsonIgnoreProperties(value = {"employee","hibernateLazyInitializer"})
    @JsonManagedReference
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @ManyToOne
//    @JsonIgnoreProperties(value = {"employee","hibernateLazyInitializer"})
    @JsonManagedReference
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "company_id")
    private Company company;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<Attendance> attendanceList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeFamily> employeeFamily;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeEducation> employeeEducation;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeExperienceDetails> employeeExperienceDetails;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeDocument> employeeDocuments;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeReference> employeeReferences;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeSalary> employeeSalaries;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<TaskMaster> taskList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<Downtime> downtimeList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeLeave> employeeLeaveList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<AdvancePayment> advancePaymentList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeePayroll> employeePayrollList;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<EmployeeInspection> employeeInspections;

    private String wagesOptions;

    /*{ value: "pcs", label: "PCS basis" },
    { value: "point", label: "Point basis" },
    { value: "hr", label: "Hr. basis" },
    { value: "day", label: "Day basis" },
    */
    private String employeeWagesType;
    private String weeklyOffDay;

    private String policeCaseDetails;
    private Boolean isExperienceEmployee;
    private Boolean canWeContactPreviousCompany;
    private String hobbies;
    private Double expectedSalary;
    private LocalDate doj;
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
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    private Boolean hasOwnMobileDevice;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<SalarySlipPayheads> salarySlipPayheadsList;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;

}
