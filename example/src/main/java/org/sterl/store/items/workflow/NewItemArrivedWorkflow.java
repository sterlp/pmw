package org.sterl.store.items.workflow;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.component.DiscountComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.component.WarehouseStockComponent;
import org.sterl.store.warehouse.WarehouseService;

@Configuration
public class NewItemArrivedWorkflow {

    @Bean
    Workflow<NewItemArrivedState> restorePriceWorkflow(DiscountComponent discountComponent) {
        return Workflow.builder("Restore Item Price", () -> NewItemArrivedState.builder().build())
                .next("updatePrice", c -> discountComponent.setPrize(c.data().getItemId(), c.data().getOriginalPrice()))
                .next("sendMail").transactional(false).function(s -> {}).build()
                .build();
    }

    @Bean
    Workflow<NewItemArrivedState> checkWarehouseWorkflow(
            WarehouseService warehouseService,
            UpdateInStockCountComponent updateStock,
            WarehouseStockComponent createStock,
            DiscountComponent discountComponent,
            Workflow<NewItemArrivedState> restorePriceWorkflow) {

        return Workflow.builder("Check Warehouse", () -> NewItemArrivedState.builder().build())
                .next().description("check warehouse for new stock")
                    .function(c -> createStock.checkWarehouseForNewStock(c.data().getItemId()))
                    .build()
                .next("update item stock", c -> {
                    final var s = c.data();
                    final long stockCount = warehouseService.countStock(s.getItemId());
                    updateStock.updateInStockCount(s.getItemId(), stockCount);
                    s.setWarehouseStockCount(stockCount);
                })
                .sleep("wait", "Wait if stock is > 40", (s) -> s.getWarehouseStockCount() > 40 ? Duration.ofMinutes(2) : Duration.ZERO)
                .choose("check stock", s -> {
                        if (s.getWarehouseStockCount() > 40) return "discount-price";
                        else return "buy-new-items";
                    })
                    .ifTrigger("discount-price", restorePriceWorkflow)
                        .description("WarehouseStockCount > 40")
                        .delay(Duration.ofMinutes(2))
                        .function(s -> {
                            var originalPrice = discountComponent.applyDiscount(s.getItemId(), 
                                    s.getWarehouseStockCount());
                            s.setOriginalPrice(originalPrice);
                            return s;
                        }).build()
                    .ifSelected("buy-new-items", c -> {})
                    .build()
                .build();
    }
}
