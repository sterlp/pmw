package org.sterl.pmw.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.sterl.pmw.component.SimpleWorkflowStepExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.spring.SpringWorkflowService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@ComponentScan(basePackageClasses = SpringWorkflowService.class)
@Configuration
@Slf4j
public class WorkflowConfig {

    @Bean
    WorkflowRepository workflowRepository() {
        return new WorkflowRepository();
    }
}
