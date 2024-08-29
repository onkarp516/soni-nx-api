package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Institute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstituteRepository extends JpaRepository<Institute, Long> {
    List<Institute> findAllByStatus(boolean b);
    Institute findByIdAndStatus(long instituteId, boolean b);
}
