package com.agentic.ai;

import com.agentic.ai.debate.CollaborativeDebateSquad;
import com.agentic.ai.debate.CritiqueResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CollaborativeDebateTest {

    @Test
    @DisplayName("Should reach consensus when critique score meets or exceeds threshold")
    void shouldReachConsensus() {
        CollaborativeDebateSquad squad = new CollaborativeDebateSquad(
                (topic, prevCritique) -> prevCritique == null ? "Initial Draft" : "Polished Draft",
                proposal -> {
                    if ("Initial Draft".equals(proposal)) {
                        return CritiqueResult.reject(5, List.of("Lacks error handling"), List.of("Add try-catch"));
                    }
                    return CritiqueResult.approve(9, List.of("Excellent design"));
                },
                3,
                8
        );

        var outcome = squad.executeDebate("Microservice Architecture");

        assertThat(outcome.consensusReached()).isTrue();
        assertThat(outcome.totalRounds()).isEqualTo(2);
        assertThat(outcome.finalCritique().scoreOutOfTen()).isEqualTo(9);
        assertThat(outcome.finalArtifact()).isEqualTo("Polished Draft");
    }

    @Test
    @DisplayName("Should terminate gracefully when max rounds are exhausted without consensus")
    void shouldTerminateOnMaxRounds() {
        CollaborativeDebateSquad squad = new CollaborativeDebateSquad(
                (topic, prev) -> "Stubborn Proposal",
                proposal -> CritiqueResult.reject(4, List.of("Fundamental flaw"), List.of("Re-architect")),
                2,
                8
        );

        var outcome = squad.executeDebate("High-Risk Proposal");

        assertThat(outcome.consensusReached()).isFalse();
        assertThat(outcome.totalRounds()).isEqualTo(2);
        assertThat(outcome.trajectory()).hasSize(2);
    }
}
