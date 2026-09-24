package com.agentic.ai.evaluation;

import java.util.List;
import java.util.Set;

/**
 * Enterprise Agent Evaluator implementing the RAG & Agent Triad metrics:
 * 1. Faithfulness (Groundedness)
 * 2. Answer Relevance
 * 3. Tool Selection Precision
 */
public class LlmAsAJudgeEvaluator {

    /**
     * Evaluates an agent execution turn against reference contexts and expected tools.
     * 
     * @param userPrompt The original prompt submitted by the user
     * @param agentOutput The synthesized output produced by the agent
     * @param toolObservations The raw data returned by executed tools
     * @param executedTools Names of tools invoked by the agent
     * @param expectedTools The minimum required tools that should have been invoked
     * @return EvaluationResult with normalized [0.0 - 1.0] scores
     */
    public EvaluationResult evaluateTurn(
            String userPrompt,
            String agentOutput,
            List<String> toolObservations,
            List<String> executedTools,
            Set<String> expectedTools
    ) {
        // 1. Compute Tool Precision: ratio of expected tools actually called vs total called
        double toolPrecision = 1.0;
        if (!executedTools.isEmpty()) {
            long validCalls = executedTools.stream().filter(expectedTools::contains).count();
            toolPrecision = (double) validCalls / executedTools.size();
        } else if (!expectedTools.isEmpty()) {
            toolPrecision = 0.0;
        }

        // 2. Compute Faithfulness: verify that output does not contain ungrounded hallucinations
        // Checks if output facts correspond with tool observations
        double faithfulness = 1.0;
        if (!toolObservations.isEmpty()) {
            String combinedObs = String.join(" ", toolObservations).toLowerCase();
            String[] tokens = agentOutput.toLowerCase().split("\\s+");
            int matches = 0;
            for (String t : tokens) {
                if (t.length() > 3 && combinedObs.contains(t)) {
                    matches++;
                }
            }
            faithfulness = Math.min(1.0, (double) matches / Math.max(1, tokens.length / 2));
        }

        // 3. Compute Answer Relevance: checks overlap between user intent and output
        String[] queryTokens = userPrompt.toLowerCase().split("\\s+");
        int queryMatches = 0;
        for (String q : queryTokens) {
            if (q.length() > 3 && agentOutput.toLowerCase().contains(q)) {
                queryMatches++;
            }
        }
        double relevance = Math.min(1.0, (double) queryMatches / Math.max(1, queryTokens.length / 2));

        return EvaluationResult.create(faithfulness, relevance, toolPrecision, 0.70);
    }
}
