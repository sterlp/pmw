package org.sterl.pmw;

import java.io.Serializable;
import java.time.Duration;

import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

public interface WorkflowService<RegistryType> {
    RunningWorkflowId execute(String workflowName);
    RunningWorkflowId execute(String workflowName, Serializable state);
    RunningWorkflowId execute(String workflowName, Serializable state, Duration delay);

    <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow);
    <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow, T state);
    <T extends Serializable> RunningWorkflowId execute(Workflow<T> workflow, T state, Duration delay);

    TriggerStatus status(RunningWorkflowId runningWorkflowId);
    void cancel(RunningWorkflowId runningWorkflowId);

    /**
     * Clear all running and registered workflows, mainly used in tests
     */
    void clearAllWorkflows();

    /**
     * Should register a new workflow or throw an exception if the workflow is already known.
     *
     * @throws IllegalStateException if the workflow is already known
     */
    <T extends Serializable> RegistryType register(Workflow<T> workflow) throws IllegalStateException;

    /**
     * Count of known workflows
     */
    int workflowCount();
}
