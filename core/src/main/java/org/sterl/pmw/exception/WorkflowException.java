package org.sterl.pmw.exception;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;

import lombok.Getter;

public abstract class WorkflowException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    @Getter
    private final Workflow<?> workflow;
    @Getter
    private final WorkflowStep<?, ?> failedStep;

    static final String buildDefaultErrorMessage(Workflow<?> workflow, WorkflowStep<?, ?> failedStep, Exception cause) {
        return "Workflow=" + workflow.getName()
            + " failed in step=" + failedStep.getName()
            + " message=" + cause.getMessage();

    }

    public WorkflowException(Workflow<?> workflow, WorkflowStep<?, ?> failedStep, Exception cause) {
        super(buildDefaultErrorMessage(workflow, failedStep, cause), cause);
        this.workflow = workflow;
        this.failedStep = failedStep;
    }

    public static class WorkflowFailedNoRetryException extends WorkflowException {
        private static final long serialVersionUID = 1L;
        @Getter
        private final int tryCount;

        public WorkflowFailedNoRetryException(Workflow<?> workflow, WorkflowStep<?, ?> failedStep, Exception cause, int tryCount) {
            super(workflow, failedStep, cause);
            this.tryCount = tryCount;
        }
    }
    public static class WorkflowFailedDoRetryException extends WorkflowException {
        private static final long serialVersionUID = 1L;
        @Getter
        private final int tryCount;

        public WorkflowFailedDoRetryException(Workflow<?> workflow, WorkflowStep<?, ?> failedStep, Exception cause, int tryCount) {
            super(workflow, failedStep, cause);
            this.tryCount = tryCount;
        }
    }
}
