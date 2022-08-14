package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.boundary.CoreWorkflowExecutionTest;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.boundary.WorkflowService.WorkflowStatus;
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
    
    protected final AsyncAsserts asserts = new AsyncAsserts();
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    static class TestWorkflowContext extends AbstractWorkflowContext {
        private String itemName;
        private Long itemId;
        private int stock;
    }

    @BeforeEach
    void setUp() throws Exception {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(30));
        workflowService.clearAllWorkflows();
        itemRepository.deleteAllInBatch();
        
        asserts.clear();
    }

    @Test
    void testHappy() {
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
    
    @Test
    void testRollbackTransaction() {
        // GIVEN
        final AtomicInteger retries = new AtomicInteger(0);
        Workflow<TestWorkflowContext> w = Workflow.builder("create-item", () -> new TestWorkflowContext())
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    assertThat(c.getItemId()).isNotNull();
                    itemService.updateStock(c.getItemId(), c.getStock());
                    retries.incrementAndGet();
                    throw new RuntimeException("Nope! " + c.getInternalWorkflowContext().getWorkflowRetryCount());
                })
                .build();
        workflowService.register(w);
        
        // WHEN
        String wid = workflowService.execute(w, TestWorkflowContext.builder().itemName("MyName").stock(99).build());
        
        // THEN
        Awaitility.await().until(() -> workflowService.status(wid) == WorkflowStatus.COMPLETE);
        Awaitility.await().until(() -> itemRepository.findByName("MyName") != null);
        assertThat(itemRepository.findByName("MyName").getInStock()).isZero();
        assertThat(retries.get()).isGreaterThan(1);
    }
    
    @Test
    void testRollbackOutsideRetry() {
        // GIVEN
        Workflow<TestWorkflowContext> w = Workflow.builder("create-item", () -> new TestWorkflowContext())
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.getItemId(), c.getStock());
                    if (c.getInternalWorkflowContext().getWorkflowRetryCount() < 2) {
                        asserts.info("error");
                        throw new RuntimeException("Nope! " + c.getInternalWorkflowContext().getWorkflowRetryCount());
                    }
                })
                .build();
        workflowService.register(w);
        
        // WHEN
        String wid = workflowService.execute(w, TestWorkflowContext.builder().itemName("MyName").stock(99).build());
        
        // THEN
        Awaitility.await().until(() -> workflowService.status(wid) == WorkflowStatus.COMPLETE);
        Awaitility.await().until(() -> itemRepository.findByName("MyName") != null);
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(99);
        assertThat(asserts.count("error")).isEqualTo(2);
    }
    
    /**
     * Same as outside, but with the transaction set to rollback only
     */
    @Test
    void testRollbackInsideRetry() {
        // GIVEN
        Workflow<TestWorkflowContext> w = Workflow.builder("create-item", () -> new TestWorkflowContext())
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    if (c.getInternalWorkflowContext().getWorkflowRetryCount() < 2) {
                        asserts.info("error");
                        itemService.updateStock(c.getItemId(), -1);
                    } else {
                        itemService.updateStock(c.getItemId(), c.getStock());
                    }
                })
                .build();
        workflowService.register(w);
        
        // WHEN
        String wid = workflowService.execute(w, TestWorkflowContext.builder().itemName("MyName").stock(99).build());
        
        // THEN
        Awaitility.await().until(() -> workflowService.status(wid) == WorkflowStatus.COMPLETE);
        Awaitility.await().until(() -> itemRepository.findByName("MyName") != null);
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(99);
        assertThat(asserts.count("error")).isEqualTo(2);
    }
}
