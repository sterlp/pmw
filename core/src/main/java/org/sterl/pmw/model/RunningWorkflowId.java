package org.sterl.pmw.model;

import java.util.UUID;

public record RunningWorkflowId(String value) {
    public static RunningWorkflowId newWorkflowId(Workflow<?> w) {
        return new RunningWorkflowId(w.getName() + "::" + UUID.randomUUID().toString().replace('-', Character.MIN_VALUE));
    }
    public RunningWorkflowId {
        if (value == null) throw new NullPointerException("RunningWorkflowId value can't be null");
        else if(value.trim().length() == 0) throw new IllegalArgumentException("RunningWorkflowId can't be an empty string.");
    }
}
