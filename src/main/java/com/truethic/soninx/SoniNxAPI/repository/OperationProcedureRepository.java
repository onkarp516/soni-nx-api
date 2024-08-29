package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.OperationProcedure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationProcedureRepository extends JpaRepository<OperationProcedure, Long> {
    Page<OperationProcedure> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    List<OperationProcedure> findAllByStatus(boolean b);

    OperationProcedure findByIdAndStatus(long id, boolean b);
}
