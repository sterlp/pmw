package org.sterl.pmw.model;

import java.time.Duration;

/**
 * Context of the given workflow, which allows e.g.
 *
 * <li> select execution time of the next step
 * <li> cancel the workflow
 */
public interface WorkflowContext {

    /**
     * @return the current retry count of the given step
     */
    int getStepRetryCount();

    WorkflowContext delayNextStepBy(Duration duration);
    /**
     * @return the current set delay and clears it, never <code>null</code>
     */
    Duration consumeDelay();

    WorkflowContext cancelWorkflow();

}
