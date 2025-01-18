package org.sterl.pmw.spring.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.component.SimpleWorkflowStepExecutor;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.quartz.boundary.QuartzWorkflowService;
import org.sterl.pmw.quartz.job.QuartzWorkflowJobFactory;
import org.sterl.pmw.quartz.job.RetryDelayStrategy;

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
    @ConditionalOnMissingBean
    RetryDelayStrategy delayStrategy() {
        return RetryDelayStrategy.NO_DELAY;
    }

    @Bean
    QuartzWorkflowService quartzWorkflowService(
            ApplicationContext applicationContext,
            Scheduler scheduler,
            ObjectMapper mapper,
            RetryDelayStrategy delayStrategy,
            TransactionTemplate trx) throws SchedulerException {

        final QuartzWorkflowService quartzWorkflowService = new QuartzWorkflowService(scheduler, workflowRepository(), mapper);

        SpringBeanJobFactory jobFactory = null;
        if (enableSpringBeanJobFactory) {
            jobFactory = new SpringBeanJobFactory();
            jobFactory.setApplicationContext(applicationContext);
        } else {
            log.info("SpringBeanJobFactory disabled!");
        }

        scheduler.setJobFactory(new QuartzWorkflowJobFactory(
                new SimpleWorkflowStepExecutor(), quartzWorkflowService, 
                mapper, trx, 
                delayStrategy, jobFactory));

        return quartzWorkflowService;
    }


    @Bean
    SchedulerFactoryBeanCustomizer addExecuteInJTATransactionSupport(PlatformTransactionManager trxM) {
        return (sc) -> sc.setTransactionManager(trxM);
    }
}
