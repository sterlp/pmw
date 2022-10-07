package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.component.ChainedWorkflowStatusObserver;
import org.sterl.pmw.component.LoggingWorkflowStatusObserver;
import org.sterl.pmw.component.WorkflowStatusObserver;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;

public class WorkflowStatusOberserverTest {

    protected TestWorkflowObserver subject = new TestWorkflowObserver();
    protected WorkflowService<?> ws = new InMemoryWorkflowService(subject);

    @BeforeEach
    protected void setUp() {
        subject = new TestWorkflowObserver();
        ws = new InMemoryWorkflowService(new ChainedWorkflowStatusObserver(subject, new LoggingWorkflowStatusObserver()));
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
        Awaitility.await().until(() -> ws.status(id) == WorkflowStatus.COMPLETE);

        // THEN
        assertThat(subject.events).containsExactly(
                "created workflow w1",
                "start workflow w1",
                "start step s1",
                "success step s1",
                "success workflow w1");
    }
    
    @Test
    public void testObserveError() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("w1")
            .next("s1", (s) -> {throw new RuntimeException("not today");})
            .build();
        ws.register(w);

        // WHEN
        final WorkflowId id = ws.execute(w, new SimpleWorkflowState());
        Awaitility.await().until(() -> ws.status(id) == WorkflowStatus.COMPLETE);

        // THEN
        assertThat(subject.events).containsExactly(
                "created workflow w1",
                "start workflow w1",
                "start step s1",
                "failed retry step s1",
                "start step s1",
                "failed retry step s1",
                "start step s1",
                "failed step s1",
                "failed workflow w1");
    }
    
    
    
    public static class TestWorkflowObserver implements WorkflowStatusObserver {
        public final List<String> events = new ArrayList<>();
        public TestWorkflowObserver() {}

        @Override
        public <T extends WorkflowState> void workdlowCreated(Class<?> triggerClass, WorkflowId workflowId,
                Workflow<T> workflow, T userState) {
            events.add("created workflow " + workflow.getName());
        }
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
}
