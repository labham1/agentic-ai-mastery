package com.agentic.ai.core;

/**
 * Represents the outcome of an executed agent tool call.
 * Encapsulates success payload, error boundaries, and execution metrics.
 */
public record ToolExecutionResult(
        String toolName,
        boolean success,
        Object result,
        String errorMessage,
        long executionDurationMs
) {
    public static ToolExecutionResult success(String toolName, Object result, long durationMs) {
        return new ToolExecutionResult(toolName, true, result, null, durationMs);
    }

    public static ToolExecutionResult failure(String toolName, String errorMessage, long durationMs) {
        return new ToolExecutionResult(toolName, false, null, errorMessage, durationMs);
    }

    /**
     * Formats the observation string to be returned into the LLM conversation context.
     */
    public String toObservationString() {
        if (success) {
            return result != null ? result.toString() : "Success (null)";
        }
        return "Tool Execution Error: " + errorMessage;
    }
}
