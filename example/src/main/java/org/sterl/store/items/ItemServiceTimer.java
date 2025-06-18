package org.sterl.store.items;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sterl.store.items.entity.Item;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ItemServiceTimer {

    private final ItemService itemService;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Random random = new Random();

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    void newItemArived() {
        var item = new Item();
        item.setName("Item " + counter.incrementAndGet());
        item.setInStock(random.nextLong(1, 999));
        item.setPrice(new BigDecimal(random.nextDouble(0.01, 9999.99)));
        itemService.createNewItem(item);
    }
}
