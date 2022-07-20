package org.sterl.pmw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sterl.pmw.boundary.InMemoryWorkflowService;
import org.sterl.pmw.model.SimpleWorkflowContext;
import org.sterl.pmw.model.Workflow;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ExampleWorkflowTest {
    
    InMemoryWorkflowService inMemoryWorkflowService = new InMemoryWorkflowService();

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void test() {
        assertThat(inMemoryWorkflowService).isNotNull();
    }
    
    @Test
    void testWorkflow() {
        Workflow<SimpleWorkflowContext> w = new Workflow<SimpleWorkflowContext>("test-workflow")
            .next(c -> {
                log.info("do-first");
            })
            .next(c -> {
                log.info("do-second");
            })
            .choose(c -> {
                log.info("choose");
                return "left";
            }).ifSelected("left", c -> {
                log.info("  going left");
            }).ifSelected("right", c -> {
                log.info("  going right");
            })
            .end()
            .next(c -> {
                log.info("finally");
            });
        
        
        inMemoryWorkflowService.execute(w, SimpleWorkflowContext.newContextFor(w));
    }
    
    @Test
    void testRightFirst() {
        Workflow<SimpleWorkflowContext> w = new Workflow<SimpleWorkflowContext>("test-workflow")
            .choose(c -> {
                log.info("choose");
                return "right";
            }).ifSelected("left", c -> {
                log.info("  going left");
            }).ifSelected("right", c -> {
                log.info("  going right");
            })
            .end()
            .next(c -> {
                log.info("finally");
            });
        
        
        inMemoryWorkflowService.execute(w, SimpleWorkflowContext.newContextFor(w));
    }

}
