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
import org.sterl.pmw.uml.PlantUmlWritter;
import org.sterl.spring.persistent_tasks.api.RetryStrategy;

class WorkflowUmlServiceTest {

    private final static String SVG_PATH = "../doc/docs/assets/";
    private WorkflowRepository repository = new WorkflowRepository();
    private WorkflowUmlService subject = new WorkflowUmlService(repository);
    private WorkflowUmlService umlService = new WorkflowUmlService(repository);

    @BeforeEach
    void setUp() throws Exception {
        repository.clear();
    }

    @Test
    void testPlanUmlDiagram() throws Exception {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", SimpleWorkflowState::new)
                .next(s -> {})
                .next()
                    .id("stable")
                    .description("An custom id makes steps refactoring stable")
                    .connectorLabel("labeled arrow")
                    .function(e -> {})
                    .build()
                .next()
                    .description("By default each step is transactional, it can be turned off")
                    .function(e -> {})
                    .transactional(false)
                    .build()
                .stepRetryStrategy(RetryStrategy.THREE_RETRIES_IMMEDIATELY)
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
        
        PlantUmlWritter.writeAsPlantUmlSvg(SVG_PATH + "simple-workflow.svg", w, umlService);
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
                !include default-skin.puml
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
                !include default-skin.puml
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
                !include default-skin.puml
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
                !include default-skin.puml
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
                !include default-skin.puml
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
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("sleep workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .sleep(Duration.ofHours(2))
                .next(s -> {})
                .build();

        // WHEN & THEN
        assertWorkflow(w,
                """
                @startuml "sleep workflow"
                !include default-skin.puml
                start
                  :==<<T>> 10;
                  :==<$bi-hourglass,scale=1.2> 20
                  Wait for PT2H;
                  :==<<T>> 30;
                stop
                @enduml
                """);
        
        PlantUmlWritter.writeAsPlantUmlSvg(SVG_PATH + "sleep-workflow.svg", w, umlService);
    }

    @Test
    void testSubWorkflow() {
        // GIVEN
        Workflow<Integer> child = Workflow.builder("any child", () -> Integer.valueOf(0))
                .next(s -> {})
                .next(s -> {})
                .build();

        Workflow<SimpleWorkflowState> parent = Workflow.builder("parent", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .forkWorkflow(child)
                    // starting a new workflow may requirer a state mapping
                    .function(s -> Integer.valueOf(2))
                    .delay(Duration.ofMinutes(2))
                    .build()
                .next(s -> {})
                .next(s -> {})
                .build();

        // THEN
        assertWorkflow(parent,
                """
                @startuml "parent"
                !include default-skin.puml
                start
                  :==<<T>> 10;
                  fork
                    :==<<T>> 20
                    Run **any child** after PT2M;
                    partition "any child" {
                      start
                        :==<<T>> 10;
                        :==<<T>> 20;
                      stop
                    }
                  fork again
                  :==<<T>> 30;
                  :==<<T>> 40;
                end fork
                stop
                @enduml
                """);
        
        PlantUmlWritter.writeAsPlantUmlSvg(SVG_PATH + "sub-workflow.svg", parent, umlService);
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
                !include default-skin.puml
                start
                  :==<<T>> 10;
                  fork
                    :==<<T>> trigger->any child;
                    partition "any child" {
                      start
                        :==<<T>> 10;
                        :==<<T>> 20;
                      stop
                    }
                  fork again
                  :==<<T>> 20;
                end fork
                stop
                @enduml
                """);
    }
    
    @Test
    void testAwait() {
        // GIVEN
        Workflow<SimpleWorkflowState> parent = Workflow.builder("await workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .await(Duration.ZERO)
                .next(s -> {})
                .build();

        register(parent);

        // THEN
        assertWorkflow(parent, """
                @startuml "await workflow"
                !include default-skin.puml
                start
                  :==<<T>> 10;
                stop
                
                :==<$bi-envelope,scale=1.2> 20
                Suspend at most PT0S;
                  :==<<T>> 30;
                stop
                @enduml
                """);
        
        PlantUmlWritter.writeAsPlantUmlSvg(SVG_PATH + "await-workflow.svg", parent, umlService);
    }
    
    @Test
    void testWithErrorHandler() {
        // GIVEN
        Workflow<SimpleWorkflowState> parent = Workflow.builder("error flow", () ->  new SimpleWorkflowState())
                .next()
                    .description("This step may fail")
                        .function(s -> {})
                        .build()
                .onLastStepError()
                    .next()
                        .description("Will only run if the last step **failed**")
                        .function(s -> {})
                        .build()
                    .build()
                .next()
                    .description("Will only run if **no** error occured")
                    .function(s -> {})
                    .build()
                .next()
                    .description("Some other step")
                    .function(s -> {})
                    .build()
                .build();

        register(parent);

        // THEN
        assertWorkflow(parent, """
                @startuml "error flow"
                !include default-skin.puml
                start
                  :==<<T>> 10
                  This step may fail;
                  if ( error? ) then (yes)
                    :==<<T>> 20
                    Will only run if the last step **failed**;
                  else ( no)
                    :==<<T>> 30
                    Will only run if **no** error occured;
                    :==<<T>> 40
                    Some other step;
                  endif
                stop
                @enduml
                """);
        
        PlantUmlWritter.writeAsPlantUmlSvg(SVG_PATH + "error-workflow.svg", parent, umlService);
    }

    private void assertWorkflow(Workflow<?> w, String expected) {
        final String diagram = subject.printWorkflow(w);

        String[] actualLines = diagram.split("\\R"); // Splits on any line break
        String[] expectedLines = expected.split("\\R");

        int maxLines = Math.max(actualLines.length, expectedLines.length);

        for (int i = 0; i < maxLines; i++) {
            String actualLine = i < actualLines.length ? actualLines[i] : "missing line  " + (i + 1);
            String expectedLine = i < expectedLines.length ? expectedLines[i] : "missing line  " + (i + 1);
            if (!actualLine.equals(expectedLine)) {
                System.err.printf("Mismatch at line %d:%nExpected: %s%nActual:   %s%n%n", i + 1, expectedLine, actualLine);
                System.err.println(diagram);
            }
            
            assertThat(actualLine).as("Line " + (i + 1)).isEqualTo(expectedLine);
        }
    }
    
    private void register(Workflow<?> w) {
        repository.register(w.getName(), w);
    }
}
