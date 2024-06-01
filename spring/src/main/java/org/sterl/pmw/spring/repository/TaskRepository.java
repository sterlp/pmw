package org.sterl.pmw.spring.repository;

import java.time.OffsetDateTime;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.spring.model.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, WorkflowId> {

    @Modifying
    @Query("""
           UPDATE PersistentWorkflowState
           SET status = :toStatus, end = :endTime
           WHERE status in :fromStatus
           """)
    int setStatusFromStatus(@Param("toStatus") WorkflowStatus toStatus, 
            @Param("fromStatus") Set<WorkflowStatus> fromStatus, 
            @Param("endTime") OffsetDateTime endTime);

}
