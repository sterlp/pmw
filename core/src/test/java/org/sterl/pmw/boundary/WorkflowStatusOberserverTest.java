package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.component.WorkflowStatusObserver;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public class WorkflowStatusOberserverTest {

    class TestWorkflowObserver implements WorkflowStatusObserver {

        public final List<String> events = new ArrayList<>();
        @Override
        public void workflowStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
            events.add("start workflow " + runningWorkflow.workflow().getName());
        }
        @Override
        public void workflowSuspended(Class<?> triggerClass, Instant until,
                RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
            events.add("suspended workflow " + runningWorkflow.workflow().getName());
        }

        @Override
        public void workflowSuccess(Class<?> triggerClass,
                RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
            events.add("success workflow " + runningWorkflow.workflow().getName());
        }

        @Override
        public void workflowFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
                Exception error) {
            events.add("failed workflow " + runningWorkflow.workflow().getName());
        }

        @Override
        public void stepStart(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
            events.add("start step " + runningWorkflow.getCurrentStep().getName());
        }

        @Override
        public void stepSuccess(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow) {
            events.add("success step " + runningWorkflow.getCurrentStep().getName());
        }

        @Override
        public void stepFailed(Class<?> triggerClass, RunningWorkflowState<? extends WorkflowState> runningWorkflow,
                Exception error) {
            events.add("failed step " + runningWorkflow.getCurrentStep().getName());
        }

        @Override
        public void stepFailedRetry(Class<?> triggerClass,
                RunningWorkflowState<? extends WorkflowState> runningWorkflow, Exception error) {
            events.add("failed retry step " + runningWorkflow.getCurrentStep().getName());
        }

    }


    protected TestWorkflowObserver subject = new TestWorkflowObserver();
    protected InMemoryWorkflowService ws = new InMemoryWorkflowService(subject);

    @BeforeEach
    protected void setUp() throws Exception {
        subject = new TestWorkflowObserver();
        ws = new InMemoryWorkflowService(subject);
    }

    @Test
    public void testWorkflowStateIsAvailableInNextStep() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("w1")
            .next("s1", (s) -> {})
            .build();
        ws.register(w);

        // WHEN
        final WorkflowId id = ws.execute(w, new SimpleWorkflowState());

        // THEN
        Awaitility.await().until(() -> ws.status(id) == WorkflowStatus.COMPLETE);
        assertThat(subject.events).containsExactly(
                "start workflow w1",
                "start step s1",
                "success step s1",
                "success workflow w1");
    }
}
