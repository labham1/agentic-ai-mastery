package com.agentic.ai;

import com.agentic.ai.multiagent.AgentRole;
import com.agentic.ai.multiagent.AgentWorker;
import com.agentic.ai.multiagent.SupervisorAgent;
import com.agentic.ai.multiagent.WorkerReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SupervisorOrchestrationTest {

    @Test
    @DisplayName("Should successfully delegate tasks to registered workers with scoped context")
    void shouldDelegateTasksSuccessfully() {
        SupervisorAgent supervisor = new SupervisorAgent();

        supervisor.registerWorker(new AgentWorker(
                new AgentRole("DataCollector", "Extracts raw metrics"),
                assignment -> WorkerReport.success(assignment.taskId(), "DataCollector", "Data collected",
                        Map.of("count", 100), 20)
        ));

        WorkerReport report = supervisor.delegate("DataCollector", "t-1", "Fetch count", Map.of());

        assertThat(report.success()).isTrue();
        assertThat(report.roleName()).isEqualTo("DataCollector");
        assertThat((Integer) report.outputArtifacts().get("count")).isEqualTo(100);
        assertThat(supervisor.getAuditLog()).hasSize(1);
    }

    @Test
    @DisplayName("Should throw exception when delegating to an unregistered role")
    void shouldThrowOnUnregisteredRole() {
        SupervisorAgent supervisor = new SupervisorAgent();

        assertThatThrownBy(() -> supervisor.delegate("NonExistentRole", "t-2", "Do work", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No registered worker for role");
    }
}
