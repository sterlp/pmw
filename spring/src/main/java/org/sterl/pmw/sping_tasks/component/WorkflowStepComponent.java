package org.sterl.pmw.sping_tasks.component;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.spring.persistent_tasks.api.AddTriggerRequest;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TaskId.TriggerBuilder;
import org.sterl.spring.persistent_tasks.api.task.ComplexPersistentTask;
import org.sterl.spring.persistent_tasks.api.task.RunningTrigger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowStepComponent<T extends Serializable, R extends Serializable> implements ComplexPersistentTask<T, R> {

    private final WorkflowService<?> workflowService;
    private final Workflow<?> workflow;
    private final WorkflowStep<T, R> step;

    @Override
    public Collection<AddTriggerRequest<R>> accept(RunningTrigger<T> state) {
        var context = new SimpleWorkflowContext(state);
        R nextState = step.apply(state.getData(), context, workflowService);
        
        var nextStep = workflow.getNextStep(step);
        AddTriggerRequest<R> result = null;
        if (!context.canceled && nextStep != null) {
            result = TriggerBuilder.newTrigger(
                    workflow.getName() + "::" + nextStep.getName(), nextState)
                .runAfter(context.nextDelay)
                .correlationId(state.getCorrelationId())
                .build();
        }
        return result == null ? Collections.emptyList() : result.toList();
    }
    
    @Override
    public RetryStrategy retryStrategy() {
        return workflow.getRetryStrategy();
    }
    
    @Override
    public boolean isTransactional() {
        return true;
    }

    @RequiredArgsConstructor
    @Getter
    static class SimpleWorkflowContext implements WorkflowContext {
        private final RunningTrigger<? extends Serializable> state;
        private Duration nextDelay = Duration.ZERO;
        private boolean canceled = false;

        @Override
        public WorkflowContext delayNextStepBy(Duration duration) {
            nextDelay = duration;
            return this;
        }

        @Override
        public WorkflowContext cancelWorkflow() {
            canceled = true;
            return this;
        }

        public int getExecutionCount() {
            return state.getExecutionCount();
        }
    }
}
