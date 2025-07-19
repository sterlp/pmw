package org.sterl.pmw.model;

import com.github.f4b6a3.uuid.UuidCreator;

public record RunningWorkflowId(String value) {
    public static RunningWorkflowId newWorkflowId(Workflow<?> w) {
        return new RunningWorkflowId(UuidCreator.getTimeOrderedEpochFast().toString());
    }
    public RunningWorkflowId {
        if (value == null) throw new NullPointerException("RunningWorkflowId value can't be null");
        else if(value.trim().length() == 0) throw new IllegalArgumentException("RunningWorkflowId can't be an empty string.");
    }
}
