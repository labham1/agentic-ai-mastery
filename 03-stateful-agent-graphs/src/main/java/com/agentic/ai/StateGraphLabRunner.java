package com.agentic.ai;

import com.agentic.ai.graph.AgentState;
import com.agentic.ai.graph.StateGraph;
import com.agentic.ai.hitl.CheckpointManager;
import com.agentic.ai.hitl.HumanDecision;
import com.agentic.ai.reflection.SelfHealingCompilerLab;

import java.util.Scanner;

/**
 * Interactive Lab Runner for Module 03: Stateful Graphs & Reflection.
 * 
 * Demonstrates:
 * 1. Cyclic Graph Execution
 * 2. Real-time In-Memory Java Compilation & Diagnostic Error Feedback
 * 3. Autonomous Reflection & Self-Correction (Fixing syntax errors)
 * 4. Human-in-the-Loop Checkpoint Gate & Graph Resume
 */
public class StateGraphLabRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("""
            ================================================================
               AGENTIC AI MASTERY - MODULE 03: STATEFUL GRAPHS & REFLECTION
            ================================================================
            """);

        StateGraph graph = SelfHealingCompilerLab.buildGraph(3);
        CheckpointManager checkpointManager = new CheckpointManager();

        AgentState state = new AgentState();
        state.set("USER_PROMPT", "Write a high-performance Java Calculator class");

        System.out.println("Starting Autonomous Self-Healing Graph Execution...\n");
        state = graph.execute(state, 15);

        System.out.println("\nExecution History: " + state.getExecutionHistory());
        System.out.println("Final Code in State:\n" + state.get("SOURCE_CODE"));

        if (state.isWaitingForHuman()) {
            String pausedNode = state.getInterruptedAtNode();
            String checkpointId = "cp-" + System.currentTimeMillis();
            checkpointManager.saveCheckpoint(checkpointId, state);

            System.out.println("\n" + "=".repeat(64));
            System.out.println(" [!] HUMAN-IN-THE-LOOP CHECKPOINT REACHED");
            System.out.println(" Node: " + pausedNode);
            System.out.println(" Checkpoint Snapshot ID: " + checkpointId);
            System.out.println(" Proposed Action: Deploy validated Calculator.class to production");
            System.out.println("=".repeat(64));

            System.out.print("\nHuman Reviewer Action (Type 'APPROVE' or 'REJECT') [Default: APPROVE]: ");
            Scanner scanner = new Scanner(System.in);
            String input = "APPROVE";
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) input = line.toUpperCase();
            }

            if ("APPROVE".equalsIgnoreCase(input)) {
                System.out.println("\n[+] Resuming graph from checkpoint with APPROVAL...");
                state = graph.resume(state, HumanDecision.APPROVE.name(), 5);
                System.out.println("[✓] Workflow Concluded! Status: " + state.get("STATUS"));
                System.out.println("Full Node Trajectory: " + state.getExecutionHistory());
            } else {
                System.out.println("[-] Deployment rejected by human operator. Aborting.");
            }
        }
    }
}
