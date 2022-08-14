package org.sterl.pmw.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class SimpleWorkflowContext implements WorkflowContext {

    private Map<String, Object> state = new HashMap<>();
}
