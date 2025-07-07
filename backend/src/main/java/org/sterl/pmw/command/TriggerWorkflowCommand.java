package org.sterl.pmw.command;

import java.io.Serializable;
import java.time.Duration;

import org.sterl.pmw.model.Workflow;

public record TriggerWorkflowCommand<T extends Serializable>(
        Workflow<T> workflow,
        T state,
        Duration delay) {
}
