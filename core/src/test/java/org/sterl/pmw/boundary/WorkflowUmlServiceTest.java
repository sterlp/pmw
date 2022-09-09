package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Duration;

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

        assertThat(d).exists();
        assertThat(Files.size(d.toPath())).isGreaterThan(5L);
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
                :0. Step;
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
                    .ifSelected("do stuff on right", "if right", s -> {})
                    .build()
                .next(s -> {})
                .build();

        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                :0. Step;
                switch ()
                case ()
                :left;
                case (if right)
                :do stuff on right;
                endswitch
                :2. Step;
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

    @Test
    void testSleep() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .sleep(Duration.ofHours(2))
                .next(s -> {})
                .build();

        assertWorkflolw(w,
                """
                @startuml "test-workflow"
                start
                :0. Step;
                :<&clock> Sleep for PT2H;
                :2. Step;
                stop
                @enduml
                """);
    }

    @Test
    void testSubWorkflow() {
        // GIVEN
        Workflow<SimpleWorkflowState> child = Workflow.builder("any child", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .next(s -> {})
                .build();

        Workflow<SimpleWorkflowState> parent = Workflow.builder("parent", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .trigger(child, s -> s)
                .next(s -> {})
                .build();

        // THEN
        assertWorkflolw(parent,
                """
                @startuml "parent"
                start
                :0. Step;
                fork
                fork again
                partition "any child"{
                start
                :0. Step;
                :1. Step;
                stop
                }
                endfork
                :2. Step;
                stop
                @enduml
                """);
    }

    @Test
    void testSubWorkflowByName() {
        // GIVEN
        Workflow<SimpleWorkflowState> child = Workflow.builder("any child", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .next(s -> {})
                .build();

        Workflow<SimpleWorkflowState> parent = Workflow.builder("parent", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .next("trigger->any child", s -> {})
                .next(s -> {})
                .build();

        repository.register(parent);
        repository.register(child);

        // THEN
        assertThat(subject.printWorkflow("parent")).isEqualTo("""
                @startuml "parent"
                !theme carbon-gray
                start
                :0. Step;
                fork
                fork again
                partition "any child"{
                start
                :0. Step;
                :1. Step;
                stop
                }
                endfork
                :2. Step;
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
