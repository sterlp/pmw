package org.sterl.pmw.spring.model;

import java.time.OffsetDateTime;

import org.sterl.pmw.model.WorkflowStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PMW_TASK_STATE")
@Data
@EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TaskEntity {

    @Id
    private String id;
    @Id
    private String workflowName;
    @Id
    private String stepName;

    @Default
    @Column(updatable = false, name = "created_time")
    private OffsetDateTime created = OffsetDateTime.now();
    @Column(name = "start_time")
    private OffsetDateTime start;
    @Column(name = "end_time")
    private OffsetDateTime end;
    
    @Default
    private int executionCount = 0;
    
    /** priority, the higher a more priority it will get */
    @Default
    private int priority = 4;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Default
    private WorkflowStatus status = WorkflowStatus.PENDING;
    
    private String runningOn;
    
    @Lob
    private String state;
    
    @Column(columnDefinition = "TEXT")
    private String lastError;

    public TaskEntity cancel() {
        if (WorkflowStatus.ACTIVE_STATE.contains(this.status)) {
            end = OffsetDateTime.now();
            status = WorkflowStatus.CANCELED;
        }
        return this;
    }
}

