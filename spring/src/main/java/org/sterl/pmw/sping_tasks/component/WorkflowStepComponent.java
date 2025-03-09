package org.sterl.pmw.sping_tasks.component;

import java.io.Serializable;
import java.time.Duration;

import org.springframework.context.ApplicationEventPublisher;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.spring.persistent_tasks.PersistentTaskService;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TaskId.TriggerBuilder;
import org.sterl.spring.persistent_tasks.api.event.TriggerTaskCommand;
import org.sterl.spring.persistent_tasks.api.task.RunningTrigger;
import org.sterl.spring.persistent_tasks.api.task.RunningTriggerContextHolder;
import org.sterl.spring.persistent_tasks.api.task.TransactionalTask;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowStepComponent<T extends Serializable, R extends Serializable> implements TransactionalTask<T> {

    private final WorkflowService<?> workflowService;
    private final PersistentTaskService taskService;
    private final Workflow<?> workflow;
    private final WorkflowStep<T, R> step;

    @Override
    public void accept(T state) {
        var context = new SimpleWorkflowContext(RunningTriggerContextHolder.getContext());
        R nextState = step.apply(state, context, workflowService);
        
        var nextStep = workflow.getNextStep(step);
        
        if (!context.canceled && nextStep != null) {
            taskService.runOrQueue(
                TriggerBuilder.newTrigger(
                        workflow.getName() + "::" + nextStep.getName(), nextState)
                    .runAfter(context.nextDelay)
                    .correlationId(RunningTriggerContextHolder.getCorrelationId())
                    .build()
            );
        }
    }
    
    @Override
    public RetryStrategy retryStrategy() {
        return workflow.getRetryStrategy();
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
