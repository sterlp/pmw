package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.spring.persistent_tasks.api.TriggerSearch;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;
import org.sterl.spring.persistent_tasks.history.HistoryService;
import org.sterl.spring.persistent_tasks.test.AsyncAsserts;

class SubWorkflowTest extends AbstractSpringTest {
    
    @TestConfiguration
    static class Config {
        @Bean
        Workflow<AtomicInteger> intChildWorkflow(AsyncAsserts asserts) {
          return Workflow.builder("testTriggerWorkflow-child", () -> new AtomicInteger())
                    .next(s -> asserts.info("child " + s.data().incrementAndGet()))
                    .next(s -> asserts.info("child " + s.data().incrementAndGet()))
                    .build();
        }
        
        @Bean
        Workflow<AtomicInteger> parentChildWorkflow(
                Workflow<AtomicInteger> intChildWorkflow,
                AsyncAsserts asserts) {

          return Workflow.builder("testTriggerWorkflow-parent", () ->  new AtomicInteger())
                    .next(s -> asserts.info("partent " + s.data().incrementAndGet()))
                    .forkWorkflow(intChildWorkflow).build()
                    .next(s -> asserts.info("partent " + s.data().incrementAndGet()))
                    .build();
        }
    }
    
    @Autowired
    private WorkflowService<TaskId<? extends Serializable>> subject;
    @Autowired
    private Workflow<AtomicInteger> intChildWorkflow;
    @Autowired
    private Workflow<AtomicInteger> parentChildWorkflow;
    @Autowired
    private HistoryService historyService;

    @Test
    void testTriggerSubWorkflowInChoose() throws InterruptedException {
        // GIVEN
        Workflow<AtomicInteger> w = Workflow.builder("testTriggerSubWorkflowInChoose", () -> new AtomicInteger(0))
                .next(s -> asserts.info("parent " + s.data().incrementAndGet()))
                .choose(c -> c.intValue() == 1 ? "left" : "right")
                    .ifSelected("left", s -> asserts.info("left " + s.data().incrementAndGet()))
                    .ifTrigger("right", intChildWorkflow).build()
                    .build()
                .build();
        register(w);

        // WHEN
        subject.execute(w, new AtomicInteger(99));
        waitForAllWorkflows();

        // THEN
        asserts.awaitOrdered("parent 100", "child 101", "child 102");
    }

    @Test
    void testTriggerWorkflow() {
        // GIVEN
        // WHEN
        var id = subject.execute(parentChildWorkflow);
        
        // THEN 
        waitForAllWorkflows();
        asserts.awaitValueOnce("partent 1");
        asserts.awaitValueOnce("partent 2");
        // AND sub workflow should run
        asserts.awaitValueOnce("child 2");
        asserts.awaitValueOnce("child 3");
        // AND
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.SUCCESS);
    }

    @Test
    void testTriggerWorkflowDelay() {
        // GIVEN
        Workflow<AtomicInteger> parent = Workflow.builder("testTriggerWorkflowDelay-parent", () ->  new AtomicInteger())
                .next(s -> asserts.info("partent " + s.data().incrementAndGet()))
                .forkWorkflow(intChildWorkflow)
                    .delay(Duration.ofDays(999))
                    .build()
                .next(s -> asserts.info("partent " + s.data().incrementAndGet()))
                .build();
        
        register(parent);
        
        // WHEN
        subject.execute(parent);
        
        // THEN 
        waitForAllWorkflows();
        asserts.awaitValueOnce("partent 1");
        asserts.awaitValueOnce("partent 2");
        // AND sub workflow should not run
        asserts.assertMissing("child 2");
        asserts.assertMissing("child 3");
    }

    @Test
    void testSubWorkflowHasSameCorrelationId() {
        // GIVEN
        // WHEN
        var id = subject.execute(parentChildWorkflow);
        waitForAllWorkflows();
        
        // WHEN 
        var trigger = historyService.searchTriggers(
                TriggerSearch.byCorrelationId(id.value()), Pageable.ofSize(999));
        
        trigger.forEach(t -> {
            System.err.println(t.getData().getCorrelationId() + " - " + t.getKey());
        });
        // THEN 3 parent 2 from the child workflow
        assertThat(trigger.getContent()).hasSize(5);
        // AND
        assertThat(trigger.getContent().stream()
            .filter(s -> s.getKey().getTaskName().startsWith("intChildWorkflow::"))
            .count()).isEqualTo(2);
        
    }
}
