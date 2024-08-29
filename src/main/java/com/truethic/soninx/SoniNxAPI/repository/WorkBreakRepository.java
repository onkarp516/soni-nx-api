package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.WorkBreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkBreakRepository extends JpaRepository<WorkBreak, Long> {
    WorkBreak findByIdAndStatus(Long id, boolean b);
    List<WorkBreak> findAllByInstituteIdAndStatus(Long id, boolean b);

    List<WorkBreak> findByStatus(boolean b);

    @Query(value = "SELECT * FROM `break_tbl` WHERE break_name LIKE '%lunch%' AND status=1 AND institute_id=?1", nativeQuery = true)
    WorkBreak findByBreakName(long institute_id);
}
