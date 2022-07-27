package org.sterl.store.items.boundary;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;
import org.sterl.store.items.workflow.CreateItemWorkflow;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final CreateItemWorkflow createItemWorkflow;
    
    @PostConstruct
    void init() {
        
    }
    @Transactional(readOnly = true, timeout = 10)
    public Optional<Item> get(long id) {
        return itemRepository.findById(id);
    }
    public Item createNewItem(Item item) {
        itemRepository.save(item);
        itemRepository.flush();
        
        createItemWorkflow.execute(item);
        
        return item;
    }
    
    
}
