package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.ControlMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ControlMethodRepository extends JpaRepository<ControlMethod, Long> {
    ControlMethod findByIdAndStatus(long id, boolean b);

    List<ControlMethod> findAllByStatus(boolean b);
}
