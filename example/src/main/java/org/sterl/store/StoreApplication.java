package org.sterl.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.sterl.pmw.EnableWorkflows;
import org.sterl.spring.persistent_tasks_ui.EnableSpringPersistentTasksUI;

@EnableWorkflows
@EnableSpringPersistentTasksUI
@SpringBootApplication
public class StoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }

}
