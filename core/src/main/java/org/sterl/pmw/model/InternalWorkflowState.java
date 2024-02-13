package org.sterl.pmw.model;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter(AccessLevel.PACKAGE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class InternalWorkflowState implements WorkflowContext {
    private int currentStepIndex = 0;
    private String lastError;

    private Integer lastFailedStepIndex;
    private Integer lastSuccessfulStepIndex;
    private String lastSuccessfulStepName;
    private String lastFailedStepName;

    private int lastFailedStepRetryCount = 0;
    private int workflowRetryCount = 0;

    private Duration nextStepDelay;

    @Getter
    private WorkflowStatus status = WorkflowStatus.PENDING;
    @Getter
    private Instant startTime;
    @Getter
    private Instant endTime;

    public InternalWorkflowState(Duration nextStepDelay) {
        this.nextStepDelay = nextStepDelay;
    }

    int stepFailed(WorkflowStep<?> s, Exception e) {
        ++workflowRetryCount;
        this.lastFailedStepIndex = currentStepIndex;
        this.lastError = ExceptionUtils.getStackTrace(e);
        this.lastError = this.lastError.substring(0, Math.min(500, this.lastError.length()));
        lastFailedStepName = s.getName();
        return ++this.lastFailedStepRetryCount;
    }
    /**
     * marks the current step as done and returns the next index
     */
    int stepSuccessfullyFinished(WorkflowStep<?> currentStep) {
        lastFailedStepRetryCount = 0;
        lastFailedStepIndex = currentStepIndex;
        currentStepIndex = currentStepIndex + 1;
        lastSuccessfulStepName = currentStep.getName();
        return currentStepIndex;
    }

    Instant workflowStarted() {
        if (startTime == null) startTime = Instant.now();
        this.status = WorkflowStatus.RUNNING;
        return startTime;
    }
    Instant workflowEnded() {
        if (endTime == null) {
            endTime = Instant.now();
            this.status = WorkflowStatus.COMPLETE;
        }
        return endTime;
    }
    public boolean isFirstWorkflowStep() {
        return currentStepIndex == 0 && workflowRetryCount == 0;
    }
    public Duration workflowRunDuration() {
        return Duration.between(startTime, endTime == null ? Instant.now() : endTime);
    }

    @Override
    public int getStepRetryCount() {
        return lastFailedStepRetryCount;
    }
    @Override
    public WorkflowContext delayNextStepBy(Duration duration) {
        nextStepDelay = duration;
        return this;
    }
    @Override
    public WorkflowContext cancelWorkflow() {
        final Instant then = workflowEnded();
        status = WorkflowStatus.CANCELED;
        this.lastError = "Workflow " + WorkflowStatus.CANCELED + " at " + then;
        return this;
    }
    @Override
    public Duration consumeDelay() {
        Duration result;
        if (this.nextStepDelay == null) {
            result = Duration.ZERO;
        } else {
            result = this.nextStepDelay;
        }
        this.nextStepDelay = null;
        return result;
    }

    @Override
    public boolean hasDelay() {
        return nextStepDelay != null && nextStepDelay.toMillis() > 0;
    }
}
