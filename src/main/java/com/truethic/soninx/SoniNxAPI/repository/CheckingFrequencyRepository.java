package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.CheckingFrequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckingFrequencyRepository extends JpaRepository<CheckingFrequency, Long> {
    CheckingFrequency findByIdAndStatus(long id, boolean b);

    List<CheckingFrequency> findAllByStatus(boolean b);
}
