package org.sterl.pmw.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.ToString;

@ToString(of = "id")
@Getter
public abstract class AbstractStepFactory<F extends AbstractStepFactory<F, C, T>, C extends StepHolder<T>, T extends Serializable> {
    protected final C context;
    
    protected String id;
    protected String description;
    protected String connectorLabel;
    protected boolean transactional = true;

    public F id(String value) {
        id = value;
        return (F)this;
    }
    
    public F description(String value) {
        description = value;
        return (F)this;
    }
    
    public F transactional(boolean value) {
        transactional = value;
        return (F)this;
    }
    
    public F connectorLabel(String value) {
        connectorLabel = value;
        return (F)this;
    }

    protected AbstractStepFactory(C context) {
        this.context = context;
    }
}
