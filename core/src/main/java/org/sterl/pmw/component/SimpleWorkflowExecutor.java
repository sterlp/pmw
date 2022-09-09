package org.sterl.pmw.component;

import java.util.concurrent.Callable;

import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.exception.WorkflowException;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleWorkflowExecutor <T extends WorkflowState> extends SimpleWorkflowStepExecutor
    implements Callable<Void> {

    private final WorkflowId workflowId;
    private final RunningWorkflowState<T> runningWorkflowState;
    private final WorkflowService<?> workflowService;
    
    @Override
    public Void call() throws Exception {
    
        byte[] originalState = SerializationUtil.serialize(runningWorkflowState.userState());
        try {
            // we loop throw all steps as long we have one
            WorkflowStep<?> nexStep = this.executeNextStep(runningWorkflowState, workflowService);
            while (nexStep != null && runningWorkflowState.isNotCanceled()) {
                if (runningWorkflowState.hasDelay()) {
                    workflowService.runOrQueueNextStep(workflowId, runningWorkflowState);
                    break;
                } else {
                    originalState = SerializationUtil.serialize(runningWorkflowState.userState());
                    nexStep = this.executeNextStep(runningWorkflowState, workflowService);
                }
            }
    
            if (nexStep == null || runningWorkflowState.isCanceled()) workflowService.cancel(workflowId);
    
        } catch (WorkflowException.WorkflowFailedDoRetryException e) {
            workflowService.runOrQueueNextStep(workflowId, new RunningWorkflowState<>(
                    runningWorkflowState.workflow(),
                    SerializationUtil.deserializeWorkflowState(originalState),
                    runningWorkflowState.internalState())
                );
        }
        return null;
    }
}