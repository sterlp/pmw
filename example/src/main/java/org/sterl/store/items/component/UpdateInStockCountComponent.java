package org.sterl.store.items.component;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional  // Quartz will not open a trx by default
@Component
public class UpdateInStockCountComponent {

    private final ItemRepository itemRepository;
    
    @Transactional
    public void updateInStockCount(long itemId, long stockCount) {
        final Item item = itemRepository.getReferenceById(itemId);
        item.setInStock(stockCount);
    }
}
