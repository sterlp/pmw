package org.sterl.pmw.component;

import java.time.Instant;

import org.slf4j.LoggerFactory;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;

public class LoggingWorkflowStatusObserver implements WorkflowStatusObserver {@Override
    public void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        LoggerFactory.getLogger(triggerClass).info("Workflow started : {} ", runningWorkflow);
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
        LoggerFactory.getLogger(triggerClass).error("{} failed", runningWorkflow, error);
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
        LoggerFactory.getLogger(triggerClass).error("{} failed", runningWorkflow, error);
    }

    @Override
    public void stepFailedRetry(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
            Exception error) {
        LoggerFactory.getLogger(triggerClass).warn("{} failed, will retry.", runningWorkflow, error);
    }
}
