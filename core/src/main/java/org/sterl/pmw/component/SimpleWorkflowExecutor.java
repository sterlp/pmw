package org.sterl.pmw.component;

import java.util.concurrent.Callable;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

/**
 * Executes the workflow until a sleep or an error.
 *
 * @param <T> the state type
 */
@RequiredArgsConstructor
public class SimpleWorkflowExecutor <T extends WorkflowState> implements Callable<Void> {

    private final RunningWorkflowState<T> runningWorkflowState;
    private final WorkflowService<?> workflowService;
    private final WorkflowStatusObserver observer;
    private final SimpleWorkflowStepExecutor stepExecutor;

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
            workflowService.fail(runningWorkflowState.workflowId());
            throw e;
        }
        return null;
    }

    protected boolean executeSingleStepIncludingQueuing() throws Exception {
        boolean result;
        byte[] originalUserState = SerializationUtil.serialize(runningWorkflowState.userState());
        try {
            // we loop throw all steps as long we have one
            final WorkflowStep<?> nextStep = stepExecutor.executeNextStep(runningWorkflowState, workflowService);

            if (runningWorkflowState.isNextStepDelayed()) {
                queueNextExecution(runningWorkflowState);
                result = false;
            } else {
                result = nextStep != null;
            }

        } catch (WorkflowException.WorkflowFailedDoRetryException e) {
            result = false;
            queueNextExecution(new RunningWorkflowState<>(
                    runningWorkflowState.workflowId(),
                    runningWorkflowState.workflow(),
                    SerializationUtil.deserializeWorkflowState(originalUserState),
                    runningWorkflowState.internalState())
                );
        }
        return result;
    }

    /**
     * Called in case of an error and retry or a delay detected.
     */
    protected void queueNextExecution(RunningWorkflowState<T> state) {
        workflowService.queueStepForExecution(state);
    }
}