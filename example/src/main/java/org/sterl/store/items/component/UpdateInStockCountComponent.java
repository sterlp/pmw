package org.sterl.store.items.component;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional  // Quartz will not open a trx by default
@Component
public class UpdateInStockCountComponent {

    private final ItemRepository itemRepository;
    
    @Transactional
    public void updateInStockCount(long itemId, int stockCount) {
        if (stockCount < 0) throw new IllegalArgumentException("Item " + itemId 
                + " cannot have a smaller stock than 0 but got: " + stockCount);
        itemRepository.getReferenceById(itemId).setInStock(stockCount);
    }
}
