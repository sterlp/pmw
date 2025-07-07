package org.sterl.pmw.model;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString(of = "name")
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractStep<T extends Serializable> implements WorkflowStep<T> {
    @NonNull
    protected final String name;
    protected final String connectorLabel;
}
