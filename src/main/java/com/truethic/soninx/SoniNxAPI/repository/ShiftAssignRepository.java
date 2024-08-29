package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.ShiftAssign;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

public interface ShiftAssignRepository extends JpaRepository<ShiftAssign, Long> {
    @Query(value = "SELECT * FROM shift_assign_tbl WHERE employee_id=?2 AND ?1 BETWEEN from_date AND to_date ORDER BY id DESC LIMIT 1", nativeQuery = true)
    ShiftAssign getDataFromShiftAssign(LocalDate id, Long aLong);

    List<ShiftAssign> findAllByStatus(boolean b);


    @Modifying
    @Transactional
    @Cascade(CascadeType.DELETE)
    @Query(value = "DELETE FROM shift_assign_tbl WHERE id=?1", nativeQuery = true)
    void deleteEmployeeShift(Long shiftId);

    ShiftAssign findByIdAndStatus(Long id, boolean b);

    @Query(value = "SELECT shift_tbl.name FROM `shift_assign_tbl` LEFT JOIN shift_tbl ON" +
            " shift_assign_tbl.shift_id=shift_tbl.id WHERE employee_id=?1 AND (?2 BETWEEN from_date AND to_date)", nativeQuery = true)
    String getEmployeeNextDayShiftName(Long id, LocalDate nextDay);
}
