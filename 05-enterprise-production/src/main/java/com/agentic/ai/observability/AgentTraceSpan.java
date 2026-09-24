package com.agentic.ai.observability;

/**
 * OpenTelemetry-compliant GenAI Trace Span.
 * Captures token counts, model attributes, latency, and estimated cost for every agent step.
 */
public record AgentTraceSpan(
        String traceId,
        String spanId,
        String operationName,
        String modelName,
        int promptTokens,
        int completionTokens,
        long durationMs,
        double estimatedCostDollars,
        String status
) {
    /**
     * Estimated cost calculation based on typical cloud pricing
     * (e.g. $0.075 per 1M input tokens, $0.30 per 1M output tokens for Flash-tier models).
     */
    public static double calculateCost(int promptTokens, int completionTokens) {
        double inputCost = (promptTokens / 1_000_000.0) * 0.075;
        double outputCost = (completionTokens / 1_000_000.0) * 0.30;
        return inputCost + outputCost;
    }

    public static AgentTraceSpan create(String traceId, String spanId, String operation, String model,
                                         int promptTokens, int completionTokens, long durationMs, String status) {
        double cost = calculateCost(promptTokens, completionTokens);
        return new AgentTraceSpan(traceId, spanId, operation, model, promptTokens, completionTokens, durationMs, cost, status);
    }
}
