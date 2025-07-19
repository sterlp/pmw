package org.sterl.pmw.testapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.spring.PersistentWorkflowService;

public class SpringBeanWorkflowTests extends AbstractSpringTest {

    @Autowired
    PersistentWorkflowService subject;
    
    @Autowired
    Workflow<String> simpleStringWorkflow;
    
    @TestConfiguration
    public static class Config {
        
        @Bean
        Workflow<String> simpleStringWorkflow() {
            return Workflow.builder("Simple and Cool", () -> new String())
                    .next(s -> {})
                    .build();
        }
    }
    
    @Test
    void testNameAndKey() {
        
        assertThat(simpleStringWorkflow.getName()).isEqualTo("Simple and Cool");
        
        var key = subject.getWorkflowId(simpleStringWorkflow);
        assertThat(key).isNotEmpty();
        assertThat(key.get()).isEqualTo("simpleStringWorkflow");
    }

}
