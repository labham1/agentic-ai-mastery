package com.agentic.ai.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise StateGraph Engine.
 * 
 * Supports:
 * 1. Cyclic execution topologies
 * 2. Deterministic & Conditional branching
 * 3. Upper-bound iteration ceilings (Circuit Breakers)
 * 4. Human-in-the-Loop (HITL) Checkpoint Gates
 */
public class StateGraph {

    private static final Logger log = LoggerFactory.getLogger(StateGraph.class);

    public static final String START = "__START__";
    public static final String END = "__END__";

    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, String> directEdges = new ConcurrentHashMap<>();
    private final Map<String, ConditionalRouter> conditionalEdges = new ConcurrentHashMap<>();
    private final Set<String> humanCheckpointGates = Collections.synchronizedSet(new HashSet<>());
    private String entryPointNode = null;

    public StateGraph addNode(String name, Node node) {
        if (START.equals(name) || END.equals(name)) {
            throw new IllegalArgumentException("Cannot override reserved node name: " + name);
        }
        nodes.put(name, node);
        return this;
    }

    public StateGraph addEdge(String from, String to) {
        directEdges.put(from, to);
        return this;
    }

    public StateGraph addConditionalEdge(String from, ConditionalRouter router) {
        conditionalEdges.put(from, router);
        return this;
    }

    public StateGraph setEntryPoint(String nodeName) {
        this.entryPointNode = nodeName;
        return this;
    }

    /**
     * Designates a node as a Human-in-the-Loop gate.
     * Execution will pause before executing this node, waiting for human approval.
     */
    public StateGraph setCheckpointGate(String nodeName) {
        humanCheckpointGates.add(nodeName);
        return this;
    }

    /**
     * Executes the graph starting from the entry point until reaching END,
     * hitting a Human Checkpoint Gate, or exceeding maxSteps.
     */
    public AgentState execute(AgentState state, int maxSteps) throws Exception {
        if (entryPointNode == null) {
            throw new IllegalStateException("Graph entry point is not set!");
        }

        String currentNode = entryPointNode;
        int stepCount = 0;

        while (currentNode != null && !END.equals(currentNode)) {
            if (++stepCount > maxSteps) {
                log.warn("Circuit Breaker triggered: Graph exceeded maximum steps ({})", maxSteps);
                state.set("STATUS", "CIRCUIT_BREAKER_HALTED");
                break;
            }

            // Check if currentNode is a Human Gate
            if (humanCheckpointGates.contains(currentNode) && !state.has("__HUMAN_DECISION__")) {
                log.info("[HITL Gate Reached]: Pausing execution before node [{}] for human review.", currentNode);
                state.setWaitingForHuman(true, currentNode);
                return state;
            }

            // Clean one-time human decision flag if present
            state.set("__HUMAN_DECISION__", null);

            log.info("Executing Graph Node: [{}] (Step {}/{})", currentNode, stepCount, maxSteps);
            state.recordStep(currentNode);

            Node node = nodes.get(currentNode);
            if (node == null) {
                throw new IllegalStateException("Node not found in graph registry: " + currentNode);
            }

            // Execute node
            state = node.execute(state);

            // Determine next node
            if (conditionalEdges.containsKey(currentNode)) {
                ConditionalRouter router = conditionalEdges.get(currentNode);
                currentNode = router.route(state);
            } else if (directEdges.containsKey(currentNode)) {
                currentNode = directEdges.get(currentNode);
            } else {
                // No outgoing edge means termination
                currentNode = END;
            }
        }

        log.info("Graph execution concluded. Visited nodes: {}", state.getExecutionHistory());
        return state;
    }

    /**
     * Resumes execution of an interrupted graph state following a human decision.
     */
    public AgentState resume(AgentState pausedState, String humanDecision, int maxSteps) throws Exception {
        if (!pausedState.isWaitingForHuman()) {
            throw new IllegalStateException("Cannot resume state: graph was not waiting for human input.");
        }

        String targetNode = pausedState.getInterruptedAtNode();
        pausedState.setWaitingForHuman(false, null);
        pausedState.set("__HUMAN_DECISION__", humanDecision);

        log.info("[HITL Resumed]: Resuming node [{}] with decision: [{}]", targetNode, humanDecision);
        
        // Temporarily adjust entry point to the paused node
        String originalEntry = this.entryPointNode;
        this.entryPointNode = targetNode;
        try {
            return execute(pausedState, maxSteps);
        } finally {
            this.entryPointNode = originalEntry;
        }
    }
}
