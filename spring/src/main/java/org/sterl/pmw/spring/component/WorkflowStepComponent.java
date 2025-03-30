package org.sterl.pmw.spring.component;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.command.TriggerWorkflowCommand;
import org.sterl.pmw.model.WaitStep;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowContext;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.spring.persistent_tasks.PersistentTaskService;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TaskId.TriggerBuilder;
import org.sterl.spring.persistent_tasks.api.task.RunningTrigger;
import org.sterl.spring.persistent_tasks.api.task.RunningTriggerContextHolder;
import org.sterl.spring.persistent_tasks.api.task.TransactionalTask;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WorkflowStepComponent<T extends Serializable> implements TransactionalTask<T> {

    private final WorkflowService<?> workflowService;
    private final PersistentTaskService taskService;
    private final Workflow<T> workflow;
    private final WorkflowStep<T> step;

    @Override
    public void accept(T state) {
        var context = new SimpleWorkflowContext<T>(
            (RunningTrigger<T>)RunningTriggerContextHolder.getContext()
        );
        step.apply(context);
        
        var nextStep = selectNextStep(context, step);

        triggerCommands(context.commands);

        if (nextStep == null) return; // done


        if (!context.canceled) {
            var nextTrigger = TriggerBuilder.newTrigger(
                    workflow.getName() + "::" + nextStep.getName(), context.data())
                    .runAfter(context.getNextDelay())
                    .correlationId(RunningTriggerContextHolder.getCorrelationId())
                    .build();
            taskService.runOrQueue(nextTrigger);
        } else {
            //var key = taskService.queue(nextTrigger);
            //taskService.cancel
        }
    }
    
    void triggerCommands(List<TriggerWorkflowCommand<? extends Serializable>> commands) {
        for (TriggerWorkflowCommand<? extends Serializable> t : commands) {
            log.debug("Workflow={} triggers sub-workflow={} in={}", workflow.getName(), t.workflow().getName(), t.delay());
            workflowService.execute(t.workflow().getName(), t.state(), t.delay());
        }
    }
    
    WorkflowStep<T> selectNextStep(SimpleWorkflowContext<T> c, WorkflowStep<T> currentStep) {
        var nextStep = workflow.getNextStep(currentStep);

        if (nextStep instanceof WaitStep<T> waitFor) {
            waitFor.apply(c);
            nextStep = selectNextStep(c, waitFor);
        }
        return nextStep;
    }
    
    @Override
    public RetryStrategy retryStrategy() {
        return workflow.getRetryStrategy();
    }

    @RequiredArgsConstructor
    @Getter
    static class SimpleWorkflowContext<T extends Serializable> implements WorkflowContext<T> {
        private final RunningTrigger<T> state;
        private Duration nextDelay = Duration.ZERO;
        private boolean canceled = false;
        private List<TriggerWorkflowCommand<? extends Serializable>> commands = new ArrayList<>();

        @Override
        public void delayNextStepBy(Duration duration) {
            nextDelay = duration;
        }

        @Override
        public void cancelWorkflow() {
            canceled = true;
        }

        public int executionCount() {
            return state.getExecutionCount();
        }

        @Override
        public T data() {
            return state.getData();
        }

        @Override
        public <R extends Serializable> void addCommand(TriggerWorkflowCommand<R> command) {
            commands.add(command);
        }
    }
}
