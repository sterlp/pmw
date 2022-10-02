package org.sterl.pmw.boundary;

import java.time.Duration;

import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public interface WorkflowService<RegistryType> {
    WorkflowId execute(String workflowName);
    WorkflowId execute(String workflowName, WorkflowState state);
    WorkflowId execute(String workflowName, WorkflowState state, Duration delay);

    <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow);
    <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow, T state);
    <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow, T state, Duration delay);

    void runOrQueueNextStep(RunningWorkflowState<?> runningWorkflowState);

    WorkflowStatus status(WorkflowId workflowId);
    void cancel(WorkflowId workflowId);

    /**
     * Clear all running and registered workflows, mainly used in tests
     */
    void clearAllWorkflows();

    /**
     * Should register a new workflow or throw an exception if the workflow is already known.
     *
     * @throws IllegalArgumentException if the workflow is already known
     */
    <T extends WorkflowState> RegistryType register(Workflow<T> workflow) throws IllegalArgumentException;

    int workflowCount();
}
