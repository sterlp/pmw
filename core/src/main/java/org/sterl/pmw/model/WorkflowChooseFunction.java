package org.sterl.pmw.model;

import java.io.Serializable;

@FunctionalInterface
public interface WorkflowChooseFunction<StateType extends Serializable> {
    String apply(StateType state);
}
