package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface WorkflowChooseFunction<StateType extends Serializable> extends Function<StateType, String> {
    String apply(StateType state);
}
