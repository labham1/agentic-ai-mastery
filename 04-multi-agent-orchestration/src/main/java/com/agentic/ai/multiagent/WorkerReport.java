package com.agentic.ai.multiagent;

import java.util.Map;

/**
 * Structured report returned by a Worker to the Supervisor upon task completion.
 */
public record WorkerReport(
        String taskId,
        String roleName,
        boolean success,
        String outputSummary,
        Map<String, Object> outputArtifacts,
        long executionDurationMs
) {
    public static WorkerReport success(String taskId, String roleName, String summary, Map<String, Object> artifacts, long duration) {
        return new WorkerReport(taskId, roleName, true, summary, artifacts, duration);
    }

    public static WorkerReport failure(String taskId, String roleName, String error, long duration) {
        return new WorkerReport(taskId, roleName, false, "Error: " + error, Map.of(), duration);
    }
}
