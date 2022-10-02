package org.sterl.pmw.component;

import java.util.concurrent.Callable;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

/**
 * Executes the workflow until a sleep or an error.
 *
 * @param <T> the state type
 */
public class SimpleWorkflowExecutor <T extends WorkflowState> extends SimpleWorkflowStepExecutor
    implements Callable<Void> {

    private final RunningWorkflowState<T> runningWorkflowState;
    private final WorkflowService<?> workflowService;

    public SimpleWorkflowExecutor(RunningWorkflowState<T> runningWorkflowState,
            WorkflowService<?> workflowService, WorkflowStatusObserver observer) {
        super(observer);
        this.runningWorkflowState = runningWorkflowState;
        this.workflowService = workflowService;
    }

    @Override
    public Void call() throws Exception {

        if (runningWorkflowState.internalState().isFirstWorkflowStep()) {
            observer.workflowStart(getClass(), runningWorkflowState);
        }
        try {
            boolean hasNextStep = true;

            while (hasNextStep && runningWorkflowState.isNextStepReady()) {
                hasNextStep = executeSingleStepIncludingQueuing();
            }

            if (runningWorkflowState.isCanceled()) {
                workflowService.cancel(runningWorkflowState.workflowId());
            }
            if (runningWorkflowState.isFinished()) {
                observer.workflowSuccess(getClass(), runningWorkflowState);
            }
        } catch (Exception e) {
            observer.workflowFailed(getClass(), runningWorkflowState, e);
            throw e;
        }
        return null;
    }

    protected boolean executeSingleStepIncludingQueuing() throws Exception {
        boolean result;
        byte[] originalUserState = SerializationUtil.serialize(runningWorkflowState.userState());
        try {
            // we loop throw all steps as long we have one
            final WorkflowStep<?> nextStep = this.executeNextStep(runningWorkflowState, workflowService);

            if (runningWorkflowState.isNextStepDelayed()) {
                workflowService.runOrQueueNextStep(runningWorkflowState);
                result = false;
            } else {
                result = nextStep != null;
            }

        } catch (WorkflowException.WorkflowFailedDoRetryException e) {
            result = false;
            workflowService.runOrQueueNextStep(new RunningWorkflowState<>(
                    runningWorkflowState.workflowId(),
                    runningWorkflowState.workflow(),
                    SerializationUtil.deserializeWorkflowState(originalUserState),
                    runningWorkflowState.internalState())
                );
        }
        return result;
    }
}