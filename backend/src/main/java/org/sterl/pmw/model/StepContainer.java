package org.sterl.pmw.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import lombok.Getter;

public class StepContainer<T extends Serializable> {

    @Getter
    private final LinkedHashMap<String, WorkflowStep<T>> steps = new LinkedHashMap<>();
    private final Set<String> usedIds = new HashSet<>();

    public StepContainer<T> next(WorkflowStep<T> s) {
        useId(s.getId());
        steps.put(s.getId(), s);
        return this;
    }
    
    public StepContainer<T> next(String id, WorkflowStep<T> s) {
        useId(id);
        steps.put(id, s);
        return this;
    }
    
    /**
     * Locks an ID for this step collection
     * 
     * @throws IllegalArgumentException if the id is already in use
     */
    public void useId(String id) {
        if (usedIds.contains(id)) {
            throw new IllegalArgumentException("WorkflowStep with ID "
                    + id + " already exists.");
        }
        usedIds.add(id);
    }
}
