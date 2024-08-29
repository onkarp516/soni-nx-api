package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemAccessPermissions;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String userRole;
    private String permissions;

    @ManyToOne
    @JsonIgnoreProperties(value = {"users", "hibernateLazyInitializer"})
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "role_id")
    @JsonManagedReference
    private RoleMaster roleMaster;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<SystemAccessPermissions> systemAccessPermissions;

    private String plain_password;
    private Boolean isSuperadmin;
    private Long createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    private Boolean isAdmin;
    @ManyToOne
    @JoinColumn(name = "institute_id")
    @JsonBackReference
    private Institute institute;

}
