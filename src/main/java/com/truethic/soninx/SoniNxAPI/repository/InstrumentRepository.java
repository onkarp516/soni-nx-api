package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.Instrument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    Page<Instrument> findByStatusOrderByIdDesc(Pageable pageable, boolean b);

    Instrument findByIdAndStatus(long id, boolean b);

    List<Instrument> findAllByStatus(boolean b);
}
