package org.sterl.pmw.testapp;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.spring.PersistentWorkflowService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;
import org.sterl.spring.persistent_tasks.api.TriggerKey;
import org.sterl.spring.persistent_tasks.history.HistoryService;
import org.sterl.spring.persistent_tasks.scheduler.SchedulerService;
import org.sterl.spring.persistent_tasks.scheduler.component.EditSchedulerStatusComponent;
import org.sterl.spring.persistent_tasks.scheduler.config.SchedulerConfig;
import org.sterl.spring.persistent_tasks.scheduler.config.SchedulerThreadFactory;
import org.sterl.spring.persistent_tasks.task.repository.TaskRepository;
import org.sterl.spring.persistent_tasks.test.AsyncAsserts;
import org.sterl.spring.persistent_tasks.test.PersistentTaskTestService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;

import io.micrometer.core.instrument.MeterRegistry;

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
    
    @Autowired
    protected PersistentTaskTestService persistentTaskTestService;

    protected final AsyncAsserts asserts = new AsyncAsserts();

    @BeforeEach
    void setUp() throws Exception {
        try {
            persistentTaskTestService.awaitRunningTriggers();
        } catch (Exception e) {
            System.err.println("awaitRunningTriggers has an error, do we care? No! " + e.getMessage());
        }
        itemRepository.deleteAllInBatch();
        asserts.clear();
        Awaitility.setDefaultTimeout(Duration.ofSeconds(5));
    }
    
    @Configuration
    public static class TestConfig {
        @Bean
        PersistentTaskTestService persistentTaskTestService(List<SchedulerService> schedulers, TriggerService triggerService) {
            return new PersistentTaskTestService(schedulers, triggerService);
        }
        
        @Bean
        SchedulerService schedulerService(
                MeterRegistry meterRegistry,
                TriggerService triggerService,
                SchedulerThreadFactory threadFactory,
                EditSchedulerStatusComponent editSchedulerStatus,
                TransactionTemplate trx) {

            return SchedulerConfig.newSchedulerService("testScheduler", 
                    meterRegistry,
                    triggerService, 
                    editSchedulerStatus, 
                    threadFactory,
                    5, 
                    Duration.ofSeconds(0), 
                    trx); 
        }
    }

    protected Set<TriggerKey> waitForAllWorkflows() {
        return persistentTaskTestService.scheduleNextTriggersAndWait(Duration.ofSeconds(3));
    }
}
