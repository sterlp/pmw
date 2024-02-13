
package org.sterl.pmw.component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;

public class WorkflowRepository {

    private final Map<String, Workflow<? extends WorkflowState>> workflows = new ConcurrentHashMap<>();

    public void clear() {
        workflows.clear();
    }

    public String register(Workflow<?> w) {
        workflows.put(w.getName(), w);
        return w.getName();
    }

    public void registerUnique(Workflow<WorkflowState> w) {
        Workflow<? extends WorkflowState> oldWorkflow = workflows.put(w.getName(), w);
        if (oldWorkflow != null) {
            throw new IllegalArgumentException("Workflow with the name "
                    + w.getName() + " already registered.");
        }
    }
    public Optional<Workflow<? extends WorkflowState>> findWorkflow(String name) {
        Workflow<? extends WorkflowState> w = workflows.get(name);
        return w == null ? Optional.empty() : Optional.of(w);
    }
    public Workflow<? extends WorkflowState> getWorkflow(String name) {
        Workflow<? extends WorkflowState> w = workflows.get(name);
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
