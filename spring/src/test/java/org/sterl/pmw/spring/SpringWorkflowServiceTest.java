package org.sterl.pmw.spring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.model.SimpleWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowStatus;

@SpringBootTest
public class SpringWorkflowServiceTest {

    @Autowired SpringWorkflowService subject;
    
    final Workflow<SimpleWorkflowState> w = Workflow.builder("test-SpringWorkflowServiceTest", () ->  new SimpleWorkflowState())
            .next((s) -> {
                System.out.println(s.getState());
            })
            .build();
    
    @Test
    void fooTest() {
        // GIVEN
        subject.register(w);

        // WHEN
        WorkflowId id = subject.execute(w);
        
        // THEN
        assertThat(subject.status(id)).isEqualTo(WorkflowStatus.PENDING);
    }
}
