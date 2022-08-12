package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.AbstractWorkflowContext;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.testapp.item.boundary.ItemService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SpringBootTest
class RunInsTransactionTest {
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private WorkflowService<JobDetail> workflowService;
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    static class TestWorkflowContext extends AbstractWorkflowContext {
        private String itemName;
        private Long itemId;
        private int stock;
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void test() {
        // GIVEN
        Workflow<TestWorkflowContext> w = Workflow.builder("create-item", () -> new TestWorkflowContext())
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.getItemId(), c.getStock());
                })
                .build();
        workflowService.register(w);
        
        // WHEN
        workflowService.execute(w, TestWorkflowContext.builder().itemName("MyName").stock(5).build());
        
        // THEN
        Awaitility.await().until(() -> itemRepository.findByName("MyName") != null);
        Awaitility.await().until(() -> itemRepository.findByName("MyName").getInStock() == 5);
    }

}
