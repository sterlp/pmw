package org.sterl.pmw.boundary;

import java.time.Duration;

import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

/**
 * Manages all workflows and their state. Also allows to trigger a workflow.
 */
public interface WorkflowService<RegistryType> {
    WorkflowId execute(String workflowName);
    WorkflowId execute(String workflowName, WorkflowState state, Duration delay);
    
    <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow, T state, Duration delay);
    
    default <T extends WorkflowState> WorkflowId execute(String workflowName, T state) {
        return execute(workflowName, state, Duration.ZERO);
    }

    default <T extends WorkflowState>  WorkflowId execute(Workflow<T> w) {
        return execute(w, w.newEmtyContext());
    }

    default <T extends WorkflowState>  WorkflowId execute(Workflow<T> w, T c) {
        return execute(w, c, Duration.ZERO);
    }
    
    void runOrQueueNextStep(WorkflowId id, RunningWorkflowState<?> runningWorkflowState);

    WorkflowStatus status(WorkflowId workflowId);
    
    void cancel(WorkflowId workflowId);
    int cancelAll();

    /**
     * Should register a new workflow or throw an exception if the workflow is already known.
     *
     * @throws IllegalArgumentException if the workflow is already known
     */
    <T extends WorkflowState> RegistryType register(Workflow<T> workflow) throws IllegalArgumentException;

    int workflowCount();
}
