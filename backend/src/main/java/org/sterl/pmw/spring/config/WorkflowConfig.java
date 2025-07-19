package org.sterl.pmw.spring.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.EnableWorkflows;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.spring.PersistentWorkflowService;
import org.sterl.spring.persistent_tasks.EnableSpringPersistentTasks;
import org.sterl.spring.persistent_tasks.PersistentTaskService;
import org.sterl.spring.persistent_tasks.task.TaskService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;

import lombok.extern.slf4j.Slf4j;

@EnableSpringPersistentTasks
@ComponentScan(basePackageClasses = EnableWorkflows.class)
@Configuration
@Slf4j
public class WorkflowConfig {

    @Bean
    PersistentWorkflowService persistentWorkflowService(PersistentTaskService pts,
            TriggerService ts,
            TaskService taskS,
            WorkflowRepository wr,
            Map<String, Workflow<?>> workflows) {
        
        var result = new PersistentWorkflowService(pts, ts, taskS, wr);
        
        workflows.forEach((k, w) -> result.register(k, w));
        
        return result;
    }
}
