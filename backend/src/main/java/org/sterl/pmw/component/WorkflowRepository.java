
package org.sterl.pmw.component;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.sterl.pmw.model.Workflow;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowRepository {

    @Getter
    private final Map<String, Workflow<? extends Serializable>> workflows = new ConcurrentHashMap<>();

    public void clear() {
        workflows.clear();
    }

    public <T extends Serializable> Workflow<? extends Serializable> register(
            String id, Workflow<T> w) {
        log.debug("Registering workflow={} id={}", w, id);
        return workflows.put(id, w);
    }

    public <T extends Serializable> void registerUnique(
            String id, Workflow<T> w) {
        Workflow<? extends Serializable> oldWorkflow = register(id, w);
        if (oldWorkflow != null) {
            throw new IllegalArgumentException("Workflow with the ID "
                    + id + " already registered.");
        }
    }

    public Optional<Workflow<? extends Serializable>> findWorkflow(String id) {
        Workflow<? extends Serializable> w = workflows.get(id);
        return w == null ? Optional.empty() : Optional.of(w);
    }

    public Workflow<? extends Serializable> getWorkflow(String id) {
        Workflow<? extends Serializable> w = workflows.get(id);
        if (w == null) {
            throw new IllegalStateException("No workflow with the ID "
                    + id + " found. Registered " + workflows.keySet());
        }
        return w;
    }
    
    public Optional<String> getWorkflowId(Workflow<?> workflow) {
        return this.workflows.entrySet().stream().filter(e -> e.getValue() == workflow)
                   .map(e -> e.getKey())
                   .findFirst();
    }

    public boolean hasWorkflows() {
        return !workflows.isEmpty();
    }

    public int workflowCount() {
        return workflows.size();
    }
}
