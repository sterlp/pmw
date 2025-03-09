package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.sping_tasks.PersistentWorkflowService;
import org.sterl.pmw.testapp.item.boundary.ItemService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

class RunInsTransactionTest extends AbstractSpringTest {
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private PersistentWorkflowService workflowService;

    protected final AsyncAsserts asserts = new AsyncAsserts();

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    static class TestWorkflowState implements Serializable {
        private static final long serialVersionUID = 1L;
        private String itemName;
        private Long itemId;
        private int stock;
    }

    @Test
    void testHappy() {
        // GIVEN
        Workflow<TestWorkflowState> w = Workflow.builder("create-item1", TestWorkflowState::new)
                .next(c -> {
                    var itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.getItemId(), c.getStock());
                })
                .build();
        workflowService.register(w);

        // WHEN
        workflowService.execute(w, TestWorkflowState.builder().itemName("MyName").stock(5).build());

        // THEN
        waitForAllWorkflows();
        assertThat(itemRepository.findByName("MyName")).isNotNull();
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(5);
    }

    @Test
    void testRollbackTransaction() {
        // GIVEN
        final AtomicInteger retries = new AtomicInteger(0);
        Workflow<TestWorkflowState> w = Workflow.builder("create-item2", TestWorkflowState::new)
                .next(c -> c.setItemId(itemService.newItem(c.getItemName()).getId()))
                .next( (c, s) -> {
                    assertThat(c.getItemId()).isNotNull();
                    itemService.updateStock(c.getItemId(), c.getStock());
                    retries.incrementAndGet();
                    // we to a rollback
                    throw new RuntimeException("Nope for item " + c.getItemId() + " retry: " + s.getExecutionCount());
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        final RunningWorkflowId w1 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName1").stock(99).build());
        final RunningWorkflowId w2 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName2").stock(99).build());
        final RunningWorkflowId w3 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName3").stock(99).build());
        // AND
        waitForAllWorkflows();

        // THEN
        assertThat(workflowService.status(w1)).isEqualTo(TriggerStatus.FAILED);
        assertThat(workflowService.status(w2)).isEqualTo(TriggerStatus.FAILED);
        assertThat(workflowService.status(w3)).isEqualTo(TriggerStatus.FAILED);
        // AND
        assertThat(itemRepository.findByName("MyName1").getInStock()).isZero();
        assertThat(itemRepository.findByName("MyName2").getInStock()).isZero();
        assertThat(itemRepository.findByName("MyName3").getInStock()).isZero();
        // AND 3 times normal and after that 3 retries
        assertThat(retries.get()).isEqualTo(12);
    }

    @Test
    void testRollbackOutsideRetry() {

        // GIVEN
        final var name = UUID.randomUUID().toString();
        Workflow<TestWorkflowState> w = Workflow.builder("create-item3", TestWorkflowState::new)
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.getItemId(), c.getStock());
                    if (asserts.info("error") < 2) {
                        throw new RuntimeException("Nope! " + asserts.getCount("error"));
                    }
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        RunningWorkflowId wid = workflowService.execute(w, TestWorkflowState.builder().itemName(name).stock(99).build());

        // THEN
        waitForAllWorkflows();
        assertThat(workflowService.status(wid)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(itemRepository.findByName(name)).isNotNull();
        assertThat(itemRepository.findByName(name).getInStock()).isEqualTo(99);
        assertThat(asserts.getCount("error")).isEqualTo(2);
    }

    /**
     * Same as outside, but with the transaction set to rollback only by the service
     */
    @Test
    void testRollbackInsideRetry() {
        // GIVEN
        Workflow<TestWorkflowState> w = Workflow.builder("create-item4", TestWorkflowState::new)
                .next(c -> {
                    Long itemId = itemService.newItem(c.getItemName()).getId();
                    c.setItemId(itemId);
                })
                .next(c -> {
                    if (asserts.info("error") < 2) {
                        itemService.updateStock(c.getItemId(), -1);
                    } else {
                        itemService.updateStock(c.getItemId(), c.getStock());
                    }
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        RunningWorkflowId wid = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName").stock(99).build());

        // THEN
        waitForAllWorkflows();
        assertThat(workflowService.status(wid)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(99);
        assertThat(asserts.getCount("error")).isEqualTo(2);
    }
}
