package org.sterl.store.items.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.sterl.spring.persistent_tasks.scheduler.SchedulerService;
import org.sterl.spring.persistent_tasks.test.PersistentTaskTestService;
import org.sterl.spring.persistent_tasks.trigger.TriggerService;
import org.sterl.store.items.entity.Item;

@SpringBootTest
@ActiveProfiles("test")
class ItemServiceTest {

    @Autowired ItemService subject;
    @Autowired PersistentTaskTestService persistentTaskTestService;
    
    @TestConfiguration
    static class Config {
        @Bean
        PersistentTaskTestService persistentTaskTestService(List<SchedulerService> schedulers, TriggerService triggerService) {
            return new PersistentTaskTestService(schedulers, triggerService);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
    }

    @Test
    void test() {
        // GIVEN
        var item = Item.builder().name("Foo1").price(new BigDecimal("12.99")).build();

        // WHEN
        subject.createNewItem(item);
        persistentTaskTestService.scheduleNextTriggersAndWait();

        // THEN
        assertThat(subject.get(item.getId())).isPresent();
    }

}
