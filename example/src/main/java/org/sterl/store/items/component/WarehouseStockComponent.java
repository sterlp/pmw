package org.sterl.store.items.component;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;
import org.sterl.store.warehouse.WarehouseItem;
import org.sterl.store.warehouse.WarehouseService;

import lombok.RequiredArgsConstructor;

@Component
@Transactional // Quartz will not open a trx by default
@RequiredArgsConstructor
public class WarehouseStockComponent {

    private final WarehouseService warehouseService;
    private final ItemRepository itemRepository;

    public int checkWarehouseForNewStock(long itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        List<WarehouseItem> count = warehouseService.checkForNewItemsInStock(item);
        item.setInStock(item.getInStock() + count.size());
        return count.size();
    }
}
