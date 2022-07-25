package org.sterl.pmw.spring.config;

import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;
import org.sterl.pmw.quartz.job.QuartzWorkflowJobFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ComponentScan(basePackageClasses = WorkflowConfig.class)
@Configuration
public class WorkflowConfig {

    @Bean
    public WorkflowRepository workflowRepository() {
        return new WorkflowRepository();
    }
    @Bean
    public QuartzWorkflowService quartzWorkflowService(Scheduler scheduler, ObjectMapper mapper) {
        return new QuartzWorkflowService(scheduler, workflowRepository(), mapper);
    }
    @Bean
    public SchedulerFactoryBeanCustomizer registerPwm(
            ApplicationContext applicationContext, ObjectMapper mapper) {
        
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        return (sf) -> {
            sf.setJobFactory(new QuartzWorkflowJobFactory(
                    new SimpleWorkflowStepStrategy(), workflowRepository(), mapper, jobFactory));
        };
    }
}
