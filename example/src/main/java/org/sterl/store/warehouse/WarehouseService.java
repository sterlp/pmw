package org.sterl.store.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.sterl.store.items.Item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final Random random = new Random();
    private final StockItemRepository stockItemRepository;
    
    public List<StockItem> createStockItems(Item item) {
        int stockCount = random.nextInt(1, 100);
        log.info("Creating {} stock items for {}", stockCount, item);
        List<StockItem> result = new ArrayList<>(stockCount);
        for (int i = 0; i < stockCount; i++) {
            result.add(new StockItem(null, item, "B_" + random.nextLong()));
        }
        log.info("Created {} stock items for {}", result.size(), item.getId());
        return stockItemRepository.saveAll(result);
    }
}
