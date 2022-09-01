package org.sterl.pmw.testapp.item.boundary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.testapp.item.entity.Item;
import org.sterl.pmw.testapp.item.repository.ItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item newItem(String name) {
        var r = itemRepository.save(Item.builder().name(name).build());
        if (r.getName() == null || r.getName().length() <= 2) throw new IllegalArgumentException("Name '" + name + "' to short, transaction rollback!");
        return r;
    }

    public void updateStock(long itemId, int stock) {
        if (stock < 0) throw new IllegalArgumentException("Stock smaller than 0, rollback before save!");
        itemRepository.getReferenceById(itemId).setInStock(stock);
    }
}
