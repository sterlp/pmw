package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.SimpleWorkflowState;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.sping_tasks.PersistentWorkflowService;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;
import org.sterl.spring.persistent_tasks.task.repository.TaskRepository;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SpringBootTest
public class SpringCoreTests {

    @Autowired
    private TriggerService triggerService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private PersistentWorkflowService subject;
    
    private final AsyncAsserts asserts = new AsyncAsserts();

    @BeforeEach
    protected void setUp() throws Exception {
        taskRepository.clear();
        triggerService.deleteAll();
        subject.clearAllWorkflows();
        asserts.clear();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    protected static class TestWorkflowCtx implements Serializable {
        private static final long serialVersionUID = 1L;
        private int anyValue = 0;
    }

    @AfterEach
    protected void tearDown() throws Exception {
        asserts.clear();
        subject.clearAllWorkflows();
    }

    @Test
    public void testWorkflowServiceIsCreated() {
        assertThat(subject).isNotNull();
    }

    @Test
    public void testRegisterWorkflow() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("any-workflow", TestWorkflowCtx::new)
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
    public void testWorkflowStatus() {
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
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId id = subject.execute(w, Duration.ofMillis(50), Duration.ofMillis(250));

        // THEN
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.WAITING);
        // AND
        Awaitility.await().pollInterval(Duration.ofMillis(25)) .until(() -> subject.status(id) == TriggerStatus.RUNNING);
        // AND
        Awaitility.await().until(() -> subject.status(id) == TriggerStatus.SUCCESS);
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
        assertThat(subject.status(id)).isEqualTo(TriggerStatus.SUCCESS);
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
        final RunningWorkflowId id = subject.execute(w, new TestWorkflowCtx(1));
        Awaitility.await().until(() -> subject.status(id) == TriggerStatus.SUCCESS);

        // THEN
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
        final RunningWorkflowId runningWorkflowId = subject.execute(w);

        // THEN
        Awaitility.await().until(() -> subject.status(runningWorkflowId) == TriggerStatus.SUCCESS);
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
        asserts.awaitOrdered("choose 1", "  going right", "choose 2", "  going mid", "finally");
    }

    @Test
    public void testRetry() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next("failing step", (s, c) -> {
                    asserts.info("failing " + c.getExecutionCount());
                    if (c.getExecutionCount() < 2) {
                        throw new IllegalStateException("Not now " + c.getExecutionCount());
                    }
                    return s;
                }).next((s, c) -> asserts.info("done"))
                .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN
        asserts.awaitOrdered("failing 0", "failing 1", "failing 2", "done");
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
                .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN we should use the default 3 times retry
        asserts.awaitOrdered("failing 1", "failing 2", "failing 3");
        assertThat(failCount.get()).isEqualTo(3);
    }

    @Test
    public void testWaitForNextStep() {
        // GIVEN
        final AtomicLong timeFirstStep = new AtomicLong(0);
        final AtomicLong timeSecondStep = new AtomicLong(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                TestWorkflowCtx::new)
                .next((s, c) -> {
                    asserts.info("wait");
                    c.delayNextStepBy(Duration.ofSeconds(1));
                    timeFirstStep.set(System.currentTimeMillis());
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

        // THEN
        Awaitility.await().until(() -> subject.status(runningWorkflowId) == TriggerStatus.WAITING);
        asserts.awaitOrdered("wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(999L);
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
                .sleep(Duration.ofSeconds(1))
                .next((s) -> {
                    timeSecondStep.set(System.currentTimeMillis());
                    asserts.info("done");
                })
                .build();
        subject.register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w);

        // THEN
        Awaitility.await().until(() -> subject.status(runningWorkflowId) == TriggerStatus.WAITING);
        asserts.awaitOrdered("wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(1000L);
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
        Awaitility.await().until(() -> subject.status(runningWorkflowId) == TriggerStatus.SUCCESS);
        asserts.awaitOrdered("step 1", "step 2");
        asserts.assertMissing("cancel");
    }

    @Test
    public void testTriggerWorkflow() throws InterruptedException {
        // GIVEN
        final AtomicInteger stateValue = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        Workflow<TestWorkflowCtx> subW = Workflow.builder("subW", TestWorkflowCtx::new)
                .next(s -> stateValue.set(s.getAnyValue()))
                .next(s -> latch.countDown())
                .build();

        Workflow<TestWorkflowCtx> w = Workflow.builder("w", TestWorkflowCtx::new)
                .next(s -> s.setAnyValue(2))
                .trigger(subW, s -> s)
                .build();

        // WHEN
        subject.register(w);
        subject.register(subW);
        subject.execute(w);

        // THEN
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(stateValue.get()).isEqualTo(2);
    }
}
