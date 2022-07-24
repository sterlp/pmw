package org.sterl.pmw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import lombok.Getter;

public class Workflow<T extends AbstractWorkflowContext> {
    
    public static <T extends AbstractWorkflowContext> WorkflowFactory<T> builder(
            String name, Supplier<T> newContextCreator) {
        return new WorkflowFactory<T>(name, newContextCreator);
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
    
    public int getStepCount() {
        return workflowSteps.size();
    }
    
    public WorkflowStep<T> nextStep(T c) {
        var state = c.getInternalWorkflowContext();
        var currentStepIndex = state.getCurrentStepIndex();
        if (currentStepIndex == 0) {
            state.workflowStarted();
        } else if (currentStepIndex + 1 > workflowSteps.size()) {
            state.workflowEnded();
            return null;
        }
        return workflowSteps.get(currentStepIndex);
    }

    public boolean success(WorkflowStep<T> currentStep, T c) {
        c.getInternalWorkflowContext().stepSuccessfullyFinished(currentStep);
        return nextStep(c) != null;
    }

    public boolean fail(WorkflowStep<T> nextStep, T c, Exception e) {
        int retryCount = c.getInternalWorkflowContext().stepFailed(nextStep, e);
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
