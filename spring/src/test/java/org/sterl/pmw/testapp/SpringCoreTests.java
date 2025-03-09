package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.SimpleWorkflowState;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.sping_tasks.PersistentWorkflowService;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SpringBootTest
public class SpringCoreTests extends AbstractSpringTest {

    @Autowired
    private PersistentWorkflowService subject;
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    protected static class TestWorkflowCtx implements Serializable {
        private static final long serialVersionUID = 1L;
        private int anyValue = 0;
    }

    @Test
    public void testWorkflowServiceIsCreated() {
        assertThat(subject).isNotNull();
    }
    
    @Test
    public void testRegisterWorkflowNoSteps() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("bad-workflow", TestWorkflowCtx::new).build();

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> subject.register(w));
    }

    @Test
    public void testRegisterWorkflow() {
        // GIVEN
        subject.clearAllWorkflows();
        Workflow<TestWorkflowCtx> w = Workflow.builder("any-workflow", TestWorkflowCtx::new)
                .sleep(Duration.ofHours(1))
                .build();
        assertThat(subject.workflowCount()).isZero();

        // WHEN
        subject.register(w);

        // THEN
        assertThat(subject.workflowCount()).isOne();

        // AND
        assertThrows(IllegalArgumentException.class, () -> subject.register(w));
    }

    @Test
    public void testWorkflowStateValue() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);

        final Workflow<Integer> workflow = Workflow.builder("test-workflow", () -> Integer.valueOf(0))
            .next((s, c) -> { return s + 1; })
            .next((s) -> state.set(s))
            .build();
        subject.register(workflow);

        // WHEN
        final RunningWorkflowId id = subject.execute(workflow, Integer.valueOf(10));
        Awaitility.await().until(() -> subject.status(id) == TriggerStatus.SUCCESS);

        // THEN
        assertThat(state.get()).isEqualTo(11);
    }

    @Test
    public void testWorkflowStatus() throws InterruptedException {
        // GIVEN
        Workflow<Duration> w = Workflow.builder("any-workflow", () -> Duration.ZERO)
                .next((s, c) -> {
                    try {
                        Thread.sleep(s.toMillis());
                    } catch (InterruptedException e) {}
                    return 50;
                })
                .next((s, c) -> {
                    try {
                        Thread.sleep(s);
                    } catch (InterruptedException e) {}
                    return null;
                })
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId id = subject.execute(w, Duration.ofMillis(250), Duration.ofMillis(50));
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.WAITING);
        
        // AND wait for the start delay
        Thread.sleep(51);
        var triggered = subject.queueAllWorkflows();

        // THEN
        assertThat(triggered).hasSize(1);
        Awaitility.await().atMost(Duration.ofMillis(500)).until(() -> subject.status(id) == TriggerStatus.RUNNING);
        // AND
        Awaitility.await().atMost(Duration.ofMillis(1500)).until(() -> {
            waitForAllWorkflows();
            return subject.status(id) == TriggerStatus.SUCCESS;
        });
    }
    
    @Test
    public void testCancelWorkflowByService() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("cancel-workflow", SimpleWorkflowState::new)
                .next((s, c) -> { return null; })
                .build();
        
        subject.register(w);
        
        // WHEN
        final RunningWorkflowId id = subject.execute("cancel-workflow", new SimpleWorkflowState(), Duration.ofSeconds(1));
        
        // THEN
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.WAITING);
        
        // WHEN
        subject.cancel(id);
        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.CANCELED);
    }

    @Test
    public void testWorkflowStateIsAvailableInNextStep() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", TestWorkflowCtx::new)
            .next((s) -> s.setAnyValue(s.getAnyValue() + 1))
            .next((s) -> state.set(s.getAnyValue()))
            .build();
        subject.register(w);

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
    public void testNoUserStateUpdateOnException() {
        final AtomicLong state = new AtomicLong(0);

        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", TestWorkflowCtx::new)
                .next((s, c) -> {
                    state.set(s.getAnyValue());
                    s.setAnyValue(99);
                    if (c.getExecutionCount() == 0) throw new RuntimeException("Not now");
                    return s;
                })
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w, new TestWorkflowCtx(77));

        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
        assertThat(state.get()).isEqualTo(77);
    }

    @Test
    public void testWorkflow() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
            .next((s) -> {
                asserts.info("do-first");
            })
            .next((s) -> {
                asserts.info("do-second");
            })
            .choose(s -> {
                asserts.info("choose");
                return "left";
            }).ifSelected("left", (s, c) -> {
                asserts.info("  going left");
                return s;
            }).ifSelected("right", s -> {
                asserts.info("  going right");
            })
            .build()
            .next((s, c) -> {
                asserts.info("finally");
                return null;
            })
            .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN
        waitForAllWorkflows();
        asserts.awaitOrdered("do-first", "do-second", "choose", "  going left", "finally");
    }

    @Test
    public void testChoose() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
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
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN
        waitForAllWorkflows();
        asserts.awaitOrdered("choose 1", "  going right", "choose 2", "  going mid", "finally");
    }

    @Test
    public void testRetry() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next("failing step", (s, c) -> {
                    asserts.info("failing " + c.getExecutionCount());
                    if (c.getExecutionCount() < 3) {
                        throw new IllegalStateException("Not now " + c.getExecutionCount());
                    }
                    return s;
                }).next((s, c) -> asserts.info("done"))
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        subject.register(w);

        // WHEN
        var runningWorkflowId = subject.execute(w);

        // THEN
        asserts.awaitOrdered(() -> waitForAllWorkflows(), "failing 1", "failing 2", "failing 3", "done");
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
    }

    @Test
    public void testFailForever() {
        // GIVEN
        final AtomicInteger failCount = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next("failing step", (s, c) -> {
                    asserts.info("failing " + failCount.incrementAndGet());
                    throw new IllegalStateException("Not now " + failCount.get());
                }).next((s, c) -> asserts.info("done"))
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        subject.register(w);

        // WHEN
        var runningWorkflowId = subject.execute(w);

        // THEN we should use the default 3 times retry
        waitForAllWorkflows();
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.FAILED);
        asserts.awaitOrdered("failing 1", "failing 2", "failing 3", "failing 4");
        asserts.assertMissing("failing 5");
        assertThat(failCount.get()).isEqualTo(4);
    }

    @Test
    public void testWaitForNextStep() throws Exception {
        // GIVEN
        final AtomicLong timeFirstStep = new AtomicLong(0);
        final AtomicLong timeSecondStep = new AtomicLong(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next((s, c) -> {
                    timeFirstStep.set(System.currentTimeMillis());
                    asserts.info("wait");
                    c.delayNextStepBy(Duration.ofMillis(500));
                    return s;
                })
                .next(s -> {
                    timeSecondStep.set(System.currentTimeMillis());
                    asserts.info("done");
                })
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w);
        asserts.awaitOrdered("wait");
        // AND the next one should be delayed!
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.WAITING);

        // WHEN - should still be waiting, as we delayed by 500ms
        Thread.sleep(250);
        waitForAllWorkflows();
        // THEN
        asserts.assertMissing("done");
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.WAITING);

        // WHEN we wait a bit more
        Thread.sleep(250);
        waitForAllWorkflows();
        // THEN the last task should be done too
        asserts.awaitOrdered("wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(500L);
    }

    @Test
    public void testWaitForNextStepCorrectWay() {
        // GIVEN
        final AtomicLong timeFirstStep = new AtomicLong(0);
        final AtomicLong timeSecondStep = new AtomicLong(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next(s -> {
                    asserts.info("wait");
                    timeFirstStep.set(System.currentTimeMillis());
                })
                .sleep(Duration.ofMillis(500))
                .next((s) -> {
                    timeSecondStep.set(System.currentTimeMillis());
                    asserts.info("done");
                })
                .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN
        asserts.awaitOrdered(() -> waitForAllWorkflows(), "wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(500L);
    }

    @Test
    public void testCancelWorkflow() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next(s -> asserts.info("step 1"))
                .next((s, c) -> {
                    asserts.info("step 2");
                    c.cancelWorkflow();
                    return s;
                })
                .next(s -> asserts.info("cancel"))
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w);

        // THEN
        waitForAllWorkflows();
        assertThat(subject.status(runningWorkflowId)).isEqualTo(TriggerStatus.SUCCESS);
        asserts.awaitOrdered("step 1", "step 2");
        asserts.assertMissing("cancel");
    }

    @Test
    public void testTriggerSubWorkflow() throws InterruptedException {
        // GIVEN
        final AtomicInteger stateValue = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> subW = Workflow.builder("subW", TestWorkflowCtx::new)
                .next(s -> stateValue.set(s.getAnyValue() + 1))
                .build();

        Workflow<TestWorkflowCtx> w = Workflow.builder("w", TestWorkflowCtx::new)
                .next(s -> s.setAnyValue(1))
                .trigger(subW, s -> s)
                .build();

        // WHEN
        subject.register(w);
        subject.register(subW);
        subject.execute(w);

        // THEN
        waitForAllWorkflows();
        assertThat(stateValue.get()).isEqualTo(2);
    }
}
