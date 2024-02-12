package org.sterl.pmw.spring;

import java.time.Duration;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public class SpringWorkflowService implements WorkflowService {

    @Override
    public WorkflowId execute(String workflowName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkflowId execute(String workflowName, WorkflowState state) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkflowId execute(String workflowName, WorkflowState state, Duration delay) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkflowId execute(Workflow workflow) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkflowId execute(Workflow workflow, WorkflowState state) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkflowId execute(Workflow workflow, WorkflowState state, Duration delay) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void runOrQueueNextStep(WorkflowId id, RunningWorkflowState runningWorkflowState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public WorkflowStatus status(WorkflowId workflowId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cancel(WorkflowId workflowId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int clearAllWorkflows() {
        
        
    }

    @Override
    public Object register(Workflow workflow) throws IllegalArgumentException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int workflowCount() {
        // TODO Auto-generated method stub
        return 0;
    }

}
