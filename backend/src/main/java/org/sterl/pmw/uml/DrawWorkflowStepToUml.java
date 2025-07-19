package org.sterl.pmw.uml;

import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DrawWorkflowStepToUml {

    private final PlantUmlDiagram diagram;
    
    public void draw(WorkflowStep<?> s) {
        diagram.labeledConnector(s.getConnectorLabel());
        if (s.getDescription() == null) {
            diagram.appendState(s.getId());
        } else {
            diagram.appendState(s.getId(), s.getDescription());
        }
    }
}
