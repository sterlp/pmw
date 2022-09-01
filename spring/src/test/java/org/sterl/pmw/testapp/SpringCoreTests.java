package org.sterl.pmw.testapp;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.boundary.CoreWorkflowExecutionTest;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;

@SpringBootTest
class SpringCoreTests extends CoreWorkflowExecutionTest {

    @Autowired
    private QuartzWorkflowService quartzWorkflowService;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        quartzWorkflowService.clearAllWorkflows();
        asserts.clear();
        subject = quartzWorkflowService;
    }
}
