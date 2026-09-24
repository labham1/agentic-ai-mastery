package com.agentic.ai;

import com.agentic.ai.graph.AgentState;
import com.agentic.ai.graph.StateGraph;
import com.agentic.ai.reflection.SelfHealingCompilerLab;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelfHealingCompilerTest {

    @Test
    @DisplayName("Should autonomously heal compilation errors via reflection loop and halt at deploy gate")
    void shouldSelfHealCompilationErrors() throws Exception {
        StateGraph graph = SelfHealingCompilerLab.buildGraph(3);

        AgentState state = new AgentState();
        state.set("USER_PROMPT", "Create a Calculator class");

        // Execute self-healing cycle
        state = graph.execute(state, 15);

        // Verification of cycle execution trajectory:
        // GENERATE_CODE (with bug) -> COMPILE_CODE (fails) -> REFLECT -> GENERATE_CODE (fixed) -> COMPILE_CODE (passes) -> pauses before DEPLOY_GATE
        assertThat(state.getExecutionHistory()).contains(
                "GENERATE_CODE",
                "COMPILE_CODE",
                "REFLECT",
                "GENERATE_CODE",
                "COMPILE_CODE"
        );

        // Verify compilation passed after reflection
        assertThat((Boolean) state.get("COMPILATION_PASSED")).isTrue();
        assertThat((String) state.get("SOURCE_CODE")).contains("return a + b;");

        // Verify HITL Gate is engaged
        assertThat(state.isWaitingForHuman()).isTrue();
        assertThat(state.getInterruptedAtNode()).isEqualTo("DEPLOY_GATE");

        // Resume with human approval
        AgentState finalState = graph.resume(state, "APPROVE", 5);
        assertThat(finalState.isWaitingForHuman()).isFalse();
        assertThat((String) finalState.get("STATUS")).isEqualTo("DEPLOYED_SUCCESSFULLY");
    }
}
