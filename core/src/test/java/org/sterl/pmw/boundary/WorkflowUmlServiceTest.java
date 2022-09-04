package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.component.PlanUmlDiagram;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;

class WorkflowUmlServiceTest {

    private WorkflowRepository repository = new WorkflowRepository();
    private WorkflowUmlService subject = new WorkflowUmlService(repository);

    @BeforeEach
    void setUp() throws Exception {
        repository.clear();
    }
    
    @Test
    void testPlanUmlDiagram() throws Exception {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .next(s -> {})
                .next(s -> {})
                .build();
        
        repository.register(w);
        
        File d = new File("./test-workflow.svg");
        if (d.exists()) d.delete();
        d.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(d)) {
            subject.printWorkflowAsPlantUmlSvg("test-workflow", out);
        }
        
    }

    @Test
    void testOneStep() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .build();

        // WHEN
        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                :Step 0;
                stop
                @enduml
                """);
    }
    
    @Test
    void testOneGivenName() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next("foo bar", s -> {})
                .build();

        // THEN
        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                :foo bar;
                stop
                @enduml
                """);
    }

    @Test
    void testChoose() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .choose(s -> "a")
                    .ifSelected("left", s -> {})
                    .ifSelected("right", s -> {})
                    .build()
                .next(s -> {})
                .build();

        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                :Step 0;
                switch ()
                case ()
                :left;
                case ()
                :right;
                endswitch
                :Step 2;
                stop
                @enduml
                """);
    }
    
    @Test
    void testChooseWithName() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .choose("if any", s -> "a")
                    .ifSelected("left", s -> {})
                    .ifSelected("right", s -> {})
                    .build()
                .build();

        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                switch (if any)
                case ()
                :left;
                case ()
                :right;
                endswitch
                stop
                @enduml
                """);
    }

    public void assertWorkflolw(Workflow<?> w, String expected) {
        final PlanUmlDiagram result = new PlanUmlDiagram(w.getName(), null);
        subject.addWorkflow(w, result);
        final String diagram = result.build();
        if (!diagram.equals(expected)) {
            System.err.println(diagram);
        }
        assertThat(diagram).isEqualTo(expected);
    }
}
