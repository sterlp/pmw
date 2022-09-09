package org.sterl.pmw.model;

import java.util.UUID;

public record WorkflowId(String value) {
    public static WorkflowId newWorkflowId(Workflow<?> w) {
        return new WorkflowId(w.getName() + ":" + UUID.randomUUID().getMostSignificantBits());
    }
    public WorkflowId {
        if (value == null) throw new NullPointerException("WorkflowId value can't be null");
        else if(value.trim().length() == 0) throw new IllegalArgumentException("WorkflowId can't be an empty string.");
    }
}
