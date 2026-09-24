package com.agentic.ai;

import com.agentic.ai.graph.AgentState;
import com.agentic.ai.graph.StateGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StateGraphEngineTest {

    @Test
    @DisplayName("Should execute direct linear graph sequence")
    void shouldExecuteLinearGraph() throws Exception {
        StateGraph graph = new StateGraph();

        graph.addNode("A", state -> state.set("A_VISITED", true));
        graph.addNode("B", state -> state.set("B_VISITED", true));

        graph.setEntryPoint("A");
        graph.addEdge("A", "B");
        graph.addEdge("B", StateGraph.END);

        AgentState result = graph.execute(new AgentState(), 10);

        assertThat(result.getExecutionHistory()).containsExactly("A", "B");
        assertThat((Boolean) result.get("A_VISITED")).isTrue();
        assertThat((Boolean) result.get("B_VISITED")).isTrue();
    }

    @Test
    @DisplayName("Should conditionally route based on state inspection")
    void shouldRouteConditionally() throws Exception {
        StateGraph graph = new StateGraph();

        graph.addNode("CHECK_BALANCE", state -> state);
        graph.addNode("APPROVE_LOAN", state -> state.set("LOAN_STATUS", "APPROVED"));
        graph.addNode("REJECT_LOAN", state -> state.set("LOAN_STATUS", "REJECTED"));

        graph.setEntryPoint("CHECK_BALANCE");
        graph.addConditionalEdge("CHECK_BALANCE", state -> {
            int score = state.getOrDefault("CREDIT_SCORE", 0);
            return score >= 700 ? "APPROVE_LOAN" : "REJECT_LOAN";
        });
        graph.addEdge("APPROVE_LOAN", StateGraph.END);
        graph.addEdge("REJECT_LOAN", StateGraph.END);

        // Case 1: High score
        AgentState s1 = new AgentState().set("CREDIT_SCORE", 750);
        AgentState r1 = graph.execute(s1, 10);
        assertThat(r1.getExecutionHistory()).containsExactly("CHECK_BALANCE", "APPROVE_LOAN");
        assertThat((String) r1.get("LOAN_STATUS")).isEqualTo("APPROVED");

        // Case 2: Low score
        AgentState s2 = new AgentState().set("CREDIT_SCORE", 620);
        AgentState r2 = graph.execute(s2, 10);
        assertThat(r2.getExecutionHistory()).containsExactly("CHECK_BALANCE", "REJECT_LOAN");
        assertThat((String) r2.get("LOAN_STATUS")).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Should halt execution when max steps circuit breaker threshold is exceeded")
    void shouldTriggerCircuitBreakerOnInfiniteLoop() throws Exception {
        StateGraph graph = new StateGraph();

        // Deliberate cycle A -> B -> A
        graph.addNode("A", state -> state);
        graph.addNode("B", state -> state);

        graph.setEntryPoint("A");
        graph.addEdge("A", "B");
        graph.addEdge("B", "A");

        AgentState result = graph.execute(new AgentState(), 5);

        assertThat(result.getExecutionHistory()).hasSize(5);
        assertThat((String) result.get("STATUS")).isEqualTo("CIRCUIT_BREAKER_HALTED");
    }

    @Test
    @DisplayName("Should pause at HITL checkpoint and resume with decision")
    void shouldPauseAndResumeAtHumanGate() throws Exception {
        StateGraph graph = new StateGraph();

        graph.addNode("STEP_1", state -> state.set("STAGE", 1));
        graph.addNode("SENSITIVE_DEPLOY", state -> state.set("STAGE", 2));

        graph.setEntryPoint("STEP_1");
        graph.addEdge("STEP_1", "SENSITIVE_DEPLOY");
        graph.addEdge("SENSITIVE_DEPLOY", StateGraph.END);
        graph.setCheckpointGate("SENSITIVE_DEPLOY");

        // First run pauses before SENSITIVE_DEPLOY
        AgentState state = graph.execute(new AgentState(), 10);
        assertThat(state.isWaitingForHuman()).isTrue();
        assertThat(state.getInterruptedAtNode()).isEqualTo("SENSITIVE_DEPLOY");
        assertThat(state.getExecutionHistory()).containsExactly("STEP_1");

        // Resume run advances past gate
        AgentState resumed = graph.resume(state, "APPROVE", 10);
        assertThat(resumed.isWaitingForHuman()).isFalse();
        assertThat(resumed.getExecutionHistory()).containsExactly("STEP_1", "SENSITIVE_DEPLOY");
        assertThat((Integer) resumed.get("STAGE")).isEqualTo(2);
    }
}
