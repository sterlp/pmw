package org.sterl.pmw;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.uml.PlantUmlDiagram;

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
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
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
    void testSimpleSteps() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .next(s -> {})
                .next(s -> {})
                .build();

        // WHEN
        assertWorkflow(w,
                """
                @startuml "test-workflow"
                start
                  :**10**;
                  :**20**;
                  :**30**;
                stop
                @enduml
                """);
    }

    @Test
    void testOneGivenName() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next()
                    .id("foo bar")
                    .description("HA ha")
                    .connectorLabel("asdad")
                    .function(s -> {})
                    .build()
                .build();

        // THEN
        assertWorkflow(w,
                """
                @startuml "test-workflow"
                start
                  -> asdad;
                  :-- **foo bar** --
                  HA ha;
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
                    .ifSelected("do stuff on right", s -> {})
                    .build()
                .next(s -> {})
                .build();

        assertWorkflow(w,
                """
                @startuml "test-workflow"
                start
                  :**10**;
                  switch ( 20 )
                    case ()
                      :**left**;
                    case ()
                      :**do stuff on right**;
                  endswitch
                  :**30**;
                stop
                @enduml
                """);
    }

    @Test
    void testChooseWithName() {
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
                .choose("select", s -> "left")
                    .ifSelected("left", s -> {})
                    .ifSelected()
                        .id("something")
                        .description("ja ja")
                        .connectorLabel("nope")
                        .function(s -> {})
                        .build()
                    .build()
                .next(s -> {})
                .build();

        assertWorkflow(w,
                """
                @startuml "test-workflow"
                start
                  switch ( select )
                    case ()
                      :**left**;
                    case ( ja ja )
                      -> nope;
                      :**something**;
                  endswitch
                  :**10**;
                stop
                @enduml
                """);
    }
    
    @Test
    void testChooseSubWorkflow() {
        Workflow<String> sendMail = Workflow.builder("send-mail", () -> new String())
                .next("build-mail", s -> {})
                .next("send-mail", s -> {})
                .sleep(Duration.ofMinutes(1))
                .next("check-response", s -> {})
                .build();
                
        Workflow<SimpleWorkflowState> doStuff = Workflow.builder("test-workflow", SimpleWorkflowState::new)
                .choose("select", s -> "left")
                    .ifTrigger("hcheCkMail", sendMail)
                        .description("Has email")
                        .function(s -> "paul@paul.de")
                        .build()
                    .ifSelected("noMail", s -> {})
                    .build()
                .next("create-user-task", s -> {})
                .build();
        
        repository.register(sendMail);
        repository.register(doStuff);

        assertWorkflow(doStuff,
                """
                @startuml "test-workflow"
                start
                  switch ( select )
                    case ( Has email )
                      partition "send-mail" {
                        start
                          :**build-mail**;
                          :**send-mail**;
                          :-- **<&clock> 10** --
                          Wait for PT1M;
                          :**check-response**;
                        stop
                      }
                    case ()
                      :**noMail**;
                  :**create-user-task**;
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

        assertWorkflow(w,
                """
                @startuml "test-workflow"
                start
                  :**10**;
                  :-- **<&clock> 20** --
                  Wait for PT2H;
                  :**30**;
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
                .trigger(child).function(s -> s).id("cool workflow").delay(Duration.ofMinutes(2)).build()
                .next(s -> {})
                .build();

        // THEN
        assertWorkflow(parent,
                """
                @startuml "parent"
                start
                  :**10**;
                  :-- **cool workflow** --
                  Start any child;
                  fork
                  fork again
                    :**<&clock> PT2M**;
                    partition "any child" {
                      start
                        :**10**;
                        :**20**;
                      stop
                    }
                  end fork
                  :**20**;
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
        assertWorkflow(parent, """
                @startuml "parent"
                start
                  :**10**;
                  :**trigger->any child**;
                  fork
                  fork again
                    partition "any child" {
                      start
                        :**10**;
                        :**20**;
                      stop
                    }
                  end fork
                  :**20**;
                stop
                @enduml
                """);
    }

    public void assertWorkflow(Workflow<?> w, String expected) {
        final PlantUmlDiagram result = new PlantUmlDiagram(w.getName(), null);
        subject.addWorkflow(w, result);
        final String diagram = result.build().toString();

        String[] actualLines = diagram.split("\\R"); // Splits on any line break
        String[] expectedLines = expected.split("\\R");

        int maxLines = Math.max(actualLines.length, expectedLines.length);

        for (int i = 0; i < maxLines; i++) {
            String actualLine = i < actualLines.length ? actualLines[i] : "missing line  + i + 1";
            String expectedLine = i < expectedLines.length ? expectedLines[i] : "missing line  + i + 1";
            if (!actualLine.equals(expectedLine)) {
                System.err.printf("Mismatch at line %d:%nExpected: %s%nActual:   %s%n%n", i + 1, expectedLine, actualLine);
                System.err.println(diagram);
            }
            
            assertThat(actualLine).as("Line " + (i + 1)).isEqualTo(expectedLine);
        }
    }
}
