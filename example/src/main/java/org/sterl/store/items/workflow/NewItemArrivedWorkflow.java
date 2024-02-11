package org.sterl.store.items.workflow;

import java.time.Duration;

import org.quartz.JobDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
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
    private final WorkflowService<JobDetail> workflowService;

    @Getter
    private Workflow<NewItemArrivedWorkflowState> checkWarehouse;
    @Getter
    private Workflow<NewItemArrivedWorkflowState> restorePriceSubWorkflow;


    @PostConstruct
    void createWorkflow() {
        checkWarehouse = Workflow.builder("check-warehouse", () -> NewItemArrivedWorkflowState.builder().build())
                .next("check warehouse for new stock", s -> createStock.checkWarehouseForNewStock(s.getItemId()))
                .next("update item stock", (s, c) -> {
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);

                    s.setWarehouseStockCount(stockCount);
                })
                .sleep("Wait if stock is > 40", (s) -> s.getWarehouseStockCount() > 40 ? Duration.ofMinutes(2) : Duration.ZERO)
                .choose("check stock", s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "check-warehouse-again";
                    })
                    .ifSelected("discount-price", "> 40", s -> {
                        var originalPrice = discountComponent.applyDiscount(s.getItemId(), s.getWarehouseStockCount());
                        s.setOriginalPrice(originalPrice);

                        workflowService.execute(restorePriceSubWorkflow, s, Duration.ofMinutes(2));
                    })
                    .ifSelected("trigger->restore-item-price", "< 40", s -> this.execute(s.getItemId()))
                    .build()
                .build();

        workflowService.register(checkWarehouse);

        restorePriceSubWorkflow = Workflow.builder("restore-item-price", () -> NewItemArrivedWorkflowState.builder().build())
                .next("set price from workflow state", s -> discountComponent.setPrize(s.getItemId(), s.getOriginalPrice()))
                .build();

        workflowService.register(restorePriceSubWorkflow);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public WorkflowId execute(long itemId) {
        return workflowService.execute(checkWarehouse, NewItemArrivedWorkflowState.builder()
                .itemId(itemId).build());
    }
}
