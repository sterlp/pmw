package org.sterl.store.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final Random random = new Random();
    private final StockItemRepository stockItemRepository;

    private final static AtomicLong STOCK_ARRIVALS = new AtomicLong(0);

    public long countStock(long itemId) {
        return stockItemRepository.countItems(itemId);
    }

    public List<WarehouseItem> checkForNewItemsInStock(Item item) {
        int stockCount = random.nextInt(0, 100);

        List<WarehouseItem> result = new ArrayList<>(stockCount);
        for (int i = 0; i < stockCount; i++) {
            result.add(new WarehouseItem(null, item, "NEW_ITEM_" + STOCK_ARRIVALS.incrementAndGet()));
        }
        log.info("Checking warehouse for {}, found {} new items.", item, stockCount);
        return stockItemRepository.saveAll(result);
    }
}
