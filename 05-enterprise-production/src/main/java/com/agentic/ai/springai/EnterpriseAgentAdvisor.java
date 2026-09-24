package com.agentic.ai.springai;

import com.agentic.ai.guardrails.PiiMasker;
import com.agentic.ai.guardrails.PromptInjectionGuard;
import com.agentic.ai.observability.AgentTraceSpan;
import com.agentic.ai.observability.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Function;

/**
 * Spring AI Enterprise Advisor / Interceptor.
 * 
 * Chains enterprise policies around agent calls:
 * [User Input] -> Prompt Injection Guard -> PII Masker -> Tracing Span Start ->
 * [Agent Invocation] -> Tracing Span Complete -> Metrics Recording -> [Response]
 */
public class EnterpriseAgentAdvisor {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseAgentAdvisor.class);

    private final MetricsCollector metricsCollector;

    public EnterpriseAgentAdvisor(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    public String executeAround(String userPrompt, Function<String, String> agentInvocation) {
        String traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        log.info("[Advisor: Start Trace {}] Validating security guardrails...", traceId);

        // 1. Guardrail: Prompt Injection Defense
        PromptInjectionGuard.validateInput(userPrompt);

        // 2. Guardrail: PII Masking
        PiiMasker.MaskingResult maskingResult = PiiMasker.maskPii(userPrompt);
        if (maskingResult.redactionCount() > 0) {
            log.warn("[Advisor: PII Guard] Redacted {} sensitive identifiers from input prompt",
                    maskingResult.redactionCount());
        }
        String sanitizedPrompt = maskingResult.sanitizedText();

        // 3. Invoke Model / Agent
        String response;
        String status = "OK";
        try {
            response = agentInvocation.apply(sanitizedPrompt);
        } catch (Exception e) {
            status = "ERROR: " + e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int estimatedPromptTokens = (int) Math.ceil(sanitizedPrompt.length() / 4.0);
            int estimatedOutputTokens = 50; // default baseline

            // 4. Record OpenTelemetry Trace Span
            AgentTraceSpan span = AgentTraceSpan.create(
                    traceId, spanId, "agent_chat_turn", "gemini-2.0-flash",
                    estimatedPromptTokens, estimatedOutputTokens, duration, status
            );
            metricsCollector.recordSpan(span);
            log.info("[Advisor: Trace Complete] Span {} finished in {} ms. Estimated Cost: ${}",
                    spanId, duration, String.format("%.6f", span.estimatedCostDollars()));
        }

        return response;
    }
}
