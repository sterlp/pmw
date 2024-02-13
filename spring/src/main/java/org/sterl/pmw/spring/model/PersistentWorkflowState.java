package org.sterl.pmw.spring.model;

import java.time.OffsetDateTime;

import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowStatus;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PMW_WORKFLOW_STATE")
@Data
@EqualsAndHashCode
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PersistentWorkflowState {

    @EmbeddedId
    @AttributeOverrides(
        @AttributeOverride(name = "value", column = @Column(name = "id", length = 50, updatable = false))
    )
    private WorkflowId id;
    private String name;
    @Default
    @Column(updatable = false, name = "created_time")
    private OffsetDateTime created = OffsetDateTime.now();
    @Column(name = "start_time")
    private OffsetDateTime start;
    @Column(name = "end_time")
    private OffsetDateTime end;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Default
    private WorkflowStatus status = WorkflowStatus.PENDING;
    
    @Lob
    private String userState;
    
    @Column(columnDefinition = "TEXT")
    private String lastError;

    public PersistentWorkflowState cancel() {
        if (WorkflowStatus.ACTIVE_STATE.contains(this.status)) {
            end = OffsetDateTime.now();
            status = WorkflowStatus.CANCELED;
        }
        return this;
    }
}

