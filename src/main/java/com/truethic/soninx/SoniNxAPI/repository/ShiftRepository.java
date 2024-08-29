package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Page<Shift> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    Shift findByIdAndStatus(long id, boolean b);

    List<Shift> findAllByStatus(boolean b);
    List<Shift> findAllByInstituteIdAndStatus(long instituteId, boolean b);
}
