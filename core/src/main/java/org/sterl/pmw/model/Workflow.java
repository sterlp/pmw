package org.sterl.pmw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import lombok.Getter;

public class Workflow<T extends WorkflowState> {

    public static <T extends WorkflowState> WorkflowFactory<T> builder(
            final String name, final Supplier<T> newContextCreator) {
        return new WorkflowFactory<>(name, newContextCreator);
    }

    @Getter
    private final String name;
    private final Supplier<T> newContextCreator;
    private final List<WorkflowStep<T>> workflowSteps = new ArrayList<>();

    public Workflow(String name, Supplier<T> newContextCreator) {
        super();
        this.name = name;
        this.newContextCreator = newContextCreator;
    }

    public List<WorkflowStep<T>> getSteps() {
        return Collections.unmodifiableList(this.workflowSteps);
    }

    public int getStepCount() {
        return workflowSteps.size();
    }

    public WorkflowStep<T> nextStep(InternalWorkflowState state) {
        var currentStepIndex = state.getCurrentStepIndex();
        if (currentStepIndex == 0) {
            state.workflowStarted();
        } else if (currentStepIndex + 1 > workflowSteps.size()) {
            state.workflowEnded();
            return null;
        }
        return workflowSteps.get(currentStepIndex);
    }

    /**
     * Marks the current step as a success and returns the next step if available
     * @return the next step if available, otherwise <code>null</code>
     */
    public WorkflowStep<T> success(WorkflowStep<T> currentStep, InternalWorkflowState state) {
        state.stepSuccessfullyFinished(currentStep);
        final WorkflowStep<T> nextStep = nextStep(state);
        return nextStep;
    }

    public boolean fail(WorkflowStep<T> nextStep, InternalWorkflowState state, Exception e) {
        final int retryCount = state.stepFailed(nextStep, e);
        return retryCount < nextStep.getMaxRetryCount();
    }

    void setWorkflowSteps(Collection<WorkflowStep<T>> workflowSteps) {
        this.workflowSteps.clear();
        this.workflowSteps.addAll(workflowSteps);
    }

    public T newEmtyContext() {
        return this.newContextCreator.get();
    }

    @Override
    public String toString() {
        return "Workflow [name=" + name + ", workflowSteps=" + workflowSteps.size() + "]";
    }

    public WorkflowStep<T> getStepByPosition(int pos) {
        if (pos > workflowSteps.size()) return null;
        else return workflowSteps.get(pos);

    }
}
