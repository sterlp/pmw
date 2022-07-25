package org.sterl.store.items.workflow;

import org.sterl.pmw.model.AbstractWorkflowContext;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class CreateItemWorkflowContext extends AbstractWorkflowContext {
    
    private long itemId;  
    private Integer inStockCount;

}
