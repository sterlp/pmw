package org.sterl.pmw.spring.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.sterl.pmw.component.WorkflowRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RepositoryHealth implements HealthIndicator {

    private final WorkflowRepository workflowRepository;
    @Override
    public Health health() {
        Builder result = workflowRepository.hasWorkflows() ? Health.up() : Health.down();
        result.withDetail("workflows", workflowRepository.getWorkflowNames());
        return result.build();
    }

}
