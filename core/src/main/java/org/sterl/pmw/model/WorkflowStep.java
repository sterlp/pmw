package org.sterl.pmw.model;

import java.io.Serializable;

public interface WorkflowStep<T extends Serializable> {
    /**
     * Name of the step itself, should be unique.
     */
    String getName();
    /**
     * Optional label for the connector leading to this step, just for readability
     */
    String getConnectorLabel();

    void apply(WorkflowContext<T> context);
}
