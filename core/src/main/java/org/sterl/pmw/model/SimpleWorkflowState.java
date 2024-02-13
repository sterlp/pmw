package org.sterl.pmw.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class SimpleWorkflowState implements WorkflowState {
    private static final long serialVersionUID = 1L;
    private Map<String, Object> state = new HashMap<>();

    public Object get(String name) {
        return state.get(name);
    }
}
