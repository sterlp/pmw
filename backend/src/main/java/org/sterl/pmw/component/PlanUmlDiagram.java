package org.sterl.pmw.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class PlanUmlDiagram {
    private static final String NEW_LINE = "\n";
    private static final String START = ":";
    private static final String END = ";";

    private final List<String> diagram = new ArrayList<>();
    private final String name;
    private final String theme;
    private int intend = 0;

    public PlanUmlDiagram(String name) {
        this(name, "!theme carbon-gray");
    }

    public PlanUmlDiagram(String name, String theme) {
        super();
        this.name = name;
        this.theme = theme;
    }
    
    public void intend() {
        this.intend += 2;
    }
    
    public void stopIntend() {
        this.intend -= 2;
    }

    public PlanUmlDiagram appendLine(String line) {
        if (line != null) {
            diagram.add(StringUtils.repeat(' ', intend) + line);
        }
        return this;
    }

    public PlanUmlDiagram line(String value) {
        return appendLine(value);
    }

    public PlanUmlDiagram start() {
        appendLine("start");
        intend();
        return this;
    }
    public PlanUmlDiagram stop() {
        stopIntend();
        appendLine("stop");
        return this;
    }

    public PlanUmlDiagram appendWaitState(String id, String description) {
        this.appendState("<&clock> " + id, description);
        return this;
    }
    
    public PlanUmlDiagram labeldConnector(String label) {
        line("-> " + label + END);
        return this;
    }

    public PlanUmlDiagram appendState(String stateName) {
        line(START + "**" + stateName + "**" + END);
        return this;
    }
    
    public void appendState(String id, String description) {
        if (description == null) {
            appendState(id);
        } else {
            line(START + "--**" + id + "**--");
            line(description + END);
        }
    }
    
    public PlanUmlDiagram startSwitch(String label) {
        if (label == null) line("switch ()");
        else line("switch ( " + label + " )");
        intend();
        return this;
    }
    
    public PlanUmlDiagram endSwitch() {
        stopIntend();
        line("endswitch");
        return this;
    }

    public void startCase() {
        line("case ()");
        intend();
    }
    public void stopCase() {
        stopIntend();
    }
    public void appendCase(String label) {
        if (label == null) line("case ()");
        else line("case ( " + label + " )");
    }
    
    public StringBuilder build() {
        var result = new StringBuilder(diagram.size() * 8 + 24);
        result.append("@startuml ").append("\"").append(name).append("\"").append(NEW_LINE);
        
        if (theme != null) result.append(theme).append(NEW_LINE);
        
        diagram.forEach(l -> result.append(l).append(NEW_LINE));
        result.append("@enduml");
        return result;
    }
}
