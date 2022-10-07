package org.sterl.pmw.testapp;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sterl.pmw.boundary.WorkflowStatusOberserverTest;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;

@SpringBootTest
public class SpringStatusTest extends WorkflowStatusOberserverTest {

    @Autowired
    private QuartzWorkflowService quartzWorkflowService;
    @Autowired
    private TestWorkflowObserver testWorkflowObserver;

    @Override
    @BeforeEach
    protected void setUp() {
        quartzWorkflowService.clearAllWorkflows();
        ws = quartzWorkflowService;
        testWorkflowObserver.events.clear();
        subject = testWorkflowObserver;
    }
}
