package org.sterl.pmw;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

public interface WorkflowService<RegistryType> {
    Collection<String> listWorkflows();
    
    WorkflowId execute(String workflowName);
    WorkflowId execute(String workflowName, Serializable state);
    WorkflowId execute(String workflowName, Serializable state, Duration delay);

    <T extends Serializable> WorkflowId execute(Workflow<T> workflow);
    <T extends Serializable> WorkflowId execute(Workflow<T> workflow, T state);
    <T extends Serializable> WorkflowId execute(Workflow<T> workflow, T state, Duration delay);

    TriggerStatus status(WorkflowId runningWorkflowId);
    void cancel(WorkflowId runningWorkflowId);

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
