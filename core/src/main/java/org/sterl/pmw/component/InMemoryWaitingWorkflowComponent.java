package org.sterl.pmw.component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.sterl.pmw.boundary.WorkflowService;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InMemoryWaitingWorkflowComponent {

    public static record WaitingWorkflow<T extends WorkflowState>(Instant until, RunningWorkflowState<T> runningWorkflowState) {}

    private Map<WorkflowId, WaitingWorkflow<?>> waitingWorkflows = new ConcurrentHashMap<>();

    private final WorkflowService<?> workflowService;
    private final int sleepTimeBetweenLoops;
    private final ExecutorService loopThread;
    private final AtomicBoolean looping = new AtomicBoolean(false);

    public InMemoryWaitingWorkflowComponent(WorkflowService<?> workflowService) {
        this(workflowService, 250, Executors.newFixedThreadPool(1,
                new BasicThreadFactory.Builder()
                    .namingPattern("Workflow-Sleep")
                    .priority(Thread.NORM_PRIORITY - 2)
                    .build()));
    }

    public boolean isWaiting(WorkflowId workflowId) {
        return this.waitingWorkflows.containsKey(workflowId);
    }

    public <T extends WorkflowState> WaitingWorkflow<?> addWaitingWorkflow(WorkflowId id,
            RunningWorkflowState<T> runningWorkflowState, Duration duration) {
        return addWaitingWorkflow(id, new WaitingWorkflow<>(Instant.now().plus(duration), runningWorkflowState));
    }

    /**
     * Queues the given workflow.
     *
     * @return any {@link Workflow} which is registered with the same {@link WorkflowId}, this shouldn't be possible
     */
    public <T extends WorkflowState> WaitingWorkflow<?> addWaitingWorkflow(WorkflowId id, WaitingWorkflow<T> waitingWorkflow) {
        final WaitingWorkflow<?> old = waitingWorkflows.put(id, waitingWorkflow);
        log.debug("Workflow {} is now waiting until {}.", id, waitingWorkflow.until());
        start();
        return old;
    }

    private class WaitingWorkflowChecker implements Runnable {
        @Override
        public void run() {
            try {
                while (waitingWorkflows.size() > 0 && looping.get()) {
                    final Instant now = Instant.now();
                    for (Entry<WorkflowId, WaitingWorkflow<?>> w : new HashSet<>(waitingWorkflows.entrySet())) {
                        if (now.isAfter(w.getValue().until)) {
                            workflowService.runOrQueueNextStep(w.getKey(), w.getValue().runningWorkflowState());
                            waitingWorkflows.remove(w.getKey());
                        }
                    }
                    try {
                        Thread.sleep(sleepTimeBetweenLoops);
                    } catch (InterruptedException e) {
                        if (Thread.interrupted()) break;
                    }
                }
            } finally {
                looping.set(false);
            }
        }
    }

    /**
     * Can be called multiple times in a row
     */
    public void start() {
        if (!looping.get()) {
            synchronized (looping) {
                if (!looping.get()) {
                    looping.set(true);
                    loopThread.submit(new WaitingWorkflowChecker());
                }
            }
        }
    }

    /**
     * Stops this service, {@link #start()} cannot longer be called. This is the destroy method.
     */
    public void stop() {
        this.clear();
        this.loopThread.shutdownNow();
    }

    public void clear() {
        this.waitingWorkflows.clear();
    }

    public void remove(WorkflowId workflowId) {
        this.waitingWorkflows.remove(workflowId);
    }
}
