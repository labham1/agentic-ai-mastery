package com.agentic.ai.graph;

/**
 * Functional contract for conditional routing between nodes in a StateGraph.
 */
@FunctionalInterface
public interface ConditionalRouter {

    /**
     * Inspects the current state and returns the name of the next node to execute,
     * or StateGraph.END to terminate.
     * 
     * @param state The current graph state
     * @return Target node name
     */
    String route(AgentState state);
}
