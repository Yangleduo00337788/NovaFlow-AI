package ai.novaflow.server.config;

import ai.novaflow.workflow.service.WorkflowElRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
@Profile("!test")
@RequiredArgsConstructor
public class WorkflowElRefreshRunner implements ApplicationRunner {

    private final WorkflowElRefreshService workflowElRefreshService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            workflowElRefreshService.refreshPublishedWorkflowEl();
        } catch (Exception ex) {
            log.warn("Failed to refresh published workflow EL expressions: {}", ex.getMessage());
        }
    }
}
