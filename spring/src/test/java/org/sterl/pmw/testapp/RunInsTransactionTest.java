package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.sping_tasks.PersistentWorkflowService;
import org.sterl.pmw.testapp.item.boundary.ItemService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;
import org.sterl.spring.persistent_tasks.test.AsyncAsserts;

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
        Workflow<TestWorkflowState> w = Workflow.builder("testHappy", TestWorkflowState::new)
                .next(c -> {
                    var itemId = itemService.newItem(c.state().getItemName()).getId();
                    c.state().setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.state().getItemId(), c.state().getStock());
                })
                .build();
        workflowService.register(w);

        // WHEN
        workflowService.execute(w, TestWorkflowState.builder().itemName("MyName").stock(5).build());
        waitForAllWorkflows();

        // THEN
        assertThat(itemRepository.findByName("MyName")).isNotNull();
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(5);
    }

    @Test
    void testRollbackTransaction() {
        // GIVEN
        final AtomicInteger retries = new AtomicInteger(0);
        Workflow<TestWorkflowState> w = Workflow.builder("testRollbackTransaction", TestWorkflowState::new)
                .next(c -> c.state().setItemId(itemService.newItem(c.state().getItemName()).getId()))
                .next("fail-step", c -> {
                    assertThat(c.state().getItemId()).isNotNull();
                    itemService.updateStock(c.state().getItemId(), c.state().getStock());
                    retries.incrementAndGet();
                    // we to a rollback
                    throw new RuntimeException("Nope for item " + c.state().getItemId() + " retry: " + c.executionCount());
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        final RunningWorkflowId w1 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName1").stock(99).build());
        final RunningWorkflowId w2 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName2").stock(77).build());
        final RunningWorkflowId w3 = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName3").stock(55).build());
        waitForAllWorkflows();

        // THEN
        assertThat(schedulerService.getRunning()).isEmpty();
        assertThat(schedulerService.hasRunningTriggers()).isFalse();
        // AND
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

    @RepeatedTest(15)
    void testRollbackOutsideRetry() {
        // GIVEN
        final var name = UUID.randomUUID().toString();
        Workflow<TestWorkflowState> w = Workflow.builder("testRollbackOutsideRetry", TestWorkflowState::new)
                .next(c -> {
                    Long itemId = itemService.newItem(c.state().getItemName()).getId();
                    c.state().setItemId(itemId);
                })
                .next(c -> {
                    itemService.updateStock(c.state().getItemId(), c.state().getStock());
                    if (asserts.info("error") < 2) {
                        throw new RuntimeException("Nope! " + asserts.getCount("error"));
                    }
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        RunningWorkflowId wid = workflowService.execute(w, TestWorkflowState.builder().itemName(name).stock(99).build());
        waitForAllWorkflows();

        // THEN
        assertThat(schedulerService.getRunning().size()).isZero();
        assertThat(schedulerService.hasRunningTriggers()).isFalse();

        assertThat(workflowService.status(wid)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(itemRepository.findByName(name)).isNotNull();
        assertThat(itemRepository.findByName(name).getInStock()).isEqualTo(99);
        assertThat(asserts.getCount("error")).isEqualTo(2);
    }

    /**
     * Same as outside, but with the transaction set to rollback only by the service
     */
    @RepeatedTest(15)
    void testRollbackInsideRetry() throws InterruptedException {
        // GIVEN
        asserts.clear();
        Workflow<TestWorkflowState> w = Workflow.builder("testRollbackInsideRetry", TestWorkflowState::new)
                .next(c -> {
                    Long itemId = itemService.newItem(c.state().getItemName()).getId();
                    c.state().setItemId(itemId);
                    asserts.add("testRollbackInsideRetry->createdItem");
                })
                .next(c -> {
                    if (asserts.info("testRollbackInsideRetry->error") < 2) {
                        itemService.updateStock(c.state().getItemId(), -1);
                    } else {
                        asserts.add("testRollbackInsideRetry->success");
                        itemService.updateStock(c.state().getItemId(), c.state().getStock());
                    }
                })
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        workflowService.register(w);

        // WHEN
        RunningWorkflowId wid = workflowService.execute(w, TestWorkflowState.builder().itemName("MyName").stock(99).build());
        waitForAllWorkflows();
        waitForAllWorkflows(); // TODO

        // THEN

        assertThat(schedulerService.getRunning()).isEmpty();
        assertThat(schedulerService.hasRunningTriggers()).isFalse();

        assertThat(workflowService.status(wid)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(itemRepository.findByName("MyName").getInStock()).isEqualTo(99);
        assertThat(asserts.getCount("testRollbackInsideRetry->error")).isEqualTo(2);
    }
}
