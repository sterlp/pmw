package org.sterl.pmw.spring;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.boundary.CoreWorkflowExecutionTest;
import org.sterl.pmw.boundary.InMemoryWorkflowService;

@SpringBootTest
class SpringCoreWorkflowExecutionTest extends CoreWorkflowExecutionTest {

    @Autowired SpringWorkflowService springWorkflowService;
    
    @BeforeEach
    protected void setUp() throws Exception {
        springWorkflowService.cancelAll();
        Awaitility.setDefaultTimeout(Duration.ofSeconds(5));
        asserts.setDefaultTimeout(Duration.ofSeconds(5));
        asserts.clear();
        subject = springWorkflowService;
    }

}
