package org.sterl.pmw.model;

import java.io.Serializable;

import org.sterl.pmw.WorkflowService;

public interface WorkflowStep<T extends Serializable, R extends Serializable> {
    /**
     * Name of the step itself, should be unique.
     */
    String getName();
    /**
     * Optional label for the connector leading to this step, just for readability
     */
    String getConnectorLabel();

    R apply(T state, WorkflowContext context, WorkflowService<?> workflowService);
}
