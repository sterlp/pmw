package org.sterl.pmw;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class SimpleWorkflowState implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Serializable> state = new HashMap<>();
}
