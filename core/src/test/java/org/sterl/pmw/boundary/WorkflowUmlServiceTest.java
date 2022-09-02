package org.sterl.pmw.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.boundary.CoreWorkflowExecutionTest.TestWorkflowCtx;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowState;

class WorkflowUmlServiceTest {

    private WorkflowRepository repository = new WorkflowRepository();
    private WorkflowUmlService subject = new WorkflowUmlService(repository);

    @BeforeEach
    void setUp() throws Exception {
    }
    
    @Test
    void testOneStep() {
        // GIVEN
        Workflow<SimpleWorkflowState> w = Workflow.builder("test-workflow", () ->  new SimpleWorkflowState())
                .next(s -> {})
                .build();

        // WHEN
        final StringBuilder result = subject.printWorkflow(w);

        // THEN
        assertThat(result.toString()).isEqualTo(
                """
                start
                :Step 0;
                stop
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
                start
                :foo bar;
                stop
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
                start
                switch (if any)
                case ()
                :left;
                case ()
                :right;
                endswitch
                stop
                """);
    }

    public void assertWorkflolw(Workflow<?> w, String expected) {
        final StringBuilder result = subject.printWorkflow(w);
        if (!result.toString().equals(expected)) {
            System.err.println(result);
        }
        assertThat(result.toString()).isEqualTo(expected);
    }
}
