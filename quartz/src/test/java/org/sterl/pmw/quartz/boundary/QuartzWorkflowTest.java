package org.sterl.pmw.quartz.boundary;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.SimpleWorkflowContext;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.quartz.job.PwmQuartzJobFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class QuartzWorkflowTest {
    
    WorkflowRepository workflowRepository;
    QuartzWorkflowService subject;
    Scheduler scheduler;
    
    @BeforeEach
    void setUp() throws Exception {
        DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
        scheduler = DirectSchedulerFactory.getInstance().getScheduler();
        workflowRepository = new WorkflowRepository();
        subject = new QuartzWorkflowService(scheduler, workflowRepository);
        scheduler.setJobFactory(new PwmQuartzJobFactory(
                new SimpleWorkflowStepStrategy(), workflowRepository, null));
        
        scheduler.start();

    }

    @AfterEach
    void tearDown() throws SchedulerException {
        scheduler.shutdown();
    }
    
    @Test
    void testWorkflow() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch left = new CountDownLatch(1);
        
        Workflow<SimpleWorkflowContext> w = new Workflow<SimpleWorkflowContext>("test-workflow")
            .next(c -> {
                log.info("do-first");
            })
            .next(c -> {
                log.info("do-second");
            })
            .choose(c -> {
                log.info("choose");
                return "left";
            }).ifSelected("left", c -> {
                log.info("  going left");
            }).ifSelected("right", c -> {
                log.info("  going right");
            })
            .end()
            .next(c -> {
                log.info("finally");
                latch.countDown();
            });
        
        subject.register(w);
        subject.execute(w, SimpleWorkflowContext.newContextFor(w));
        
        left.await();
        latch.await();
    }
    
    @Test
    void testRightFirst() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Workflow<SimpleWorkflowContext> w = new Workflow<SimpleWorkflowContext>("test-workflow")
            .choose(c -> {
                log.info("choose");
                return "right";
            }).ifSelected("left", c -> {
                log.info("  going left");
            }).ifSelected("right", c -> {
                log.info("  going right");
            })
            .end()
            .next(c -> {
                latch.countDown();
                log.info("finally");
            });
        
        subject.register(w);
        
        subject.execute(w, SimpleWorkflowContext.newContextFor(w));
        
        latch.await();
    }

}
