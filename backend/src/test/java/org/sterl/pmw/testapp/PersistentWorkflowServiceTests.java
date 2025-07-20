package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sterl.pmw.SimpleWorkflowState;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.spring.PersistentWorkflowService;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

import com.github.f4b6a3.uuid.UuidCreator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PersistentWorkflowServiceTests extends AbstractSpringTest {

    @Autowired
    private PersistentWorkflowService subject;

    @Test
    void testWorkflowServiceIsCreated() {
        assertThat(subject).isNotNull();
    }
    
    @Test
    void testRegisterWorkflowNoSteps() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("bad-workflow", TestWorkflowCtx::new).build();

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> register(w));
    }

    @Test
    void testRegisterWorkflow() {
        // GIVEN
        var startCount = subject.workflowCount();
        Workflow<TestWorkflowCtx> w = Workflow.builder("any-workflow", TestWorkflowCtx::new)
                .sleep(Duration.ofHours(1))
                .build();

        // WHEN
        register(w);
        // THEN
        assertThat(subject.workflowCount()).isEqualTo(startCount + 1);

        // AND
        assertThrows(IllegalArgumentException.class, () -> register(w));
        // WHEN
        assertThat(subject.workflowCount()).isEqualTo(startCount + 1);
    }

    @Test
    void testWorkflowStateValue() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);

        final Workflow<TestWorkflowCtx> workflow = Workflow.builder("test-workflow-state", TestWorkflowCtx::new)
            .next(c -> c.data().increment())
            .next(c -> state.set(c.data().getAnyValue()))
            .build();
        register(workflow);

        // WHEN
        var id = subject.execute(workflow, new TestWorkflowCtx(10));
        waitForAllWorkflows();
        // THEN
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(state.get()).isEqualTo(11);
        
        // WHEN
        id = subject.execute(workflow);
        waitForAllWorkflows();
        // THEN
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(state.get()).isEqualTo(1);
        
        // WHEN given state is null -> should build a new one
        id = subject.execute(workflow, null);
        waitForAllWorkflows();
        // THEN
        assertThat(state.get()).isEqualTo(1);
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.SUCCESS);
    }

    @Test
    void testWorkflowStatus() throws InterruptedException {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("testWorkflowStatus", TestWorkflowCtx::new)
                .next(c -> {
                    try {
                        Thread.sleep(c.data().getAnyValue());
                    } catch (InterruptedException e) {}
                    c.data().setAnyValue(50);
                })
                .next(c -> {
                    try {
                        Thread.sleep(c.data().getAnyValue());
                    } catch (InterruptedException e) {}
                })
                .build();
        register(w);

        // WHEN
        final RunningWorkflowId id = subject.execute(w, new TestWorkflowCtx(250), Duration.ofMillis(100));
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.WAITING);
        
        // AND wait for the start delay
        Thread.sleep(101);
        var triggered = persistentTaskTestService.scheduleNextTriggers();

        // THEN
        assertThat(triggered).hasSize(1);
        await().atMost(Duration.ofMillis(500)).until(() -> subject.status(id) == TriggerStatus.RUNNING);
        // AND
        await().atMost(Duration.ofMillis(1500)).until(() -> {
            waitForAllWorkflows();
            return subject.status(id) == TriggerStatus.SUCCESS;
        });
    }
    
    @Test
    void testCancelWorkflowByService() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("cancel-workflow", SimpleWorkflowState::new)
                .next(c -> asserts.add("foo"))
                .build();
        
        register(w);
        
        // WHEN
        final RunningWorkflowId id = subject.execute("cancel-workflow", new SimpleWorkflowState(), Duration.ofSeconds(1));
        
        // THEN
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.WAITING);
        
        // WHEN
        subject.cancel(id);
        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.CANCELED);
        asserts.assertMissing("foo");
    }

    @Test
    void testWorkflowStateIsAvailableInNextStep() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("testWorkflowStateIsAvailableInNextStep", TestWorkflowCtx::new)
            .next((s) -> s.data().increment())
            .next((s) -> state.set(s.data().getAnyValue()))
            .build();
        register(w);

        // WHEN
        final var runningWorkflowId = subject.execute(w, new TestWorkflowCtx(1));

        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(state.get()).isEqualTo(2);
    }

    /**
     * If we run into an error the user state should be the same
     * on the next run.
     */
    @Test
    void testNoUserStateUpdateOnException() {
        final AtomicLong state = new AtomicLong(0);

        Workflow<TestWorkflowCtx> w = Workflow.builder("testNoUserStateUpdateOnException", TestWorkflowCtx::new)
                .next(c -> {
                    state.set(c.data().getAnyValue());
                    c.data().setAnyValue(99);
                    if (c.executionCount() == 0) throw new RuntimeException("Not now");
                })
                .build();
        register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w, new TestWorkflowCtx(77));

        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(state.get()).isEqualTo(77);
    }

    @Test
    void testWorkflow() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow-long", SimpleWorkflowState::new)
            .next((s) -> {
                asserts.info("do-first");
            })
            .next((s) -> {
                asserts.info("do-second");
            })
            .choose(s -> {
                asserts.info("choose");
                return "left";
            }).ifSelected("left", c -> {
                asserts.info("  going left");
            }).ifSelected("right", s -> {
                asserts.info("  going right");
            })
            .build()
            .next(c -> {
                asserts.info("finally");
            })
            .build();
        register(w);

        // WHEN
        subject.execute(w);

        // THEN
        waitForAllWorkflows();
        asserts.awaitOrdered("do-first", "do-second", "choose", "  going left", "finally");
    }

    @Test
    void testChoose() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("testChoose", SimpleWorkflowState::new)
            .choose(s -> {
                    asserts.info("choose 1");
                    return "right";
                })
                .ifSelected("left", s -> {
                    asserts.info("  going left");
                })
                .ifSelected("mid", s -> {
                    asserts.info("  going mid");
                })
                .ifSelected("right", s -> {
                    asserts.info("  going right");
                })
                .build()
            .choose(s -> {
                    asserts.info("choose 2");
                    return "mid";
                })
                .ifSelected("left", s -> {
                    asserts.info("  going left");
                })
                .ifSelected("mid", s -> {
                    asserts.info("  going mid");
                })
                .ifSelected("right", s -> {
                    asserts.info("  going right");
                })
                .build()
            .next(s -> {
                asserts.info("finally");
            })
            .build();
        register(w);

        // WHEN
        subject.execute(w);

        // THEN
        waitForAllWorkflows();
        asserts.awaitOrdered("choose 1", "  going right", "choose 2", "  going mid", "finally");
    }

    @Test
    void testRetry() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("testRetry",
                TestWorkflowCtx::new)
                .next("failing step", c -> {
                    asserts.info("failing " + c.executionCount());
                    if (c.executionCount() < 3) {
                        throw new IllegalStateException("Not now " + c.executionCount());
                    }
                }).next(c -> asserts.info("done"))
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        register(w);

        // WHEN
        var runningWorkflowId = subject.execute(w);

        // THEN
        asserts.awaitOrdered(() -> waitForAllWorkflows(), "failing 1", "failing 2", "failing 3", "done");
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
    }

    @Test
    void testFailForever() {
        // GIVEN
        final AtomicInteger failCount = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("testFailForever",
                TestWorkflowCtx::new)
                .next("failing step", c -> {
                    asserts.info("failing " + failCount.incrementAndGet());
                    throw new IllegalStateException("Not now " + failCount.get());
                }).next(c -> asserts.info("done"))
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        register(w);

        // WHEN
        var runningWorkflowId = subject.execute(w);

        // THEN we should use the default 3 times retry
        waitForAllWorkflows();
        asserts.awaitOrdered("failing 1", "failing 2", "failing 3", "failing 4");
        asserts.assertMissing("failing 5");
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.FAILED);
        assertThat(failCount.get()).isEqualTo(4);
    }

    @Test
    void testCancelWorkflow() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("testCancelWorkflow",
                TestWorkflowCtx::new)
                .next(s -> asserts.info("step 1"))
                .next(c -> {
                    asserts.info("step 2");
                    c.cancelWorkflow();
                })
                .next(s -> asserts.info("cancel"))
                .build();
        register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w);

        // THEN
        waitForAllWorkflows();
        asserts.awaitOrdered("step 1", "step 2");
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.CANCELED);
        asserts.assertMissing("cancel");
    }
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    protected static class TestWorkflowCtx implements Serializable {
        private static final long serialVersionUID = 1L;
        private int anyValue = 0;
        void increment() {
            ++anyValue;
        }
    }
}
