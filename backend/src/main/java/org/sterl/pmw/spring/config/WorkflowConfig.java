package org.sterl.pmw.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.EnableWorkflows;
import org.sterl.pmw.WorkflowUmlService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.spring.persistent_tasks.EnableSpringPersistentTasks;

import lombok.extern.slf4j.Slf4j;

@EnableSpringPersistentTasks
@ComponentScan(basePackageClasses = EnableWorkflows.class)
@Configuration
@Slf4j
public class WorkflowConfig {

    @Bean
    WorkflowRepository workflowRepository() {
        return new WorkflowRepository();
    }
    
    @Bean
    WorkflowUmlService workflowUmlService() {
        return new WorkflowUmlService(workflowRepository());
    }
}
