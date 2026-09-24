package com.agentic.ai.observability;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Aggregates observability spans into operational metrics (Tokens, Costs, Latency).
 */
public class MetricsCollector {

    private final Queue<AgentTraceSpan> completedSpans = new ConcurrentLinkedQueue<>();

    public void recordSpan(AgentTraceSpan span) {
        if (span != null) {
            completedSpans.add(span);
        }
    }

    public int totalRequests() {
        return completedSpans.size();
    }

    public int totalPromptTokens() {
        return completedSpans.stream().mapToInt(AgentTraceSpan::promptTokens).sum();
    }

    public int totalCompletionTokens() {
        return completedSpans.stream().mapToInt(AgentTraceSpan::completionTokens).sum();
    }

    public int totalTokens() {
        return totalPromptTokens() + totalCompletionTokens();
    }

    public double totalCostDollars() {
        return completedSpans.stream().mapToDouble(AgentTraceSpan::estimatedCostDollars).sum();
    }

    public double averageLatencyMs() {
        if (completedSpans.isEmpty()) return 0.0;
        return completedSpans.stream().mapToLong(AgentTraceSpan::durationMs).average().orElse(0.0);
    }

    public List<AgentTraceSpan> getSpans() {
        return Collections.unmodifiableList(new ArrayList<>(completedSpans));
    }

    public void clear() {
        completedSpans.clear();
    }
}
