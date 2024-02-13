package org.sterl.pmw.spring.component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.sterl.pmw.component.RunningWorkflowComponent;
import org.sterl.pmw.component.SimpleWorkflowStepExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.spring.model.PersistentWorkflowState;
import org.sterl.pmw.spring.repository.PersistentWorkflowStateRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SpringWorkflowExecutorComponent implements RunningWorkflowComponent {

    private final WorkflowRepository workflowRepository;
    private final PersistentWorkflowStateRepository workflowStateRepository;
    private final ObjectMapper objectMapper;
    private final Executor runner = Executors.newWorkStealingPool();

    public void run(List<PersistentWorkflowState> pendingWorkflows) {
        for (PersistentWorkflowState ps : pendingWorkflows) {
            Workflow<? extends WorkflowState> workflow = workflowRepository.getWorkflow(ps.getName());
            SimpleWorkflowStepExecutor
        }
    }

    @Override
    public void runOrQueueNextStep(WorkflowId id, RunningWorkflowState<?> runningWorkflowState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cancel(WorkflowId workflowId) {
        workflowStateRepository.findById(workflowId)
            .ifPresent(w -> w.cancel());
    }

    @Override
    public <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow, T state, Duration delay) {
        // TODO Auto-generated method stub
        return null;
    }
}
