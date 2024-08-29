package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Inspection;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    Inspection findByIdAndStatus(Long drawingSizeId, boolean b);

    @Modifying
    @Transactional
    @Cascade(CascadeType.DELETE)
    @Query(value = "DELETE FROM inspection_tbl WHERE id=?1", nativeQuery = true)
    void deleteLineInspection(Long id);


    List<Inspection> findByJobOperationIdAndStatus(Long id, boolean b);
}
