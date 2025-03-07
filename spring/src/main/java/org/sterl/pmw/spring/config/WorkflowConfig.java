package org.sterl.pmw.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.EnableWorkflows;
import org.sterl.pmw.component.WorkflowRepository;

import lombok.extern.slf4j.Slf4j;

@ComponentScan(basePackageClasses = EnableWorkflows.class)
@Configuration
@Slf4j
public class WorkflowConfig {

    @Bean
    WorkflowRepository workflowRepository() {
        return new WorkflowRepository();
    }
}
