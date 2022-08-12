package org.sterl.store.items.workflow;

import javax.annotation.PostConstruct;

import org.quartz.JobDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.component.CreateStockComponent;
import org.sterl.store.items.component.UpdateInStockCountComponent;
import org.sterl.store.items.entity.Item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateItemWorkflow {
    
    private final CreateStockComponent createStock;
    private final UpdateInStockCountComponent updateStock;
    private final WorkflowService<JobDetail> workflowService;

    private Workflow<CreateItemWorkflowContext> w;
    

    @PostConstruct
    void createWorkflow() {
        w = Workflow.builder("create-item", () -> CreateItemWorkflowContext.builder().build())
                .next(c -> {
                    c.setInStockCount(createStock.execute(c.getItemId()));
                })
                .next(c -> {
                    updateStock.updateInStockCount(c.getItemId(), c.getInStockCount());
                    log.info("");
                    c.setRetry(c.getRetry() + 1);
                    if (c.getRetry() < 10) throw new IllegalStateException("No " + c.getRetry());
                })
                .choose(c -> {
                    if (c.getInStockCount() > 50) return "large";
                    else return "small";
                }).ifSelected("large", c -> {
                    log.info("Created item {} with a large stock {}", c.getItemId(), c.getInStockCount());
                }).ifSelected("small", c -> {
                    log.info("Created item {} with a small stock", c.getItemId(), c.getInStockCount());
                }).build()
                .build();

        workflowService.register(w);
    }
    
    @Transactional(propagation = Propagation.MANDATORY)
    public String execute(Item item) {
        return workflowService.execute(w, CreateItemWorkflowContext.builder()
                .itemId(item.getId()).build());
    }
}
