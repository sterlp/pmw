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

        register(w);

        File d = new File("./test-workflow.svg");
        if (d.exists()) d.delete();
        //d.deleteOnExit();

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
                  :==<<T>> 10;
                  :==<<T>> 20;
                  :==<<T>> 30;
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
                  :==<<T>> foo bar
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
                  :==<<T>> 10;
                  switch ( 20 )
                    case ()
                      :==<<T>> left;
                    case ()
                      :==<<T>> do stuff on right;
                  endswitch
                  :==<<T>> 30;
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
                      :==<<T>> left;
                    case ( ja ja )
                      -> nope;
                      :==<<T>> something
                      ja ja;
                  endswitch
                  :==<<T>> 10;
                stop
                @enduml
                """);
    }
    
    @Test
    void testChooseSubWorkflow() {
        Workflow<String> sendMail = Workflow.builder("send-mail", () -> new String())
                .next("build-mail").transactional(false).function(c -> {}).build()
                .next("send-mail").transactional(false).function(c -> {}).build()
                .sleep(Duration.ofMinutes(1))
                .next("check-response").transactional(false).function(c -> {}).build()
                .build();
                
        Workflow<SimpleWorkflowState> doStuff = Workflow.builder("test-workflow", SimpleWorkflowState::new)
                .choose("select", s -> "left")
                    .ifTrigger("checkMail", sendMail)
                        .description("Has email")
                        .function(s -> "paul@paul.de")
                        .build()
                    .ifSelected("noMail")
                        .function(s -> {})
                        .transactional(false)
                        .build()
                    .build()
                .next("create-user-task", s -> {})
                .build();
        
        register(sendMail);
        register(doStuff);

        assertWorkflow(doStuff,
                """
                @startuml "test-workflow"
                start
                  switch ( select )
                    case ( Has email )
                      partition "send-mail" {
                        start
                          :==build-mail;
                          :==send-mail;
                          :==<$bi-hourglass,scale=1.2> 10
                          Wait for PT1M;
                          :==check-response;
                        stop
                      }
                    case ()
                      :==noMail;
                  :==<<T>> create-user-task;
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
                  :==<<T>> 10;
                  :==<$bi-hourglass,scale=1.2> 20
                  Wait for PT2H;
                  :==<<T>> 30;
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
                  :==<<T>> 10;
                  :==<<T>> cool workflow
                  Start any child;
                  fork
                  fork again
                    :==<$bi-hourglass,scale=1.2> PT2M;
                    partition "any child" {
                      start
                        :==<<T>> 10;
                        :==<<T>> 20;
                      stop
                    }
                  end fork
                  :==<<T>> 20;
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

        register(parent);
        register(child);

        // THEN
        assertWorkflow(parent, """
                @startuml "parent"
                start
                  :==<<T>> 10;
                  :==<<T>> trigger->any child;
                  fork
                  fork again
                    partition "any child" {
                      start
                        :==<<T>> 10;
                        :==<<T>> 20;
                      stop
                    }
                  end fork
                  :==<<T>> 20;
                stop
                @enduml
                """);
    }
    
    @Test
    void testAwait() {
        // GIVEN
        Workflow<SimpleWorkflowState> parent = Workflow.builder("parent", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .await(Duration.ZERO)
                .next(s -> {})
                .build();

        register(parent);

        // THEN
        assertWorkflow(parent, """
                @startuml "parent"
                start
                  :==<<T>> 10;
                stop
                
                :==<$bi-envelope,scale=1.2> 20
                Suspend at most PT0S;
                  :==<<T>> 30;
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
    
    void register(Workflow<?> w) {
        repository.register(w.getName(), w);
    }
}
