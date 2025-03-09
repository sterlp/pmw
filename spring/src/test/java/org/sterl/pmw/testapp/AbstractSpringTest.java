package org.sterl.pmw.testapp;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.AsyncAsserts;
import org.sterl.pmw.sping_tasks.PersistentWorkflowService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;
import org.sterl.spring.persistent_tasks.history.HistoryService;
import org.sterl.spring.persistent_tasks.scheduler.SchedulerService;
import org.sterl.spring.persistent_tasks.scheduler.component.EditSchedulerStatusComponent;
import org.sterl.spring.persistent_tasks.scheduler.component.TaskExecutorComponent;
import org.sterl.spring.persistent_tasks.task.repository.TaskRepository;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;
import org.sterl.spring.persistent_tasks.trigger.model.TriggerEntity;

@SpringBootTest
public abstract class AbstractSpringTest {

    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected TriggerService triggerService;
    @Autowired
    protected PersistentWorkflowService workflowService;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected SchedulerService schedulerService;
    @Autowired
    protected HistoryService historyService;

    protected final AsyncAsserts asserts = new AsyncAsserts();

    @BeforeEach
    void setUp() throws Exception {
        historyService.deleteAll();
        workflowService.clearAllWorkflows();
        taskRepository.clear();
        triggerService.deleteAll();
        itemRepository.deleteAllInBatch();
        asserts.clear();
        
        schedulerService.setMaxThreads(10);
        schedulerService.start();
    }
    
    @Configuration
    public static class TestConfig {
        @Bean
        SchedulerService schedulerService(
                TriggerService triggerService, 
                EditSchedulerStatusComponent editSchedulerStatus,
                TransactionTemplate trx) {

            final var taskExecutor = new TaskExecutorComponent(triggerService, 10);
            taskExecutor.setMaxShutdownWaitTime(Duration.ofSeconds(0));
            return new SchedulerService("testScheduler", triggerService, taskExecutor, editSchedulerStatus, trx);
        }
    }

    protected void waitForAllWorkflows() {
      workflowService.queueAllWorkflowsAndWait();
    }
}
