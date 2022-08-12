package org.sterl.pmw.model;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractWorkflowContext {
    
    @Setter(AccessLevel.PACKAGE) @Getter
    private InternalWorkflowContext internalWorkflowContext = new InternalWorkflowContext();

    @Getter @Setter(AccessLevel.PACKAGE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InternalWorkflowContext {
        private int currentStepIndex = 0;
        private String lastError;

        private Integer lastFailedStepIndex;
        private Integer lastSuccessfulStepIndex;
        private String lastSuccessfulStepName;
        private String lastFailedStepName;

        private int lastFailedStepRetryCount = 0;
        private int workflowRetryCount = 0;

        private Instant workflowStart;
        private Instant workflowEnd;
        
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
            if (workflowStart == null) workflowStart = Instant.now();
            return workflowStart;
            
        }
        Instant workflowEnded() {
            if (workflowEnd == null) {
                workflowEnd = Instant.now();
            }
            return workflowEnd;
        }
        public boolean isFirstWorkflowStep() {
            return currentStepIndex == 0 && workflowRetryCount == 0;
        }
        public Duration workflowRunDuration() {
            return Duration.between(workflowStart, workflowEnd == null ? Instant.now() : workflowEnd);
        }
        
    }
}
