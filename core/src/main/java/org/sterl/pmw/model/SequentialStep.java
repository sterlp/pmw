package org.sterl.pmw.model;

import java.util.Objects;
import java.util.function.Consumer;

import lombok.Getter;

@Getter
public class SequentialStep<T extends WorkflowContext> extends AbstractStep<T> {
    private final Consumer<T> fn;

    SequentialStep(String name, Consumer<T> fn) {
        super(name);
        Objects.requireNonNull(fn, "Function cannot be null.");
        this.fn = fn;
    }

    @Override
    public void apply(T c) {
        fn.accept(c);
    }
}
