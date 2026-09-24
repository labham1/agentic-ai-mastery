package com.agentic.ai.graph;

/**
 * Functional contract for a single computational or decision node within a StateGraph.
 */
@FunctionalInterface
public interface Node {

    /**
     * Executes the node's business logic, reads state, and returns modified state.
     * 
     * @param state The current snapshot of shared graph state
     * @return The updated state
     * @throws Exception If an unhandled execution error occurs
     */
    AgentState execute(AgentState state) throws Exception;
}
