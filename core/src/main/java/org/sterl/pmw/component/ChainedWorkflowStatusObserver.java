package org.sterl.pmw.component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChainedWorkflowStatusObserver implements WorkflowStatusObserver {

    private final List<WorkflowStatusObserver> observers = new ArrayList<>();

    public ChainedWorkflowStatusObserver addObserver(WorkflowStatusObserver observer) {
        this.observers.add(observer);
        return this;
    }
    public ChainedWorkflowStatusObserver removeObserver(WorkflowStatusObserver observer) {
        this.observers.remove(observer);
        return this;
    }

    @Override
    public void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        applyWorkflowStatusObserver(o -> o.workflowStart(triggerClass, runningWorkflow));
    }

    @Override
    public void workflowSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        applyWorkflowStatusObserver(o -> o.workflowSuccess(triggerClass, runningWorkflow));
    }

    @Override
    public void workflowFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error) {
        applyWorkflowStatusObserver(o -> o.workflowFailed(triggerClass, runningWorkflow, error));
    }

    @Override
    public void stepStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        applyWorkflowStatusObserver(o -> o.stepStart(triggerClass, runningWorkflow));
    }

    @Override
    public void stepSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        applyWorkflowStatusObserver(o -> o.stepSuccess(triggerClass, runningWorkflow));
    }

    @Override
    public void stepFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error) {
        applyWorkflowStatusObserver(o -> o.stepFailed(triggerClass, runningWorkflow, error));
    }

    @Override
    public void stepFailedRetry(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
            Exception error) {
        applyWorkflowStatusObserver(o -> o.stepFailedRetry(triggerClass, runningWorkflow, error));
    }

    @Override
    public void workflowSuspended(Class<?> triggerClass, Instant until,
            RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
        applyWorkflowStatusObserver(o -> o.workflowSuspended(triggerClass, until, runningWorkflow));
    }

    private void applyWorkflowStatusObserver(Consumer<WorkflowStatusObserver> consume) {
        for (WorkflowStatusObserver o : observers) {
            try {
                consume.accept(o);
            } catch (Exception e) {
                log.warn("Observer {} failed.", o.getClass(), e);
            }
        }
    }
}