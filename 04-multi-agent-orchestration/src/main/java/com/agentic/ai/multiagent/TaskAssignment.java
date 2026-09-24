package com.agentic.ai.multiagent;

import java.util.Map;

/**
 * Scoped task assignment passed from the Supervisor to an individual Worker.
 * Enforces clean context boundaries (prevents context explosion).
 */
public record TaskAssignment(
        String taskId,
        String targetRole,
        String instruction,
        Map<String, Object> inputContext
) {}
