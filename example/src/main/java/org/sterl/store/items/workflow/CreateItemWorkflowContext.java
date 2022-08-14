package org.sterl.store.items.workflow;

import org.sterl.pmw.model.WorkflowContext;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class CreateItemWorkflowContext implements WorkflowContext {
    private static final long serialVersionUID = 1L;
    private long itemId;  
    private Integer inStockCount;
    @Builder.Default
    private int retry = 0;
}
