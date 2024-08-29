package com.truethic.soninx.SoniNxAPI.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "push_message_tbl")
public class PushMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fromDate;
    private LocalDate toDate;
    @Column(columnDefinition = "TEXT")
    private String message;
    private Boolean status;

    @CreationTimestamp
    private LocalDateTime createdAt;
    private Long createdBy;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private Long updatedBy;
    @ManyToOne
    @JoinColumn(name = "institute_id", nullable = false)
    @JsonBackReference
    private Institute institute;
}
