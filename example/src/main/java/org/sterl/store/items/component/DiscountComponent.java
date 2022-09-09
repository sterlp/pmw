package org.sterl.store.items.component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional(propagation = Propagation.MANDATORY)
@RequiredArgsConstructor
public class DiscountComponent {

    private final ItemRepository itemRepository;

    public BigDecimal applyDiscount(long id, long stockCount) {
        final Optional<Item> item = itemRepository.findById(id);
        if (item.isPresent()) {

            final BigDecimal discount = new BigDecimal(  Math.min(60, stockCount) / 100.0, new MathContext(3));
            var oldPrice = item.get().applyDiscount(discount);
            log.info("new discount for {} of {}%, oldPrice={} newPrice={}", id, discount, oldPrice, item.get().getPrice());
            return oldPrice;
        }
        return null;
    }

    public void setPrize(long id, BigDecimal price) {
        final Optional<Item> item = itemRepository.findById(id);
        if (item.isPresent()) {
            item.get().setPrice(price);
            log.info("Setting new price {} for {}", price, id);
        }
    }
}
