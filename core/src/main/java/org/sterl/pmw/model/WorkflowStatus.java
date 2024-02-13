package org.sterl.pmw.model;

import java.util.EnumSet;
import java.util.Set;

public enum WorkflowStatus {
    
    /** Waiting for execution */
    PENDING,
    CANCELED,
    RUNNING,
    /** Next step was delayed */
    SLEEPING,
    COMPLETE,
    FAILED;
    
    public static final Set<WorkflowStatus> END_STATE = EnumSet.of(
            WorkflowStatus.CANCELED, WorkflowStatus.COMPLETE, WorkflowStatus.FAILED);

    public static final Set<WorkflowStatus> ACTIVE_STATE = EnumSet.of(
            WorkflowStatus.PENDING, WorkflowStatus.RUNNING, WorkflowStatus.SLEEPING);
}
