package com.agentic.ai.multiagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Supervisor Agent (Central Orchestrator).
 * 
 * Implements the Orchestrator-Worker pattern:
 * - Decomposes goals into specialized subtasks
 * - Delegates execution to isolated worker agents
 * - Consolidates outputs into an end-to-end deliverable
 */
public class SupervisorAgent {

    private static final Logger log = LoggerFactory.getLogger(SupervisorAgent.class);

    private final Map<String, AgentWorker> workers = new ConcurrentHashMap<>();
    private final List<WorkerReport> executionAuditLog = new ArrayList<>();

    public void registerWorker(AgentWorker worker) {
        workers.put(worker.getRole().roleName().toLowerCase(), worker);
        log.info("Supervisor registered worker role: [{}]", worker.getRole().roleName());
    }

    public WorkerReport delegate(String targetRole, String taskId, String instruction, Map<String, Object> context) {
        AgentWorker worker = workers.get(targetRole.toLowerCase());
        if (worker == null) {
            throw new IllegalArgumentException("No registered worker for role: " + targetRole + ". Available: " + workers.keySet());
        }

        TaskAssignment assignment = new TaskAssignment(taskId, targetRole, instruction, context);
        WorkerReport report = worker.executeTask(assignment);

        synchronized (executionAuditLog) {
            executionAuditLog.add(report);
        }

        return report;
    }

    public List<WorkerReport> getAuditLog() {
        synchronized (executionAuditLog) {
            return Collections.unmodifiableList(new ArrayList<>(executionAuditLog));
        }
    }

    public int activeWorkersCount() {
        return workers.size();
    }
}
