package org.sterl.pmw.model;

import java.time.Duration;

/**
 * Context of the given workflow, which allows e.g.
 *
 * <ul>
 * <li> select execution time of the next step</li>
 * <li> cancel the workflow</li>
 * </ul>
 */
public interface WorkflowContext {

    /**
     * @return the current retry count of the given step
     */
    int getStepRetryCount();

    /**
     * This method shouldn't be directly called, use the <b>sleep</b> factory method of the workflow builder.
     */
    WorkflowContext delayNextStepBy(Duration duration);
    /**
     * @return the current set delay and clears it, never <code>null</code>
     */
    Duration consumeDelay();
    boolean hasDelay();

    WorkflowContext cancelWorkflow();

}
