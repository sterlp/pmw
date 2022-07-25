package org.sterl.store.items;

import javax.annotation.PostConstruct;

import org.quartz.JobDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.workflow.CreateItemWorkflowContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Transactional
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final WorkflowService<JobDetail> workflowService;
    
    @PostConstruct
    void init() {
        
    }
    
    public Item createNewItem(Item item) {
        itemRepository.save(item);
        itemRepository.flush();
        workflowService.execute("create-item", CreateItemWorkflowContext.builder()
                .itemId(item.getId()).build());
        
        System.err.println(item);
        return item;
    }
    
    @Transactional
    public void updateInStockCount(long itemId, int stockCount) {
        itemRepository.getReferenceById(itemId).setInStock(stockCount);
    }
}
