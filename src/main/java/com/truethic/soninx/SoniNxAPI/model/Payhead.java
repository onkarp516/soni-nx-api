package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "payhead_tbl")
public class Payhead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double percentage;
    private Double amount;
    private String payheadSlug;

    @OneToOne(optional = true)
    @JsonIgnoreProperties(value = {"payhead", "hibernateLazyInitializer"})
    @JoinColumn(name = "percentage_of", nullable = true)
    private Payhead percentageOf;

    @JsonBackReference
    @OneToMany(fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<MasterPayhead> masterPayheads;

    private Boolean isAdminRecord;
    private Boolean status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Boolean payheadStatus;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
