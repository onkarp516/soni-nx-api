package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.PushMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;

public interface PushMessageRepository extends JpaRepository<PushMessage, Long> {
    @Query(value = "SELECT * FROM `push_message_tbl` WHERE ?1 BETWEEN from_date AND to_date AND status=?2 AND institute_id=?3 ORDER BY id" +
            " DESC LIMIT 1", nativeQuery = true)
    PushMessage getMessageByDateAndInstituteId(Serializable localDate, boolean b, long instituteId);
}
