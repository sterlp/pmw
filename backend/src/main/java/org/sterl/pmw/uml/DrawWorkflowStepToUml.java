package org.sterl.pmw.uml;

import org.sterl.pmw.model.WorkflowStep;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DrawWorkflowStepToUml {

    private final PlantUmlDiagram diagram;
    
    public void draw(WorkflowStep<?> s) {
        diagram.labeledConnector(s.getConnectorLabel());
        var icon = s.isTransactional() ? PlantUmlDiagram.ICON_TRX + " " : "";
        diagram.appendState(icon + s.getId(), s.getDescription());
    }
}
