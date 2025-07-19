package org.sterl.pmw.spring.component;

import org.sterl.pmw.model.WorkflowStep;

public class WorkflowHelper {

    public static final String SPLIT = "::";

    public static String stepName(String workflowId, WorkflowStep<?> s) {
        return workflowId + SPLIT + s.getId();
    }
}
