package org.sterl.pmw.quartz.boundary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.quartz.Scheduler;
import org.quartz.impl.DirectSchedulerFactory;
import org.sterl.pmw.boundary.CoreWorkflowExecutionTest;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.quartz.job.QuartzWorkflowJobFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class QuartzWorkflowTest extends CoreWorkflowExecutionTest {
    
    WorkflowRepository workflowRepository;
    Scheduler scheduler;
    private ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        DirectSchedulerFactory.getInstance().createVolatileScheduler(10);
        scheduler = DirectSchedulerFactory.getInstance().getScheduler();
        workflowRepository = new WorkflowRepository();
        subject = new QuartzWorkflowService(scheduler, workflowRepository);
        scheduler.setJobFactory(new QuartzWorkflowJobFactory(
                new SimpleWorkflowStepStrategy(), workflowRepository, mapper, null));
        
        scheduler.start();

    }

    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
        scheduler.shutdown();
    }
    

}
