package org.sterl.store.items;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.command.TriggerWorkflowCommand;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.entity.Item;
import org.sterl.store.items.repository.ItemRepository;
import org.sterl.store.items.workflow.NewItemArrivedState;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final Workflow<NewItemArrivedState> checkWarehouseWorkflow;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    void init() {

    }
    @Transactional(readOnly = true, timeout = 10)
    public Optional<Item> get(long id) {
        return itemRepository.findById(id);
    }

    public Item createNewItem(Item item) {
        itemRepository.save(item);
        eventPublisher.publishEvent(new TriggerWorkflowCommand<>(
                checkWarehouseWorkflow, 
                NewItemArrivedState.builder().itemId(item.getId()).build(), 
                Duration.ZERO));
        
        log.info("Fired TriggerWorkflowCommand for " + item.getId());
        return item;
    }
}
