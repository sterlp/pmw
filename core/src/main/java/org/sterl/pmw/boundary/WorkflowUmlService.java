package org.sterl.pmw.boundary;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;

import org.sterl.pmw.component.PlanUmlDiagram;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.WaitStep;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;

@RequiredArgsConstructor
public class WorkflowUmlService {

    private final WorkflowRepository workflowRepository;
    
    public DiagramDescription printWorkflowAsPlantUmlSvg(String workflowId, OutputStream out) throws IOException {
        return this.printWorkflowAsPlantUmlSvg(workflowRepository.getWorkflow(workflowId), out);
    }
    public DiagramDescription printWorkflowAsPlantUmlSvg(Workflow<? extends WorkflowState> workflow, OutputStream out) throws IOException {
        final String workflowUml = printWorkflow(workflow);
        return convertAsPlantUmlSvg(workflowUml, out);
    }
    
    public DiagramDescription convertAsPlantUmlSvg(String diagram, OutputStream out) throws IOException {
        SourceStringReader reader = new SourceStringReader(diagram);
        return reader.outputImage(out, 0, new FileFormatOption(FileFormat.SVG));
    }
    
    public String printWorkflow(String workflowId) {
        return printWorkflow(workflowRepository.getWorkflow(workflowId));
    }

    public String printWorkflow(Workflow<? extends WorkflowState> workflow) {
        PlanUmlDiagram diagram = new PlanUmlDiagram(workflow.getName());
        addWorkflow(workflow, diagram);
        return diagram.build();
    }

    void addWorkflow(final Workflow<? extends WorkflowState> workflow, final PlanUmlDiagram diagram) {
        diagram.start();

        for (WorkflowStep<? extends WorkflowState> step : workflow.getSteps()) {
            if (step instanceof ChooseStep<?> ifStep) {
                addIfStep(ifStep, diagram);
            } else if (step instanceof WaitStep<?>) {
                diagram.appendWaitState(step.getName());
            } else {
                addStepName(step, diagram);
            }
        }

        diagram.stop();
    }

    private void addIfStep(ChooseStep<?> ifStep, PlanUmlDiagram diagram) {
        addSwitch(ifStep, diagram);
        for (Entry<String, WorkflowStep<?>> e : ifStep.getSubSteps().entrySet()) {
            diagram.appendCase(e.getValue().getConnectorLabel());
            diagram.appendState(e.getKey());
        }
        diagram.appendLine("endswitch");
    }
    
    private void addSwitch(ChooseStep<?> step, PlanUmlDiagram diagram) {
        diagram.append("switch (");
        if (!step.getName().startsWith("Step ")) {
            diagram.append(step.getName());
        }
        diagram.appendLine(")");
    }
    
    private void addStepName(WorkflowStep<?> step, PlanUmlDiagram diagram) {
        diagram.appendState(step.getName());
    }
}
