package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.sterl.spring.persistent_tasks.api.RetryStrategy;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class Workflow<T extends Serializable> {
    
    public static <T extends Serializable> WorkflowFactory<T> builder(
            String name, Supplier<T> contextBuilder) {
        Workflow<T> workflow = new Workflow<>(name, contextBuilder);
        return new WorkflowFactory<T>(contextBuilder, workflow);
    }

    @Getter
    private final String name;
    @Getter
    @Setter
    @NonNull
    private RetryStrategy retryStrategy = RetryStrategy.THREE_RETRIES;
    private final Supplier<T> contextBuilder;
    private final List<WorkflowStep<T>> workflowSteps = new ArrayList<>();

    public Workflow(String name, Supplier<T> contextBuilder) {
        super();
        this.name = name;
        this.contextBuilder = contextBuilder;
    }
    
    public T newContext() {
        return contextBuilder.get();
    }

    public List<WorkflowStep<T>> getSteps() {
        return Collections.unmodifiableList(this.workflowSteps);
    }

    public int getStepCount() {
        return workflowSteps.size();
    }

    void setWorkflowSteps(Collection<WorkflowStep<T>> workflowSteps) {
        this.workflowSteps.clear();
        this.workflowSteps.addAll(workflowSteps);
    }

    @Override
    public String toString() {
        return "Workflow [name=" + name + ", workflowSteps=" + workflowSteps.size() + "]";
    }

    public WorkflowStep<T> getNextStep(WorkflowStep<T> current) {
        return getStepByPosition(workflowSteps.indexOf(current) + 1);
    }

    public WorkflowStep<T> getStepByPosition(int pos) {
        if (pos >= workflowSteps.size()) return null;
        else return workflowSteps.get(pos);
    }
}
