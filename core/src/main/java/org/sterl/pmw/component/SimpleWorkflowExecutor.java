package org.sterl.pmw.component;

import java.util.concurrent.Callable;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

/**
 * Executes the workflow until a sleep or an error.
 * 
 * @param <T> the state type
 */
@RequiredArgsConstructor
public class SimpleWorkflowExecutor <T extends WorkflowState> extends SimpleWorkflowStepExecutor
    implements Callable<Void> {

    private final WorkflowId workflowId;
    private final RunningWorkflowState<T> runningWorkflowState;
    private final WorkflowService<?> workflowService;
    
    @Override
    public Void call() throws Exception {
    
        boolean hasNextStep = true;
        while (hasNextStep && runningWorkflowState.isNextStepReady()) {
            hasNextStep = executeSingleStepIncludingQueuing();
        }

        if (runningWorkflowState.isCanceled()) workflowService.cancel(workflowId);
        return null;
    }

    protected boolean executeSingleStepIncludingQueuing() throws Exception {
        boolean result;
        byte[] originalState = SerializationUtil.serialize(runningWorkflowState.userState());
        try {
            // we loop throw all steps as long we have one
            WorkflowStep<?> nexStep = this.executeNextStep(runningWorkflowState, workflowService);

            if (runningWorkflowState.isNextStepDelayed()) {
                workflowService.runOrQueueNextStep(workflowId, runningWorkflowState);
                result = false;
            } else {
                result = nexStep != null;
            }

        } catch (WorkflowException.WorkflowFailedDoRetryException e) {
            result = false;
            workflowService.runOrQueueNextStep(workflowId, new RunningWorkflowState<>(
                    runningWorkflowState.workflow(),
                    SerializationUtil.deserializeWorkflowState(originalState),
                    runningWorkflowState.internalState())
                );
        }
        return result;
    }
}