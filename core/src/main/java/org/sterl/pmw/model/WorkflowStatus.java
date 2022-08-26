package org.sterl.pmw.model;

public enum WorkflowStatus {
    /** Waiting for execution */
    PENDING,
    CANCELED,
    RUNNING,
    /** Next step was delayed */
    SLEEPING,
    COMPLETE,
    FAILED
}
