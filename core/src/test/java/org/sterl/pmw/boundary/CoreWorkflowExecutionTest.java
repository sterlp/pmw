package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public abstract class CoreWorkflowExecutionTest {

    protected final AsyncAsserts asserts = new AsyncAsserts();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    protected static class TestWorkflowCtx implements WorkflowState {
        private static final long serialVersionUID = 1L;
        private int anyValue = 0;
    }

    protected WorkflowService<?> subject;

    @BeforeEach
    protected void setUp() throws Exception {
        asserts.clear();
        subject = new InMemoryWorkflowService();
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
        Workflow<TestWorkflowCtx> w = Workflow.builder("any-workflow", () ->  new TestWorkflowCtx())
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

        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", () ->  new TestWorkflowCtx())
            .next((s) -> state.set(s.getAnyValue()))
            .build();
        subject.register(w);

        // WHEN
        final String id = subject.execute(w, new TestWorkflowCtx(10));
        Awaitility.await().until(() -> subject.status(id) == WorkflowStatus.COMPLETE);

        // THEN
        assertThat(state.get()).isEqualTo(10);
    }
    
    @Test
    public void testWorkflowStatus() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("any-workflow", () -> new SimpleWorkflowState())
                .next(s -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                })
                .next(s -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                })
                .build();
        subject.register(w);
        
        // WHEN
        final String id = subject.execute(w, new SimpleWorkflowState(), Duration.ofMillis(250));
        
        // THEN
        assertThat(subject.status(id)).isEqualTo(WorkflowStatus.SLEEPING);
        // AND
        Awaitility.await().pollInterval(Duration.ofMillis(25)) .until(() -> subject.status(id) == WorkflowStatus.RUNNING);
        // AND
        Awaitility.await().until(() -> subject.status(id) == WorkflowStatus.COMPLETE);
    }

    @Test
    public void testWorkflowStateIsAvailableInNextStep() {
        // GIVEN
        final AtomicInteger state = new AtomicInteger(0);
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", () ->  new TestWorkflowCtx())
            .next((s) -> s.setAnyValue(s.getAnyValue() + 1))
            .next((s) -> state.set(s.getAnyValue()))
            .build();
        subject.register(w);

        // WHEN
        final String id = subject.execute(w, new TestWorkflowCtx(1));
        Awaitility.await().until(() -> subject.status(id) == WorkflowStatus.COMPLETE);

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

        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                () ->  new TestWorkflowCtx(77))
                .next((s, c) -> {
                    state.set(s.getAnyValue());
                    s.setAnyValue(99);
                    if (c.getStepRetryCount() == 0) throw new RuntimeException("Not now");
                })
                .build();
        subject.register(w);

        // WHEN
        final String workflowId = subject.execute(w);

        // THEN
        Awaitility.await().until(() -> subject.status(workflowId) == WorkflowStatus.COMPLETE);
        assertThat(state.get()).isEqualTo(77);

    }
    
    @Test
    public void testWorkflow() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
            .next((s, c) -> {
                asserts.info("do-first");
            })
            .next((s, c) -> {
                asserts.info("do-second");
            })
            .choose(s -> {
                asserts.info("choose");
                return "left";
            }).ifSelected("left", (s, c) -> {
                asserts.info("  going left");
            }).ifSelected("right", (s, c) -> {
                asserts.info("  going right");
            })
            .build()
            .next((s, c) -> {
                asserts.info("finally");
            })
            .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN
        asserts.awaitOrdered("do-first", "do-second", "choose", "  going left", "finally");

    }

    @Test
    public void testRightFirst() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow",
                () ->  new SimpleWorkflowState())
            .choose(s -> {
                asserts.info("choose");
                return "right";
            }).ifSelected("left", (s, c) -> {
                asserts.info("  going left");
            }).ifSelected("right", (s, c) -> {
                asserts.info("  going right");
            })
            .build()
            .next((s, c) -> {
                asserts.info("finally");
            })
            .build();
        subject.register(w);

        // WHEN
        subject.execute(w, new SimpleWorkflowState());

        // THEN
        asserts.awaitOrdered("choose", "  going right", "finally");
    }

    @Test
    public void testRetry() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow",
                () ->  new TestWorkflowCtx())
                .next("failing step", (s, c) -> {
                    asserts.info("failing " + c.getStepRetryCount());
                    if (c.getStepRetryCount() < 2) {
                        throw new IllegalStateException("Not now " + c.getStepRetryCount());
                    }
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
                () ->  new TestWorkflowCtx())
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
                () ->  new TestWorkflowCtx())
                .next((s, c) -> {
                    asserts.info("wait");
                    c.delayNextStepBy(Duration.ofSeconds(1));
                    timeFirstStep.set(System.currentTimeMillis());
                })
                .next((s) -> {
                    timeSecondStep.set(System.currentTimeMillis());
                    asserts.info("done");
                })
                .build();
        subject.register(w);

        // WHEN
        final String workflowId = subject.execute(w);

        // THEN
        Awaitility.await().until(() -> subject.status(workflowId) == WorkflowStatus.SLEEPING);
        asserts.awaitOrdered("wait", "done");
        assertThat(timeSecondStep.get() - timeFirstStep.get()).isGreaterThan(1000L);
    }
}
