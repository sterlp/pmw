package org.sterl.pmw.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.sterl.pmw.boundary.WorkflowStatusOberserverTest.TestWorkflowObserver;
import org.sterl.pmw.component.LoggingWorkflowStatusObserver;
import org.sterl.pmw.spring.config.EnableWorkflows;
import org.sterl.pmw.spring.config.PmwBeanCustomizer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EnableWorkflows
@SpringBootApplication
public class SpringTestApp {
    public static void main(String[] args) {
        SpringApplication.run(SpringTestApp.class, args);
    }
    @Bean
    ObjectMapper mapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
    @Bean
    TestWorkflowObserver testWorkflowObserver() {
        return new TestWorkflowObserver();
    }
    @Bean
    PmwBeanCustomizer beanCustomizer() {
        return o -> o.addObserver(testWorkflowObserver()).addObserver(new LoggingWorkflowStatusObserver());
        
    }
}
