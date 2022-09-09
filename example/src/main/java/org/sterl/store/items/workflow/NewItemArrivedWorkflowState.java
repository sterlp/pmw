package org.sterl.store.items.workflow;

import java.math.BigDecimal;

import org.sterl.pmw.model.WorkflowState;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class NewItemArrivedWorkflowState implements WorkflowState {
    private static final long serialVersionUID = 1L;
    private long itemId;
    private Long warehouseStockCount;
    @Builder.Default
    private long retry = 0;
    private BigDecimal originalPrice;
}
