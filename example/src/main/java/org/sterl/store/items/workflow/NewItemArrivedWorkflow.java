package org.sterl.store.items.workflow;

import java.io.Serializable;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.store.items.component.DiscountComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.component.WarehouseStockComponent;
import org.sterl.store.warehouse.WarehouseService;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewItemArrivedWorkflow {

    private final WarehouseService warehouseService;
    private final DiscountComponent discountComponent;
    private final WarehouseStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<TaskId<? extends Serializable>> workflowService;

    @Getter
    private Workflow<NewItemArrivedWorkflowState> checkWarehouse;
    @Getter
    private Workflow<NewItemArrivedWorkflowState> restorePriceSubWorkflow;


    @PostConstruct
    void createWorkflow() {
        checkWarehouse = Workflow.builder("check-warehouse", () -> NewItemArrivedWorkflowState.builder().build())
                .next("check warehouse for new stock", c -> createStock.checkWarehouseForNewStock(c.data().getItemId()))
                .next("update item stock", c -> {
                    final var s = c.data();
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    s.setWarehouseStockCount(stockCount);
                })
                .sleep("Wait if stock is > 40", (s) -> s.getWarehouseStockCount() > 40 ? Duration.ofMinutes(2) : Duration.ZERO)
                .choose("check stock", s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "check-warehouse-again";
                    })
                    .ifSelected("discount-price", "> 40", c -> {
                        final var s = c.data();
                        var originalPrice = discountComponent.applyDiscount(s.getItemId(), s.getWarehouseStockCount());
                        s.setOriginalPrice(originalPrice);

                        workflowService.execute(restorePriceSubWorkflow, s, Duration.ofMinutes(2));
                    })
                    .ifSelected("check-warehouse-again", "< 40", c -> this.execute(c.data().getItemId()))
                    .build()
                .build();

        workflowService.register(checkWarehouse);

        restorePriceSubWorkflow = Workflow.builder("restore-item-price", () -> NewItemArrivedWorkflowState.builder().build())
                .next("set price from workflow state", c -> discountComponent.setPrize(c.data().getItemId(), c.data().getOriginalPrice()))
                .build();

        workflowService.register(restorePriceSubWorkflow);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public WorkflowId execute(long itemId) {
        return workflowService.execute(
                checkWarehouse, 
                NewItemArrivedWorkflowState.builder().itemId(itemId).build()
            );
    }
}
