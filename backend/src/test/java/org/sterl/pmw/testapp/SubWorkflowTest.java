package org.sterl.pmw.testapp;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.spring.persistent_tasks.api.TaskId;
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
    }
    
    @Autowired
    private WorkflowService<TaskId<? extends Serializable>> subject;
    @Autowired
    Workflow<AtomicInteger> intChildWorkflow;
    
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
        Workflow<AtomicInteger> parent = Workflow.builder("testTriggerWorkflow-parent", () ->  new AtomicInteger())
                .next(s -> asserts.info("partent " + s.data().incrementAndGet()))
                .forkWorkflow(intChildWorkflow)
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
        // AND sub workflow should run
        asserts.awaitValueOnce("child 2");
        asserts.awaitValueOnce("child 3");
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
}
