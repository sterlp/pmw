package org.sterl.pmw;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
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
        return diagram.build().toString();
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
            diagram.appendWaitState(step.getId(), step.getDescription());
        } else if (step instanceof TriggerWorkflowStep<?, ?> subW) {
            addSubWorkflow(subW, subW.getSubWorkflow(), subW.getDelay(), diagram);
        } else if (hasSubworkflow(step.getId()).isPresent()) {
            addSubWorkflow(step, hasSubworkflow(step.getId()).get(), null, diagram);
        } else {
            draw(step, diagram);
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

    private void addSubWorkflow(WorkflowStep<?> s, Workflow<?> w, Duration delay, final PlanUmlDiagram diagram) {
        draw(s, diagram);
        diagram.line("fork");
        diagram.line("fork again");
        diagram.intend();
        
        if (delay != null && delay.getSeconds() > 0) {
            diagram.appendWaitState(delay.toString(), null);
        }

        diagram.line("partition \"" + w.getName() + "\" {");
        
        diagram.intend();
        addWorkflow(w, diagram);
        diagram.stopIntend();
        
        diagram.line("}");
        diagram.stopIntend();
        diagram.line("end fork");
        
    }
    private void addCooseStep(ChooseStep<?> ifStep, PlanUmlDiagram diagram) {
        if (ifStep.getConnectorLabel() != null) diagram.appendLine(ifStep.getConnectorLabel());
        diagram.startSwitch(ifStep.getId());
        
        ifStep.getSubSteps().forEach((k, s) -> {
            diagram.startCase();
            addWorkflowStepToDiagramByType(diagram, s);
            diagram.stopCase();
        });

        diagram.endSwitch();
    }

    private void draw(WorkflowStep<?> s, PlanUmlDiagram diagram) {
        if (s.getConnectorLabel() != null) {
            diagram.labeldConnector(s.getConnectorLabel());
        }
        if (s.getDescription() == null) {
            diagram.appendState(s.getId());
        } else {
            diagram.appendState(s.getId(), s.getDescription());
        }
    }
}
