package org.sterl.pmw.spring.component;

import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.command.TriggerWorkflowCommand;
import org.sterl.pmw.model.RunningWorkflowId;
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

import com.github.f4b6a3.uuid.UuidCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class WorkflowStepComponent<T extends Serializable> implements TransactionalTask<T> {

    private final WorkflowService<?> workflowService;
    private final PersistentTaskService taskService;
    private final String workflowId;
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
            runOrQueueNextStep(context, nextStep);
        } else {
            log.info("Cancel Workflow={} {} requested in step={}.", workflow, context.state.getKey(), step.getId());
            workflowService.cancel(new RunningWorkflowId(RunningTriggerContextHolder.getCorrelationId()));
        }
    }

    public void runOrQueueNextStep(SimpleWorkflowContext<T> context, WorkflowStep<T> nextStep) {
        var nextTrigger = TriggerBuilder.newTrigger(WorkflowHelper.stepName(workflowId, nextStep), context.data())
                .runAfter(context.getNextDelay())
                .tag(workflowId)
                .correlationId(RunningTriggerContextHolder.getCorrelationId())
                .id(context.getNextTaskId());
        
        if (context.isSuspendNext()) {
            nextTrigger.waitForSignal(
                    OffsetDateTime.now().plus(context.getNextDelay()));
        } else {
            nextTrigger.runAfter(context.getNextDelay());
        }

        taskService.runOrQueue(nextTrigger.build());
    }
    
    void triggerCommands(List<TriggerWorkflowCommand<? extends Serializable>> commands) {
        for (TriggerWorkflowCommand t : commands) {
            log.debug("Workflow={} triggers sub-workflow={} in={}", workflow, t.workflow(), t.delay());
            workflowService.execute(t.workflow(), t.state(), t.delay());
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
    
    @Override
    public boolean isTransactional() {
        return step.isTransactional();
    }

    @RequiredArgsConstructor
    @Getter
    public static class SimpleWorkflowContext<T extends Serializable> implements WorkflowContext<T> {
        private final RunningTrigger<T> state;
        private Duration nextDelay = Duration.ZERO;
        private boolean canceled = false;
        @Setter
        private boolean suspendNext = false;
        private String nextTaskId = UuidCreator.getTimeOrderedEpochFast().toString();
        private List<TriggerWorkflowCommand<? extends Serializable>> commands = new ArrayList<>();

        //@Override
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

        @Override
        public String nextTaskId() {
            return nextTaskId;
        }
    }
}
