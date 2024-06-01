package org.sterl.pmw.spring.model;

import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;

public record WorkflowTrigger<T extends WorkflowState>(WorkflowId workflow, T state) {

}
