package org.sterl.pmw.uml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.ChooseStep;
import org.sterl.pmw.model.ErrorStep;
import org.sterl.pmw.model.TriggerWorkflowStep;
import org.sterl.pmw.model.WaitStep;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DrawWorkflowToUml {

    private final Workflow<?> workflow;
    private final WorkflowRepository workflowRepository;
    private final List<Consumer<PlantUmlDiagram>> finalDrawer = new ArrayList<>();

    public String draw() {
        var diagram = new PlantUmlDiagram(workflow.getName());
        addDiagramTo(diagram);
        return diagram.build().toString();
    }

    public void addDiagramTo(final PlantUmlDiagram diagram) {
        diagram.start();

        addDiagramStepsTo(diagram);

        diagram.stop();
    }

    public void addDiagramStepsTo(final PlantUmlDiagram diagram) {
        for (WorkflowStep<?> step : workflow.getSteps()) {
            addWorkflowStepToDiagramByType(diagram, step);
        }
        finalDrawer.forEach(c -> c.accept(diagram));
        finalDrawer.clear();
    }

    private void addWorkflowStepToDiagramByType(final PlantUmlDiagram diagram,
            WorkflowStep<?> step) {
        if (step instanceof ChooseStep<?> ifStep) {
            addCooseStep(ifStep, diagram);
        } else if (step instanceof WaitStep<?> ws) {
            addWait(ws, diagram);
        } else if (step instanceof ErrorStep<?> er) {
            error(er, diagram);
        } else if (step instanceof TriggerWorkflowStep<?, ?> subW) {
            forkWorkflow(subW, subW.getSubWorkflow(), diagram);
        } else if (hasSubworkflow(step.getId()).isPresent()) {
            forkWorkflow(step, hasSubworkflow(step.getId()).get(), diagram);
        } else {
            draw(step, diagram);
        }
    }

    private void error(ErrorStep<?> er, PlantUmlDiagram diagram) {
        diagram.startIf("error?", "yes");
        addWorkflowStepToDiagramByType(diagram, er.getStep());
        diagram.appendElse("no");

        finalDrawer.add(d -> d.stopIf());
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

    private void forkWorkflow(WorkflowStep<?> s, Workflow<?> w, final PlantUmlDiagram diagram) {
        diagram.line("fork");
        diagram.intend();

        draw(s, diagram);
        appendSubWorkflow(w, diagram);

        diagram.stopIntend();
        diagram.line("fork again");

        this.finalDrawer.add(d -> {
            d.stopIntend();
            d.line("end fork");
        });

    }

    private void appendSubWorkflow(Workflow<?> w, final PlantUmlDiagram diagram) {
        diagram.line("partition \"" + w.getName() + "\" {");
        diagram.intend();

        new DrawWorkflowToUml(w, workflowRepository).addDiagramTo(diagram);

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
                appendSubWorkflow(tf.getSubWorkflow(), diagram);
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
