package org.sterl.pmw.spring.config;

import org.sterl.pmw.component.ChainedWorkflowStatusObserver;

public interface PmwBeanCustomizer {

    void customizeWorkflowObserver(ChainedWorkflowStatusObserver observerChain);
}
