package com.agentic.ai;

import com.agentic.ai.evaluation.EvaluationResult;
import com.agentic.ai.evaluation.LlmAsAJudgeEvaluator;
import com.agentic.ai.observability.AgentTraceSpan;
import com.agentic.ai.observability.MetricsCollector;
import com.agentic.ai.springai.EnterpriseAgentAdvisor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservabilityAndEvaluationTest {

    @Test
    @DisplayName("Should aggregate OpenTelemetry spans, token metrics and dollar cost")
    void shouldTrackMetricsAndCosts() {
        MetricsCollector collector = new MetricsCollector();

        AgentTraceSpan s1 = AgentTraceSpan.create("t-1", "s-1", "chat", "gemini-2.0-flash", 1000, 200, 250, "OK");
        AgentTraceSpan s2 = AgentTraceSpan.create("t-2", "s-2", "chat", "gemini-2.0-flash", 2000, 500, 400, "OK");

        collector.recordSpan(s1);
        collector.recordSpan(s2);

        assertThat(collector.totalRequests()).isEqualTo(2);
        assertThat(collector.totalPromptTokens()).isEqualTo(3000);
        assertThat(collector.totalCompletionTokens()).isEqualTo(700);
        assertThat(collector.totalTokens()).isEqualTo(3700);
        assertThat(collector.averageLatencyMs()).isEqualTo(325.0);
        assertThat(collector.totalCostDollars()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should execute Advisor pipeline redacting PII and recording trace span")
    void shouldExecuteAdvisorPipeline() {
        MetricsCollector collector = new MetricsCollector();
        EnterpriseAgentAdvisor advisor = new EnterpriseAgentAdvisor(collector);

        String result = advisor.executeAround(
                "My email is test@company.com and I need my account summary",
                sanitizedPrompt -> {
                    assertThat(sanitizedPrompt).contains("[REDACTED_EMAIL]");
                    return "Account summary generated.";
                }
        );

        assertThat(result).isEqualTo("Account summary generated.");
        assertThat(collector.totalRequests()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should compute RAG Triad evaluation metrics correctly")
    void shouldComputeEvaluationMetrics() {
        LlmAsAJudgeEvaluator evaluator = new LlmAsAJudgeEvaluator();

        EvaluationResult result = evaluator.evaluateTurn(
                "Find Apple revenue for Q3",
                "Apple Q3 revenue was 85 billion dollars as verified by SEC filings",
                List.of("Apple Q3 revenue 85 billion dollars filed"),
                List.of("queryDatabase"),
                Set.of("queryDatabase")
        );

        assertThat(result.toolPrecisionScore()).isEqualTo(1.0);
        assertThat(result.faithfulnessScore()).isGreaterThan(0.5);
        assertThat(result.answerRelevanceScore()).isGreaterThan(0.5);
        assertThat(result.passedThresholds()).isTrue();
    }
}
