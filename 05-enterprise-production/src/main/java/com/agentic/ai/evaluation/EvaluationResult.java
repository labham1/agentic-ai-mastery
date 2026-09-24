package com.agentic.ai.evaluation;

/**
 * Results of an LLM-as-a-Judge evaluation across the RAG & Agent Triad metrics.
 */
public record EvaluationResult(
        double faithfulnessScore,
        double answerRelevanceScore,
        double toolPrecisionScore,
        boolean passedThresholds,
        String evaluationSummary
) {
    public static EvaluationResult create(double faithfulness, double relevance, double toolPrecision, double minThreshold) {
        boolean passed = faithfulness >= minThreshold && relevance >= minThreshold && toolPrecision >= minThreshold;
        String summary = String.format("Faithfulness: %.2f | Relevance: %.2f | Tool Precision: %.2f (Status: %s)",
                faithfulness, relevance, toolPrecision, passed ? "PASSED" : "FAILED");
        return new EvaluationResult(faithfulness, relevance, toolPrecision, passed, summary);
    }
}
