package org.sterl.pmw.boundary;

import java.util.Map.Entry;

import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.IfStep;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkflowUmlService {

    private final WorkflowRepository workflowRepository;
    
    public String printWorkflow(String workflowId) {
        final Workflow<? extends WorkflowState> workflow = workflowRepository.getWorkflow(workflowId);
        
        printWorkflow(workflow);
        
        return null;
    }

    public StringBuilder printWorkflow(final Workflow<? extends WorkflowState> workflow) {
        StringBuilder result = new StringBuilder("start").append("\n");

        for (WorkflowStep<? extends WorkflowState> step : workflow.getSteps()) {
            if (step instanceof IfStep<?> ifStep) {
                addSwitch(ifStep, result);
                for (Entry<String, WorkflowStep<?>> e : ifStep.getSubSteps().entrySet()) {
                    result.append("case ()\n");
                    addStepName(e.getKey(), result);
                }
                result.append("endswitch\n");
            } else {
                addStepName(step, result);
            }
        }
        result.append("stop").append("\n");
        
        return result;
    }
    
    public void addSwitch(IfStep<?> step, StringBuilder result) {
        result.append("switch (");
        if (!step.getName().startsWith("Step ")) {
            result.append(step.getName());
        }
        result.append(")\n");
    }
    
    public void addStepName(WorkflowStep<?> step, StringBuilder result) {
        addStepName(step.getName(), result);
    }
    
    public void addStepName(String stepName, StringBuilder result) {
        result.append(":").append(stepName).append( ";\n");
    }
}
