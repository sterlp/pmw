package org.sterl.pmw.component;

import java.time.Instant;

import org.slf4j.LoggerFactory;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;

public class LoggingWorkflowStatusObserver implements WorkflowStatusObserver {

    @Override
    public <T extends WorkflowState> void workdlowCreated(Class<?> triggerClass, WorkflowId workflowId,
            Workflow<T> workflow, T userState) {
        LoggerFactory.getLogger(triggerClass).info("Workflow created: {} - {}", workflow.getName(), workflowId);
    }

    @Override
    public void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Workflow started: {} ", runningWorkflow);
    }

    @Override
    public void workflowSuspended(Class<?> triggerClass, Instant until,
            RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Workflow suspended: {}  until={}", runningWorkflow, until);
    }

    @Override
    public void workflowSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Workflow success: {}", runningWorkflow);
    }

    @Override
    public void workflowFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
            Exception error) {
        LoggerFactory.getLogger(triggerClass).error("Workflow failed: {}", runningWorkflow, error);
    }

    @Override
    public void stepStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Step started: {} - {}", runningWorkflow.getCurrentStep(), runningWorkflow.workflowId());
    }

    @Override
    public void stepSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Step success: {} - {}", runningWorkflow.getCurrentStep(), runningWorkflow.workflowId());
    }

    @Override
    public void stepFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
            Exception error) {
        LoggerFactory.getLogger(triggerClass).error("Step failed: {}", runningWorkflow, error);
    }

    @Override
    public void stepFailedRetry(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
            Exception error) {
        LoggerFactory.getLogger(triggerClass).warn("Step faild, will retry: {}", runningWorkflow, error);
    }
}
