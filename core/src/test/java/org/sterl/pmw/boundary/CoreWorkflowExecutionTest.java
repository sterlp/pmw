package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.model.SimpleWorkflowContext;
import org.sterl.pmw.model.Workflow;

import lombok.Getter;
import lombok.Setter;

public abstract class CoreWorkflowExecutionTest {

    protected final AsyncAsserts asserts = new AsyncAsserts();

    @Getter @Setter
    protected static class TestWorkflowCtx extends SimpleWorkflowContext {
        private int tryCount = 0;
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
    void testWorkflowServiceCreateds() {
        assertThat(subject).isNotNull();
    }
    
    @Test
    void testWorkflow() {
        // GIVEN
        Workflow<SimpleWorkflowContext> w = Workflow.builder("test-workflow", 
                () ->  new SimpleWorkflowContext())
            .next(c -> {
                asserts.info("do-first");
            })
            .next(c -> {
                asserts.info("do-second");
            })
            .choose(c -> {
                asserts.info("choose");
                return "left";
            }).ifSelected("left", c -> {
                asserts.info("  going left");
            }).ifSelected("right", c -> {
                asserts.info("  going right");
            })
            .build()
            .next(c -> {
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
    void testRightFirst() {
        // GIVEN
        Workflow<SimpleWorkflowContext> w = Workflow.builder("test-workflow", 
                () ->  new SimpleWorkflowContext())
            .choose(c -> {
                asserts.info("choose");
                return "right";
            }).ifSelected("left", c -> {
                asserts.info("  going left");
            }).ifSelected("right", c -> {
                asserts.info("  going right");
            })
            .build()
            .next(c -> {
                asserts.info("finally");
            })
            .build();
        subject.register(w);
        
        // WHEN
        subject.execute(w, new SimpleWorkflowContext());
        
        // THEN
        asserts.awaitOrdered("choose", "  going right", "finally");
    }
    
    @Test
    void testRetry() {
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", 
                () ->  new TestWorkflowCtx())
                .next("failing step", c -> {
                    asserts.info("failing " + c.getTryCount());
                    if (c.getTryCount() < 2) {
                        c.setTryCount(c.getTryCount() + 1);
                        throw new IllegalStateException("Not now " + c.getTryCount());
                    }
                }).next(c -> asserts.info("done"))
                .build();
        subject.register(w);
        
        // WHEN
        subject.execute(w);

        // THEN
        asserts.awaitOrdered("failing 0", "failing 1", "failing 2", "done");
    }
    
    @Test
    void testFailForever() {
        final AtomicInteger failCount = new AtomicInteger(0);
        // GIVEN
        Workflow<TestWorkflowCtx> w = Workflow.builder("test-workflow", 
                () ->  new TestWorkflowCtx())
                .next("failing step", c -> {
                    asserts.info("failing " + failCount.incrementAndGet());
                    c.setTryCount(c.getTryCount() + 1);
                    throw new IllegalStateException("Not now " + c.getTryCount());
                }).next(c -> asserts.info("done"))
                .build();
        subject.register(w);

        // WHEN
        subject.execute(w);

        // THEN we should use the default 3 times retry
        asserts.awaitOrdered("failing 1", "failing 2", "failing 3");
        assertThat(failCount.get()).isEqualTo(3);
    }
    
}
