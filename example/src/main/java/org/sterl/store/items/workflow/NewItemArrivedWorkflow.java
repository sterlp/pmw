package org.sterl.store.items.workflow;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.quartz.JobDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.boundary.ItemService;
import org.sterl.store.items.component.DiscountComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.component.WarehouseStockComponent;
import org.sterl.store.items.entity.Item;
import org.sterl.store.warehouse.WarehouseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewItemArrivedWorkflow {
    
    private final WarehouseService warehouseService;
    private final DiscountComponent discountComponent;
    private final WarehouseStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<JobDetail> workflowService;

    private Workflow<NewItemArrivedWorkflowState> w;
    private Workflow<NewItemArrivedWorkflowState> restorePriceWorkflow;
    

    @PostConstruct
    void createWorkflow() {
        w = Workflow.builder("check-warehouse", () -> NewItemArrivedWorkflowState.builder().build())
                .next(s -> createStock.checkWarehouseForNewStock(s.getItemId()))
                .next((s, c) -> {
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    
                    s.setWarehouseStockCount(stockCount);
                    
                    // check after a while if we have still so many items in stock
                    if (stockCount > 40) c.delayNextStepBy(Duration.ofMinutes(2));
                })
                .choose(s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "check-warehouse";
                    })
                    .ifSelected("discount-price", s -> {
                        var originalPrice = discountComponent.applyDiscount(s.getItemId(), s.getWarehouseStockCount());
                        s.setOriginalPrice(originalPrice);
                        
                        workflowService.execute(restorePriceWorkflow, s, Duration.ofMinutes(2));
                    })
                    .ifSelected("check-warehouse", s -> this.execute(s.getItemId()))
                    .build()
                .build();

        workflowService.register(w);
        
        restorePriceWorkflow = Workflow.builder("restore-item-price", () -> NewItemArrivedWorkflowState.builder().build())
                .next(s -> discountComponent.setPrize(s.getItemId(), s.getOriginalPrice()))
                .build();
        
        workflowService.register(restorePriceWorkflow);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public String execute(long itemId) {
        return workflowService.execute(w, NewItemArrivedWorkflowState.builder()
                .itemId(itemId).build());
    }
}
