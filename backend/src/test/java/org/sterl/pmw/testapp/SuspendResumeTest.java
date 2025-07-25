package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.Workflow;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

class SuspendResumeTest extends AbstractSpringTest {
    
    @Autowired
    private WorkflowService<TaskId<? extends Serializable>> subject;
    
    @Test
    void testNextTaskIdIsSet() throws InterruptedException {
        // GIVEN
        var idRef = new AtomicReference<String>(null);
        Workflow<AtomicInteger> w = Workflow.builder("testNextTaskIdIsSet", () -> new AtomicInteger(0))
                .next(s -> {
                    asserts.info("s " + s.data().incrementAndGet());
                    idRef.set(s.nextTaskId());
                })
                .build();
        register(w);
        
        // WHEN
        subject.execute(w, new AtomicInteger(5));
        waitForAllWorkflows();

        // THEN
        assertThat(idRef.get()).isNotNull();
    }
    
    @Test
    void testSuspendAndResume() throws InterruptedException {
        // GIVEN
        var idRef = new AtomicReference<String>();
        Workflow<AtomicInteger> w = Workflow.builder("testSuspendAndResume", () -> new AtomicInteger(0))
                .next(s -> {
                    asserts.info("s " + s.data().incrementAndGet());
                    idRef.set(s.nextTaskId());
                })
                .await(Duration.ofMinutes(1))
                .next(s -> asserts.info("parent " + s.data().incrementAndGet()))
                .build();
        register(w);
        
        var w2 = subject.execute(w, new AtomicInteger(1));
        waitForAllWorkflows();
        var w1 = subject.execute(w, new AtomicInteger(5));
        assertThat(w1).isNotEqualTo(w2);
        waitForAllWorkflows(); // the ID of W1 should be the last we see

        asserts.awaitValue("s 6");
        asserts.awaitValue("s 2");
        asserts.assertMissing("parent 11");
        assertThat(asserts.getCount()).isEqualTo(2);
        
        assertThat(subject.status(w1)).isEqualTo(TriggerStatus.AWAITING_SIGNAL);
        assertThat(subject.status(w2)).isEqualTo(TriggerStatus.AWAITING_SIGNAL);

        // WHEN
        var resumed = subject.<AtomicInteger>resume(idRef.get(), s -> {s.set(10); return s;});
        assertThat(resumed).withFailMessage("The resumed ID should be found!").isTrue();
        waitForAllWorkflows();

        // THEN
        asserts.awaitValue("s 6", "parent 11");
        // AND the other task should still wait
        assertThat(asserts.getCount()).isEqualTo(3);
        assertThat(subject.status(w2)).isEqualTo(TriggerStatus.AWAITING_SIGNAL);
        assertThat(subject.status(w1)).isEqualTo(TriggerStatus.SUCCESS);
    }
}
