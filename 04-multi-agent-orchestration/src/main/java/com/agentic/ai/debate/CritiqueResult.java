package com.agentic.ai.debate;

import java.util.List;

/**
 * Structured critique schema returned by the Critic agent during peer review.
 */
public record CritiqueResult(
        int scoreOutOfTen,
        boolean approved,
        List<String> blockers,
        List<String> suggestions
) {
    public static CritiqueResult approve(int score, List<String> suggestions) {
        return new CritiqueResult(score, true, List.of(), suggestions);
    }

    public static CritiqueResult reject(int score, List<String> blockers, List<String> suggestions) {
        return new CritiqueResult(score, false, blockers, suggestions);
    }
}
