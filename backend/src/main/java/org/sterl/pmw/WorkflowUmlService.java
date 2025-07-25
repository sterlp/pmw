package org.sterl.pmw;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.TriggerWorkflowStep;
import org.sterl.pmw.model.WaitStep;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;
import org.sterl.pmw.uml.DrawWorkflowStepToUml;
import org.sterl.pmw.uml.PlantUmlDiagram;

import lombok.RequiredArgsConstructor;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.DiagramDescription;

@Service
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
        var diagram = new PlantUmlDiagram(workflow.getName());
        addWorkflow(workflow, diagram);
        return diagram.build().toString();
    }

    void addWorkflow(final Workflow<?> workflow, final PlantUmlDiagram diagram) {
        diagram.start();

        for (WorkflowStep<?> step : workflow.getSteps()) {
            addWorkflowStepToDiagramByType(diagram, step);
        }

        diagram.stop();
    }

    private void addWorkflowStepToDiagramByType(final PlantUmlDiagram diagram,
            WorkflowStep<?> step) {
        if (step instanceof ChooseStep<?> ifStep) {
            addCooseStep(ifStep, diagram);
        } else if (step instanceof WaitStep<?> ws) {
            addWait(ws, diagram);
        } else if (step instanceof TriggerWorkflowStep<?, ?> subW) {
            forkWorkflow(subW, subW.getSubWorkflow(), subW.getDelay(), diagram);
        } else if (hasSubworkflow(step.getId()).isPresent()) {
            forkWorkflow(step, hasSubworkflow(step.getId()).get(), null, diagram);
        } else {
            draw(step, diagram);
        }
    }

    private Optional<Workflow<?>> hasSubworkflow(String name) {
        if (name.toLowerCase().startsWith("trigger->")) {
            String workflowId = name.substring(9, name.length());
            return workflowRepository.findWorkflow(workflowId);
        }
        return Optional.empty();
    }
    
    private void addWait(WaitStep<?> wait, PlantUmlDiagram diagram) {
        var isSuspend = wait.isSuspendNext();
        if (isSuspend) {
            diagram.stopIntend();
            diagram.appendLine("stop");
            diagram.appendLine("");
            diagram.appendResume(wait.getId(), wait.getDescription());
            diagram.intend();
        } else {
            diagram.appendWait(wait.getId(), wait.getDescription());
        }

        
    }

    private void forkWorkflow(WorkflowStep<?> s, Workflow<?> w, Duration delay, final PlantUmlDiagram diagram) {
        draw(s, diagram);
        diagram.line("fork");
        diagram.line("fork again");
        diagram.intend();

        appendWorkflow(w, delay, diagram);

        diagram.stopIntend();
        diagram.line("end fork");

    }

    private void appendWorkflow(Workflow<?> w, Duration delay, final PlantUmlDiagram diagram) {
        if (delay != null && delay.getSeconds() > 0) {
            diagram.appendWait(delay.toString(), null);
        }

        diagram.line("partition \"" + w.getName() + "\" {");

        diagram.intend();
        addWorkflow(w, diagram);
        diagram.stopIntend();

        diagram.line("}");
    }

    private void addCooseStep(ChooseStep<?> ifStep, PlantUmlDiagram diagram) {
        diagram.appendLine(ifStep.getConnectorLabel());

        diagram.startSwitch(ifStep.getId());

        final var switchCases = new AtomicInteger(0);
        ifStep.getSubSteps().forEach((k, s) -> {
            diagram.startCase(s.getDescription());

            if (s instanceof TriggerWorkflowStep tf) {
                diagram.labeledConnector(s.getConnectorLabel());
                appendWorkflow(tf.getSubWorkflow(), tf.getDelay(), diagram);
                // not really a step, it ends in the workflow
            } else {
                draw(s, diagram);
                switchCases.incrementAndGet();
            }
            diagram.stopCase();
        });

        if (switchCases.intValue() > 1)
            diagram.endSwitch();
        else
            diagram.stopIntend();
    }

    private void draw(WorkflowStep<?> s, PlantUmlDiagram diagram) {
        new DrawWorkflowStepToUml(diagram).draw(s);
    }
}
