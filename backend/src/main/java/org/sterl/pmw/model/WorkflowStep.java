package org.sterl.pmw.model;

import java.io.Serializable;

public interface WorkflowStep<T extends Serializable> {
    /**
     * Unique ID of this step in case of updates to the workflow this Id should not change.
     */
    String getId();
    String getDescription();
    /**
     * Optional label for the connector leading to this step, just for readability
     */
    String getConnectorLabel();
    
    boolean isTransactional();

    void apply(WorkflowContext<T> context);
}
