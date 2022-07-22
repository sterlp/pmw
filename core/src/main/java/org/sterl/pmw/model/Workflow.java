package org.sterl.pmw.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.Getter;

public class Workflow<T extends AbstractWorkflowContext> {

    @Getter
    private final String name;
    @Getter
    private final Supplier<T> newContextCreator;
    @Getter
    private int retryCount = 3;
    private final List<WorkflowStep<T>> workflowSteps = new ArrayList<>();
    
    public Workflow(String name, Supplier<T> newContextCreator) {
        super();
        this.name = name;
        this.newContextCreator = newContextCreator;
    }
    
    public int getStepCount() {
        return workflowSteps.size();
    }
    
    public WorkflowStep<T> getNextStep(T c) {
        if (c.getNextStep() + 1 > workflowSteps.size()) return null;
        return workflowSteps.get(c.getNextStep());
    }

    public boolean success(WorkflowStep<T> nextStep, T c) {
        c.setNextStep(c.getNextStep() + 1);
        return getNextStep(c) != null;
    }

    public boolean fail(WorkflowStep<T> nextStep, T c, Exception e) {
        c.retry(e);
        c.setLastFailedStep(c.getNextStep());
        return retryCount > c.getRetryCount();
    }
    
    public Workflow<T> retryCount(int count) {
        this.retryCount = count;
        return this;
    }

    @Override
    public String toString() {
        return "Workflow [name=" + name + ", retryCount=" + retryCount + ", workflowSteps=" + workflowSteps.size() + "]";
    }
}
