package org.sterl.pmw.uml;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class PlantUmlDiagram {
    private static final String NEW_LINE = "\n";
    private static final String START = ":";
    private static final String END = ";";

    private final List<String> diagram = new ArrayList<>();
    private final String name;
    private final String theme;
    private int intend = 0;

    public PlantUmlDiagram(String name) {
        this(name, "!theme carbon-gray");
    }

    public PlantUmlDiagram(String name, String theme) {
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

    public PlantUmlDiagram appendLine(String line) {
        if (line != null) {
            diagram.add(StringUtils.repeat(' ', intend) + line);
        }
        return this;
    }

    public PlantUmlDiagram line(String value) {
        return appendLine(value);
    }

    public PlantUmlDiagram start() {
        appendLine("start");
        intend();
        return this;
    }
    public PlantUmlDiagram stop() {
        stopIntend();
        appendLine("stop");
        return this;
    }

    public PlantUmlDiagram appendWaitState(String id, String description) {
        this.appendState("<&clock> " + id, description);
        return this;
    }
    
    public PlantUmlDiagram labeledConnector(String label) {
        if (label != null) line("-> " + label + END);
        return this;
    }

    public PlantUmlDiagram appendState(String stateName) {
        line(START + "**" + stateName + "**" + END);
        return this;
    }
    
    public void appendState(String id, String description) {
        if (description == null) {
            appendState(id);
        } else {
            line(START + "-- **" + id + "** --");
            line(description + END);
        }
    }
    
    public PlantUmlDiagram startSwitch(String label) {
        if (label == null) line("switch ()");
        else line("switch ( " + label + " )");
        intend();
        return this;
    }
    
    public PlantUmlDiagram endSwitch() {
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
    public void startCase(String label) {
        if (label == null) startCase();
        else {
            line("case ( " + label + " )");
            intend();
        }
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
