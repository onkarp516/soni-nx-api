package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "machine_tbl")
public class Machine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String number;
    private LocalDate dateOfPurchase;
    private Integer cost;
    private String whatMachineMakes;
    private Boolean isMachineCount;
    private Long currentMachineCount;
    private Long defaultMachineCount;

    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<OperationProcedure> operationProcedures;

    private Long createdBy;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean status;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
