package org.sterl.pmw.model;

import java.io.Serializable;
import java.time.Duration;

import org.sterl.pmw.command.TriggerWorkflowCommand;

/**
 * Context of the given workflow, which allows e.g.
 */
public interface WorkflowContext<T extends Serializable> {

    T data();
    /**
     * Complete and commit the current step but cancel any other steps.
     */
    void cancelWorkflow();
    
    int executionCount();
    
    /**
     * This method shouldn't be directly called, use the <b>sleep</b> factory method of the workflow builder.
     */
    void delayNextStepBy(Duration duration);
    
    /**
     * Set the next step to suspended, means it requires a resume call
     */
    void setSuspendNext(boolean value);
    
    /**
     * In case of a own resume this gives the reference to the next task id.
     */
    String nextTaskId();
    
    /**
     * This method shouldn't be directly called, use the <b>trigger</b> factory method of the workflow builder.
     */
    <R extends Serializable> void addCommand(TriggerWorkflowCommand<R> command);
}
