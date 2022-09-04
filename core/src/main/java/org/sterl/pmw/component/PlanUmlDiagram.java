package org.sterl.pmw.component;

public class PlanUmlDiagram {
    private static final String NEW_LINE = "\n";

    private final StringBuilder diagram = new StringBuilder();
    private final String name;
    private final String theme;
    
    public PlanUmlDiagram(String name) {
        this(name, "!theme carbon-gray");
    }
    
    public PlanUmlDiagram(String name, String theme) {
        super();
        this.name = name;
        this.theme = theme;
        init();
    }
    
    private void init() {
        diagram.setLength(0);
        diagram.append("@startuml ").append("\"").append(name).append("\"").append(NEW_LINE);
        appendLine(theme);
    }
    
    public PlanUmlDiagram appendLine(String line) {
        if (line != null) {
            diagram.append(line).append(NEW_LINE);
        }
        return this;
    }
    
    public String build() {
        appendLine("@enduml");
        final String result = diagram.toString();
        init();
        return result;
    }

    public PlanUmlDiagram start() {
        appendLine("start");
        return this;
    }
    public PlanUmlDiagram stop() {
        appendLine("stop");
        return this;
    }

    public PlanUmlDiagram appendState(String stateName) {
        diagram.append(":").append(stateName).append( ";").append(NEW_LINE);
        return this;
    }

    public PlanUmlDiagram append(String value) {
        diagram.append(value);
        return this;
    }
}
