package com.agentic.ai.multiagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

/**
 * Autonomous Worker Agent.
 * Executes tasks strictly within its scoped domain persona and toolset.
 */
public class AgentWorker {

    private static final Logger log = LoggerFactory.getLogger(AgentWorker.class);

    private final AgentRole role;
    private final Function<TaskAssignment, WorkerReport> executionLogic;

    public AgentWorker(AgentRole role, Function<TaskAssignment, WorkerReport> executionLogic) {
        this.role = role;
        this.executionLogic = executionLogic;
    }

    public WorkerReport executeTask(TaskAssignment assignment) {
        long start = System.currentTimeMillis();
        log.info("[Worker: {}] Commencing task '{}': {}", role.roleName(), assignment.taskId(), assignment.instruction());

        try {
            WorkerReport report = executionLogic.apply(assignment);
            long duration = System.currentTimeMillis() - start;
            log.info("[Worker: {}] Finished task '{}' in {} ms (Success: {})",
                    role.roleName(), assignment.taskId(), duration, report.success());
            return report;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.error("[Worker: {}] Task failed with exception", role.roleName(), t);
            return WorkerReport.failure(assignment.taskId(), role.roleName(), t.getMessage(), duration);
        }
    }

    public AgentRole getRole() {
        return role;
    }
}
