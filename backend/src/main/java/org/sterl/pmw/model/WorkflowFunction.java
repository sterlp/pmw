package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.function.Consumer;

@FunctionalInterface
public interface WorkflowFunction<T extends Serializable> extends Consumer<WorkflowContext<T>> {
}
