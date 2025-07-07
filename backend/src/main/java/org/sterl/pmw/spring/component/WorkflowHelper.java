package org.sterl.pmw.spring.component;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;

public class WorkflowHelper {

    public static final String SPLIT = "::";

    public static String stepName(Workflow<?> w, WorkflowStep<?> s) {
        return w.getName() + SPLIT + s.getName();
    }
}
