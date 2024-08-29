package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Designation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

import java.util.List;

public interface DesignationRepository extends JpaRepository<Designation, Long> {
    Page<Designation> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    @Procedure("insertDesignation")
    void insertDesignation(String desigName, String code);

    Designation findByIdAndStatus(Long designationId, boolean b);

    List<Designation> findAllByStatus(boolean b);
    List<Designation> findAllByInstituteIdAndStatus(long instituteId, boolean b);
}
