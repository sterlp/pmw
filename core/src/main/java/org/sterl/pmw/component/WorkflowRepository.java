
package org.sterl.pmw.component;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.sterl.pmw.model.Workflow;

public class WorkflowRepository {

    private final Map<String, Workflow<? extends Serializable>> workflows = new ConcurrentHashMap<>();

    public void clear() {
        workflows.clear();
    }

    public <T extends Serializable> Workflow<? extends Serializable> register(Workflow<T> w) {
        return workflows.put(w.getName(), w);
    }

    public <T extends Serializable> void registerUnique(Workflow<T> w) {
        Workflow<? extends Serializable> oldWorkflow = register(w);
        if (oldWorkflow != null) {
            throw new IllegalArgumentException("Workflow with the name "
                    + w.getName() + " already registered.");
        }
    }
    public Optional<Workflow<? extends Serializable>> findWorkflow(String name) {
        Workflow<? extends Serializable> w = workflows.get(name);
        return w == null ? Optional.empty() : Optional.of(w);
    }
    public Workflow<? extends Serializable> getWorkflow(String name) {
        Workflow<? extends Serializable> w = workflows.get(name);
        if (w == null) {
            throw new IllegalStateException("No workflow with the name "
                    + name + " found. Registered " + workflows.keySet());
        }
        return w;
    }
    public Set<String> getWorkflowNames() {
        return workflows.keySet();
    }

    public boolean hasWorkflows() {
        return !workflows.isEmpty();
    }

    public int workflowCount() {
        return workflows.size();
    }
}
