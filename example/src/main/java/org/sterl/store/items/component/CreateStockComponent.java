package org.sterl.store.items.component;

import java.util.List;


import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;
import org.sterl.store.warehouse.StockItem;
import org.sterl.store.warehouse.WarehouseService;

import lombok.RequiredArgsConstructor;

@Component
@Transactional // Quartz will not open a trx by default
@RequiredArgsConstructor
public class CreateStockComponent {

    private final WarehouseService warehouseService;
    private final ItemRepository itemRepository;

    
    public int execute(long itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        List<StockItem> count = warehouseService.createStockItems(item);
        return count.size();
    }
}
