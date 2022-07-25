package org.sterl.store.items.config;

import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.store.items.CreateStockComponent;
import org.sterl.store.items.ItemService;
import org.sterl.store.items.workflow.CreateItemWorkflowContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ItemWorkflowConfig {
    
    @Autowired
    private CreateStockComponent createStock;
    @Autowired
    private ItemService itemService;
    @Autowired
    private WorkflowService<JobDetail> workflowService;

    @Autowired
    void createWorkflow() {
        Workflow<CreateItemWorkflowContext> w = Workflow.builder("create-item", () -> CreateItemWorkflowContext.builder().build())
                .next(c -> {
                    c.setInStockCount(createStock.execute(c.getItemId()));
                })
                .next(c -> itemService.updateInStockCount(c.getItemId(), c.getInStockCount()))
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
}
