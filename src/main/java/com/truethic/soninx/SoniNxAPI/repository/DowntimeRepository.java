package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Downtime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DowntimeRepository extends JpaRepository<Downtime, Long> {
    List<Downtime> findByStatus(boolean b);
}
