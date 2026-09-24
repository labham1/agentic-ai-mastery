package com.agentic.ai.debate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Collaborative Debate Squad.
 * 
 * Implements the Peer-Review / Consensus pattern between an Author (Generator)
 * and a Critic (Evaluator).
 * 
 * Guarantees convergence using:
 * 1. Structured numerical scoring
 * 2. Strict maximum debate rounds ceiling
 */
public class CollaborativeDebateSquad {

    private static final Logger log = LoggerFactory.getLogger(CollaborativeDebateSquad.class);

    public record DebateRound(int roundNumber, String proposal, CritiqueResult critique) {}

    public record DebateOutcome(
            boolean consensusReached,
            int totalRounds,
            String finalArtifact,
            CritiqueResult finalCritique,
            List<DebateRound> trajectory
    ) {}

    private final BiFunction<String, CritiqueResult, String> authorGenerator;
    private final Function<String, CritiqueResult> criticEvaluator;
    private final int maxRounds;
    private final int consensusThresholdScore;

    public CollaborativeDebateSquad(
            BiFunction<String, CritiqueResult, String> authorGenerator,
            Function<String, CritiqueResult> criticEvaluator,
            int maxRounds,
            int consensusThresholdScore
    ) {
        this.authorGenerator = authorGenerator;
        this.criticEvaluator = criticEvaluator;
        this.maxRounds = maxRounds;
        this.consensusThresholdScore = consensusThresholdScore;
    }

    public DebateOutcome executeDebate(String initialTopic) {
        List<DebateRound> trajectory = new ArrayList<>();
        CritiqueResult previousCritique = null;
        String currentProposal = "";
        CritiqueResult currentCritique = null;

        log.info("Commencing collaborative debate on topic: '{}'", initialTopic);

        for (int round = 1; round <= maxRounds; round++) {
            log.info("[Debate Round {}/{}] Author generating revised proposal...", round, maxRounds);
            currentProposal = authorGenerator.apply(initialTopic, previousCritique);

            log.info("[Debate Round {}/{}] Critic reviewing proposal...", round, maxRounds);
            currentCritique = criticEvaluator.apply(currentProposal);

            trajectory.add(new DebateRound(round, currentProposal, currentCritique));
            log.info("[Debate Round {}/{}] Score: {}/10, Approved: {}, Blockers: {}",
                    round, maxRounds, currentCritique.scoreOutOfTen(),
                    currentCritique.approved(), currentCritique.blockers());

            if (currentCritique.approved() && currentCritique.scoreOutOfTen() >= consensusThresholdScore) {
                log.info("Consensus reached at round {} with score {}/10!", round, currentCritique.scoreOutOfTen());
                return new DebateOutcome(true, round, currentProposal, currentCritique, trajectory);
            }

            previousCritique = currentCritique;
        }

        log.warn("Debate reached maximum rounds ({}) without full consensus.", maxRounds);
        return new DebateOutcome(false, maxRounds, currentProposal, currentCritique, trajectory);
    }
}
