package org.sterl.pmw.model;

import java.util.function.Consumer;

@FunctionalInterface
public interface WorkflowFunction<StateType extends WorkflowState> {

    static <StateType extends WorkflowState> WorkflowFunction<StateType> of(Consumer<StateType> consumer) {
        return (s, c) -> consumer.accept(s);
    }

    void accept(StateType state, WorkflowContext context);
}
