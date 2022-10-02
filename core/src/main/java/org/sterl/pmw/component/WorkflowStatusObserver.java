package org.sterl.pmw.component;

import java.time.Instant;

import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;

public interface WorkflowStatusObserver {
    void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow);

    void workflowSuspended(Class<?> triggerClass, Instant until, RunningWorkflowState<? extends WorkflowState> runningWorkflow);

    void workflowSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow);
    void workflowFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error);

    void stepStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow);
    void stepSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow);
    void stepFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error);
    void stepFailedRetry(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error);



    WorkflowStatusObserver NOP_OBSERVER = new WorkflowStatusObserver() {
        @Override
        public void workflowSuspended(Class<?> triggerClass, Instant until,
                RunningWorkflowState<? extends WorkflowState> runningWorkflow) {}

        @Override
        public void workflowSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {}

        @Override
        public void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {}

        @Override
        public void workflowFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
                Exception error) {
        }

        @Override
        public void stepSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        }

        @Override
        public void stepStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        }

        @Override
        public void stepFailedRetry(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
                Exception error) {
        }

        @Override
        public void stepFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
                Exception error) {
        }
    };
}
