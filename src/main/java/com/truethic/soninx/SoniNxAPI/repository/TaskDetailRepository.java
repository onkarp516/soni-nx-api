package com.truethic.soninx.SoniNxAPI.repository;

import com.truethic.soninx.SoniNxAPI.model.TaskDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskDetailRepository extends JpaRepository<TaskDetail, Long> {
    TaskDetail findTop1ByTaskMasterIdOrderByIdDesc(Long taskId);

    TaskDetail findByIdAndStatus(Long taskDetailId, boolean b);

    TaskDetail findTop1ByTaskMasterIdAndWorkBreakOrderByIdDesc(Long taskId, Object o);

    TaskDetail findTop1ByTaskMasterIdAndWorkBreakNotNullOrderByIdDesc(Long id);

    TaskDetail findTop1ByTaskMasterIdAndEndTimeNotNullOrderByIdDesc(Long id);

    TaskDetail findTop1ByTaskMasterIdAndEndTimeNullOrderByIdDesc(Long id);
}
