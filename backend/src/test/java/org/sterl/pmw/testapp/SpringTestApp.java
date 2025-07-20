package org.sterl.pmw.testapp;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.EnableWorkflows;
import org.sterl.spring.persistent_tasks.EnableSpringPersistentTasks;
import org.sterl.spring.persistent_tasks.scheduler.SchedulerService;
import org.sterl.spring.persistent_tasks.scheduler.component.EditSchedulerStatusComponent;
import org.sterl.spring.persistent_tasks.scheduler.config.SchedulerConfig;
import org.sterl.spring.persistent_tasks.scheduler.config.SchedulerThreadFactory;
import org.sterl.spring.persistent_tasks.test.AsyncAsserts;
import org.sterl.spring.persistent_tasks.test.PersistentTaskTestService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.micrometer.core.instrument.MeterRegistry;

@EnableWorkflows
@EnableSpringPersistentTasks
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
    PersistentTaskTestService persistentTaskTestService(List<SchedulerService> schedulers, TriggerService triggerService) {
        return new PersistentTaskTestService(schedulers, triggerService);
    }
    @Bean
    AsyncAsserts asserts() {
        System.err.println("AsyncAsserts -> :-(");
        return new AsyncAsserts();
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
