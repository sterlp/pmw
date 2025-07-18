package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.Map;

public interface StepHolder<T extends Serializable> {

    void addStep(WorkflowStep<T> s);
    
    Map<String, WorkflowStep<T>> steps();
    
    String nextStepId();
}
