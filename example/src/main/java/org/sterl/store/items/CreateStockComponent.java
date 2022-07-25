package org.sterl.store.items;

import java.util.List;


import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.warehouse.StockItem;
import org.sterl.store.warehouse.WarehouseService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CreateStockComponent {

    private final WarehouseService warehouseService;
    private final ItemRepository itemRepository;

    @Transactional
    public int execute(long itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        List<StockItem> count = warehouseService.createStockItems(item);
        return count.size();
    }
}
