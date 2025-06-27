package org.sterl.pmw.spring.api;

import net.sourceforge.plantuml.core.DiagramDescription;

public record WorkflowDiagram(DiagramDescription description,
        String plantUml,
        String svgBase64) {

}
