package org.sterl.pmw.testapp;

import java.time.Duration;
import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.spring.PersistentWorkflowService;
import org.sterl.pmw.testapp.item.repository.ItemRepository;
import org.sterl.spring.persistent_tasks.api.TriggerKey;
import org.sterl.spring.persistent_tasks.scheduler.SchedulerService;
import org.sterl.spring.persistent_tasks.test.AsyncAsserts;
import org.sterl.spring.persistent_tasks.test.PersistentTaskTestService;

@SpringBootTest
@ComponentScan(basePackageClasses = SpringTestApp.class)
public abstract class AbstractSpringTest {

    @Autowired
    protected PersistentWorkflowService workflowService;
    @Autowired
    protected ItemRepository itemRepository;
    @Autowired
    protected SchedulerService schedulerService;
    @Autowired
    protected PersistentTaskTestService persistentTaskTestService;
    @Autowired
    protected WorkflowUmlService umlService;
    @Autowired
    protected AsyncAsserts asserts;

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

    protected Set<TriggerKey> waitForAllWorkflows() {
        return persistentTaskTestService.scheduleNextTriggersAndWait(Duration.ofSeconds(3));
    }
    
    void register(Workflow<?> w) {
        workflowService.register(w.getName(), w);
    }
}
