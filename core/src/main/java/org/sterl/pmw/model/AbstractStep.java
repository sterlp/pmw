package org.sterl.pmw.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString(of = "name")
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractStep <T extends AbstractWorkflowContext> implements WorkflowStep<T> {
    @NonNull
    protected final String name;
    @Getter
    private int maxRetryCount = 3;
}
