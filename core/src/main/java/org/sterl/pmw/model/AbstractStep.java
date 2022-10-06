package org.sterl.pmw.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractStep <StateType extends WorkflowState> implements WorkflowStep<StateType> {
    @NonNull
    protected final String name;
    protected final String connectorLabel;
    @Getter
    protected int maxRetryCount = 3;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(name=" + name + ")";
    }
}
