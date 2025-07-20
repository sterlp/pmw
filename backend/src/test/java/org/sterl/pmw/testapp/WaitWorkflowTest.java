package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sterl.pmw.WorkflowService;
import org.sterl.pmw.model.RunningWorkflowId;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.testapp.PersistentWorkflowServiceTests.TestWorkflowCtx;
import org.sterl.spring.persistent_tasks.api.TaskId;
import org.sterl.spring.persistent_tasks.api.TriggerStatus;

class WaitWorkflowTest extends AbstractSpringTest {
    
    @Autowired
    private WorkflowService<TaskId<? extends Serializable>> subject;

    @Test
    void testWaitForNextStep() throws Exception {
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
    void testWaitForNextStepCorrectWay() {
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

}
