package org.sterl.pmw;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

import org.sterl.pmw.component.PlanUmlDiagram;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.TriggerWorkflowStep;
import org.sterl.pmw.model.WaitStep;
import org.sterl.pmw.model.Workflow;
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
    public DiagramDescription printWorkflowAsPlantUmlSvg(Workflow<?> workflow, OutputStream out) throws IOException {
        final String workflowUml = printWorkflow(workflow);
        return convertAsPlantUmlSvg(workflowUml, out);
    }

    public DiagramDescription convertAsPlantUmlSvg(String diagram, OutputStream out) throws IOException {
        var reader = new SourceStringReader(diagram);
        return reader.outputImage(out, 0, new FileFormatOption(FileFormat.SVG));
    }

    public String printWorkflow(String workflowId) {
        return printWorkflow(workflowRepository.getWorkflow(workflowId));
    }

    public String printWorkflow(Workflow<?> workflow) {
        var diagram = new PlanUmlDiagram(workflow.getName());
        addWorkflow(workflow, diagram);
        return diagram.build();
    }

    void addWorkflow(final Workflow<?> workflow, final PlanUmlDiagram diagram) {
        diagram.start();

        for (WorkflowStep<?> step : workflow.getSteps()) {
            addWorkflowStepToDiagramByType(diagram, step);
        }

        diagram.stop();
    }
    private void addWorkflowStepToDiagramByType(final PlanUmlDiagram diagram,
            WorkflowStep<?> step) {
        if (step instanceof ChooseStep<?> ifStep) {
            addCooseStep(ifStep, diagram);
        } else if (step instanceof WaitStep<?>) {
            diagram.appendWaitState(step.getName());
        } else if (step instanceof TriggerWorkflowStep<?, ?> subW) {
            addSubWorkflow(subW.getSubWorkflow(), diagram);
        } else if (hasSubworkflow(step.getName()).isPresent()) {
            addSubWorkflow(hasSubworkflow(step.getName()).get(), diagram);
        } else {
            addStepName(step, diagram);
        }
    }

    private Optional<Workflow<?>> hasSubworkflow(String name) {
        Optional<Workflow<?>> result = workflowRepository.findWorkflow(name);

        if (result.isEmpty() && name.toLowerCase().startsWith("trigger->")) {
            String workflowName = name.substring(9, name.length());
            result = workflowRepository.findWorkflow(workflowName);
        }
        return result;
    }

    private void addSubWorkflow(final Workflow<?> workflow, final PlanUmlDiagram diagram) {
        diagram.appendLine("fork");
        diagram.appendLine("fork again");
        diagram.append("partition \"").append(workflow.getName()).appendLine("\"{");
        addWorkflow(workflow, diagram);
        diagram.appendLine("}");
        diagram.appendLine("endfork");
    }
    private void addCooseStep(ChooseStep<?> ifStep, PlanUmlDiagram diagram) {
        addSwitch(ifStep, diagram);
        for (WorkflowStep<?> e : ifStep.getSubSteps().values()) {
            diagram.appendCase(e.getConnectorLabel());

            addWorkflowStepToDiagramByType(diagram, e);
        }
        diagram.appendLine("endswitch");
    }

    private void addSwitch(ChooseStep<?> step, PlanUmlDiagram diagram) {
        diagram.append("switch (");
        if (!step.getName().endsWith(" Step")) {
            diagram.append(step.getName());
        }
        diagram.appendLine(")");
    }

    private void addStepName(WorkflowStep<?> step, PlanUmlDiagram diagram) {
        diagram.appendState(step.getName());
    }
}
