package com.truethic.soninx.SoniNxAPI.views;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "downtime_view")
public class DowntimeView implements Serializable {
    @Id
    private Long downtimeId;
    private LocalDate downtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long instituteId;
}
