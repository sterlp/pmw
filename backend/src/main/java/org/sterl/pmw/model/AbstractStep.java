package org.sterl.pmw.model;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractStep<T extends Serializable> implements WorkflowStep<T> {
    @NonNull
    protected final String  id;
    protected final String  description;
    protected final String  connectorLabel;
    protected final boolean transactional;

    @Override
    public String toString() {
        return super.getClass().getSimpleName() + "[id=" + id + "]";
    }
}
