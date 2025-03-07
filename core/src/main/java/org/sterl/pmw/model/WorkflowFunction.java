package org.sterl.pmw.model;

import java.io.Serializable;

@FunctionalInterface
public interface WorkflowFunction<T extends Serializable, R extends Serializable> {

    R accept(T state, WorkflowContext context);
}
