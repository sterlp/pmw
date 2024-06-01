package org.sterl.pmw.spring.component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.hibernate.LockOptions;
import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.spring.model.TaskEntity;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringWorkflowSchedulerComponent {

    @PersistenceContext
    private final EntityManager entityManager;
    private final SpringWorkflowExecutorComponent workflowExecutor;
    private final Duration checkTime = Duration.ofSeconds(1);
    private final TransactionTemplate trx;
    final AtomicBoolean isRunnung = new AtomicBoolean(false);

    private ExecutorService queueWorkflowsRunner;
    
    
    @PostConstruct
    public synchronized void start() throws InterruptedException {
        stop();
        isRunnung.set(true);
        queueWorkflowsRunner = Executors.newFixedThreadPool(1,
                new BasicThreadFactory.Builder()
                .namingPattern("PMW-Main")
                .priority(Thread.NORM_PRIORITY - 1)
                .build());
        
        queueWorkflowsRunner.execute(new Worker(this, isRunnung, checkTime.toMillis()));
        log.info("SpringWorkflowScheduler started. Checking every {} for new workflows.", checkTime);
        
    }
    @PreDestroy
    public synchronized void stop() throws InterruptedException {
        isRunnung.set(false);
        if (queueWorkflowsRunner != null) {          
            queueWorkflowsRunner.shutdown();
            try {                
                queueWorkflowsRunner.awaitTermination(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                queueWorkflowsRunner.shutdownNow();
            } finally {                
                queueWorkflowsRunner = null;
            }
        }
    }
    
    public int runPendingWorkflows() {
        return trx.execute((t) -> {
            List<TaskEntity> pendingWorkflows = entityManager.createQuery(
                """
                SELECT e FROM PersistentWorkflowState
                WHERE e.state = :state
                ORDER BY ID ASC
                """, TaskEntity.class)
                    .setParameter("status", WorkflowStatus.PENDING)
                    .setMaxResults(5)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, LockOptions.NO_WAIT)
                    .getResultList();
            
            workflowExecutor.run(pendingWorkflows);
            return pendingWorkflows.size();
        });
    }
    
    @RequiredArgsConstructor
    @Slf4j
    private static class Worker implements Runnable {
        private final SpringWorkflowSchedulerComponent scheduler;
        private final AtomicBoolean isRunnung;
        private final long sleepTime;
        @Override
        public void run() {
            try {
                while (isRunnung.get()) {
                    try {
                        scheduler.runPendingWorkflows();                        
                    } catch (Exception e) {
                        log.warn("runPendingWorkflows failed.", e);
                    }

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // okay
                    }
                }
            } finally {
                isRunnung.set(false);
            }
            
        }
    }
}
