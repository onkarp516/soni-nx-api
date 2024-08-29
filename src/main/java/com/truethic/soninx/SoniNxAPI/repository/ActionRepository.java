package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Action;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRepository extends JpaRepository<Action, Long> {
    Action findByIdAndStatus(long actionId, boolean b);

    List<Action> findAllByStatus(boolean b);

    Page<Action> findByStatusOrderByIdDesc(Pageable pageable, boolean b);
}
