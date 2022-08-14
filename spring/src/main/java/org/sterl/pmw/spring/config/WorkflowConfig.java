package org.sterl.pmw.spring.config;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepStrategy;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;
import org.sterl.pmw.quartz.job.QuartzWorkflowJobFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@ComponentScan(basePackageClasses = WorkflowConfig.class)
@Configuration
@Slf4j
public class WorkflowConfig {

    @Value("${spring.pmw.spring-bean-job-factory.enabled:true}")
    private boolean enableSpringBeanJobFactory = true;
    
    @Bean
    WorkflowRepository workflowRepository() {
        return new WorkflowRepository();
    }

    @Bean
    QuartzWorkflowService quartzWorkflowService(Scheduler scheduler, ObjectMapper mapper) {
        return new QuartzWorkflowService(scheduler, workflowRepository(), mapper);
    }

    @Bean
    SchedulerFactoryBeanCustomizer registerPwm(
            ApplicationContext applicationContext, ObjectMapper mapper, TransactionTemplate trx) {

        final SpringBeanJobFactory jobFactory = enableSpringBeanJobFactory ? new SpringBeanJobFactory() : null;
        if (enableSpringBeanJobFactory) {
            jobFactory.setApplicationContext(applicationContext);
        } else {
            log.info("SpringBeanJobFactory disabled!");
        }

        return (sf) -> {
            sf.setJobFactory(new QuartzWorkflowJobFactory(
                    new SimpleWorkflowStepStrategy(), workflowRepository(), mapper, trx, jobFactory));
        };
    }
    
    @Bean
    SchedulerFactoryBeanCustomizer addExecuteInJTATransactionSupport(PlatformTransactionManager trxM) {
        return (sc) -> sc.setTransactionManager(trxM);
    }
}
