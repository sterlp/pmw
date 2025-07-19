package org.sterl.store.items.workflow;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class NewItemArrivedState implements Serializable {
    private static final long serialVersionUID = 1L;
    private long itemId;
    private Long warehouseStockCount;
    private BigDecimal originalPrice;
}
