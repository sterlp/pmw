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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PersistentWorkflowServiceTests extends AbstractSpringTest {

    @Autowired
    private PersistentWorkflowService subject;
    
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    protected static class TestWorkflowCtx implements Serializable {
        private static final long serialVersionUID = 1L;
        private int anyValue = 0;
        public void increment() {
            ++anyValue;
        }
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
        assertThrows(IllegalArgumentException.class, () -> register(w));
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
        register(w);

        // THEN
        assertThat(subject.workflowCount()).isOne();

        // AND
        assertThrows(IllegalArgumentException.class, () -> register(w));
    }

    @Test
    public void testWorkflowStateValue() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);

        final Workflow<TestWorkflowCtx> workflow = Workflow.builder("test-workflow", TestWorkflowCtx::new)
            .next(c -> c.data().increment())
            .next(c -> state.set(c.data().getAnyValue()))
            .build();
        register(workflow);

        // WHEN
        final RunningWorkflowId id = subject.execute(workflow, new TestWorkflowCtx(10));
        await().until(() -> subject.status(id) == TriggerStatus.SUCCESS);

        // THEN
        assertThat(state.get()).isEqualTo(11);
    }

    @Test
    public void testWorkflowStatus() throws InterruptedException {
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
    public void testCancelWorkflowByService() {
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
    public void testWorkflowStateIsAvailableInNextStep() {
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
    public void testNoUserStateUpdateOnException() {
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
    public void testWorkflow() {
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
    public void testChoose() {
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
    public void testRetry() {
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
    public void testFailForever() {
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
    public void testWaitForNextStep() throws Exception {
        // GIVEN
        final AtomicLong timeFirstStep = new AtomicLong(0);
        final AtomicLong timeSecondStep = new AtomicLong(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("testWaitForNextStep",
                TestWorkflowCtx::new)
                .next(c -> {
                    timeFirstStep.set(System.currentTimeMillis());
                    asserts.info("wait");
                    c.delayNextStepBy(Duration.ofMillis(500));
                })
                .next(s -> {
                    timeSecondStep.set(System.currentTimeMillis());
                    asserts.info("done");
                })
                .build();
        register(w);

        // WHEN
        final RunningWorkflowId runningWorkflowId = subject.execute(w);
        schedulerService.triggerNextTasks();

        // AND the next one should be delayed!
        await().atMost(Duration.ofMillis(500)).until(() -> subject.status(runningWorkflowId) == TriggerStatus.WAITING);
        asserts.assertValue("wait");
        asserts.assertMissing("done");

        // WHEN we wait a bit more, for the task to be due
        Thread.sleep(501);
        schedulerService.triggerNextTasks();
        
        // THEN the last task should be done too
        asserts.awaitOrdered("wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(500L);
    }

    @Test
    public void testWaitForNextStepCorrectWay() {
        // GIVEN
        final AtomicLong timeFirstStep = new AtomicLong(0);
        final AtomicLong timeSecondStep = new AtomicLong(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("testWaitForNextStepCorrectWay",
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
        register(w);

        // WHEN
        subject.execute(w);

        // THEN
        asserts.awaitOrdered(() -> schedulerService.triggerNextTasks(), "wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(500L);
    }

    @Test
    public void testCancelWorkflow() {
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

    @Test
    public void testTriggerSubWorkflow() throws InterruptedException {
        // GIVEN
        final AtomicInteger stateValue = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> subW = Workflow.builder("subW", TestWorkflowCtx::new)
                .next(s -> stateValue.set(s.data().getAnyValue() + 1))
                .build();
        register(subW);

        Workflow<TestWorkflowCtx> w = Workflow.builder("w", TestWorkflowCtx::new)
                .next(s -> s.data().setAnyValue(1))
                .trigger(subW, s -> s)
                .build();
        register(w);

        // WHEN
        subject.execute(w);
        waitForAllWorkflows();

        // THEN
        assertThat(stateValue.get()).isEqualTo(2);
    }
    
    @Test
    void testTriggerWorkflow() {
     // GIVEN
        Workflow<Integer> child = Workflow.builder("testTriggerWorkflow-child", () ->  Integer.SIZE)
                .next(s -> asserts.info("child 1"))
                .next(s -> asserts.info("child 2"))
                .build();

        Workflow<SimpleWorkflowState> parent = Workflow.builder("testTriggerWorkflow-parent", () ->  new SimpleWorkflowState())
                .next(s -> asserts.info("partent 1"))
                .trigger(child).function(s -> 1).id("myCoolId").build()
                .next(s -> asserts.info("partent 2"))
                .build();
        
        register(child);
        register(parent);
        
        // WHEN
        subject.execute(parent);
        
        // THEN 
        waitForAllWorkflows();
        asserts.awaitValueOnce("partent 1");
        asserts.awaitValueOnce("partent 2");
        asserts.awaitValueOnce("child 1");
        asserts.awaitValueOnce("child 2");
    }
    
    void register(Workflow<?> w) {
        subject.register(w.getName(), w);
    }
}
