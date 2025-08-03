package org.sterl.pmw;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.stereotype.Service;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.uml.DrawWorkflowToUml;

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
        final String workflowUml = new DrawWorkflowToUml(workflow, workflowRepository).draw();
        return convertAsPlantUmlSvg(workflowUml, out);
    }

    public DiagramDescription convertAsPlantUmlSvg(String diagram, OutputStream out) throws IOException {
        var reader = new SourceStringReader(diagram);
        return reader.outputImage(out, 0, new FileFormatOption(FileFormat.SVG));
    }

    public String printWorkflow(String workflowId) {
        return printWorkflow(workflowRepository.getWorkflow(workflowId));
    }

    public String printWorkflow(Workflow<?> w) {
        return new DrawWorkflowToUml(
                w,
                workflowRepository).draw().toString();
    }
}
