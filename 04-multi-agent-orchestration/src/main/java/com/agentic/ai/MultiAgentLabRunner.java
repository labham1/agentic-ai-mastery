package com.agentic.ai;

import com.agentic.ai.debate.CollaborativeDebateSquad;
import com.agentic.ai.debate.CritiqueResult;
import com.agentic.ai.mcp.McpClient;
import com.agentic.ai.mcp.McpMessage;
import com.agentic.ai.mcp.McpToolDefinition;
import com.agentic.ai.multiagent.AgentRole;
import com.agentic.ai.multiagent.AgentWorker;
import com.agentic.ai.multiagent.SupervisorAgent;
import com.agentic.ai.multiagent.WorkerReport;

import java.util.List;
import java.util.Map;

/**
 * Interactive Lab Runner for Module 04: Multi-Agent Orchestration, Debate & MCP.
 * 
 * Demonstrates:
 * 1. Autonomous Software Engineering Squad (Supervisor -> Researcher -> Developer -> QA)
 * 2. Collaborative Peer-Review Debate with Consensus Convergence
 * 3. Standard Model Context Protocol (MCP) JSON-RPC 2.0 Discovery & Execution
 */
public class MultiAgentLabRunner {

    public static void main(String[] args) {
        System.out.println("""
            ================================================================
               AGENTIC AI MASTERY - MODULE 04: MULTI-AGENT & MCP LAB
            ================================================================
            """);

        demoSupervisorSquad();
        System.out.println("\n----------------------------------------------------------------\n");

        demoCollaborativeDebate();
        System.out.println("\n----------------------------------------------------------------\n");

        demoMcpProtocol();
        System.out.println("\n================================================================");
    }

    private static void demoSupervisorSquad() {
        System.out.println("LAB 1: Autonomous Software Engineering Squad (Supervisor Pattern)");

        SupervisorAgent supervisor = new SupervisorAgent();

        // 1. Researcher Worker
        supervisor.registerWorker(new AgentWorker(
                new AgentRole("Researcher", "Domain analyst specializing in enterprise architecture specs"),
                assignment -> {
                    String spec = "Tech Brief: Requires a thread-safe PaymentGatewayService interface with authorize() and capture().";
                    return WorkerReport.success(assignment.taskId(), "Researcher", spec, Map.of("spec", spec), 45);
                }
        ));

        // 2. Developer Worker
        supervisor.registerWorker(new AgentWorker(
                new AgentRole("Developer", "Senior Java backend engineer"),
                assignment -> {
                    String spec = (String) assignment.inputContext().get("spec");
                    String code = """
                        public interface PaymentGatewayService {
                            boolean authorize(String transactionId, double amount);
                            boolean capture(String transactionId);
                        }
                        """;
                    return WorkerReport.success(assignment.taskId(), "Developer", "Implemented PaymentGatewayService interface",
                            Map.of("code", code), 75);
                }
        ));

        // 3. QA Engineer Worker
        supervisor.registerWorker(new AgentWorker(
                new AgentRole("QA_Engineer", "Test automation specialist"),
                assignment -> {
                    String code = (String) assignment.inputContext().get("code");
                    String testCode = """
                        @Test
                        void shouldAuthorizePayment() {
                            PaymentGatewayService service = mock(PaymentGatewayService.class);
                            when(service.authorize("TX-1", 100.0)).thenReturn(true);
                            assertThat(service.authorize("TX-1", 100.0)).isTrue();
                        }
                        """;
                    return WorkerReport.success(assignment.taskId(), "QA_Engineer", "Unit tests generated and verified (100% coverage)",
                            Map.of("tests", testCode), 60);
                }
        ));

        // Supervisor orchestrates execution pipeline
        System.out.println("[Supervisor] Step 1: Delegating research task to Researcher Worker...");
        WorkerReport r1 = supervisor.delegate("Researcher", "task-1", "Analyze payment gateway requirements", Map.of());
        System.out.println("   --> Received: " + r1.outputSummary());

        System.out.println("\n[Supervisor] Step 2: Passing research brief to Developer Worker...");
        WorkerReport r2 = supervisor.delegate("Developer", "task-2", "Write Java interface", r1.outputArtifacts());
        System.out.println("   --> Received: " + r2.outputSummary());

        System.out.println("\n[Supervisor] Step 3: Passing code artifact to QA Worker for test generation...");
        WorkerReport r3 = supervisor.delegate("QA_Engineer", "task-3", "Write unit test suite", r2.outputArtifacts());
        System.out.println("   --> Received: " + r3.outputSummary());

        System.out.println("\n[✓] Squad Pipeline Concluded! Total tasks executed: " + supervisor.getAuditLog().size());
    }

    private static void demoCollaborativeDebate() {
        System.out.println("LAB 2: Collaborative Peer-Review Debate (Author vs Critic)");

        CollaborativeDebateSquad squad = new CollaborativeDebateSquad(
                (topic, previousCritique) -> {
                    if (previousCritique == null) {
                        return "Proposal v1: Use a static HashMap<String, Object> for the in-memory cache.";
                    } else {
                        return "Proposal v2: Use ConcurrentHashMap with Caffeine-style eviction and soft-references.";
                    }
                },
                proposal -> {
                    if (proposal.contains("static HashMap")) {
                        return CritiqueResult.reject(4, List.of("Not thread-safe; throws ConcurrentModificationException"),
                                List.of("Switch to ConcurrentHashMap with explicit concurrency levels"));
                    } else {
                        return CritiqueResult.approve(9, List.of("Production ready, thread-safe and memory bounded."));
                    }
                },
                3,
                8
        );

        var outcome = squad.executeDebate("Design a High-Throughput In-Memory Java Cache");
        System.out.println("\n[Debate Result]:");
        System.out.println("  Consensus Reached: " + outcome.consensusReached());
        System.out.println("  Total Rounds: " + outcome.totalRounds());
        System.out.println("  Final Score: " + outcome.finalCritique().scoreOutOfTen() + "/10");
        System.out.println("  Final Artifact: " + outcome.finalArtifact());
    }

    private static void demoMcpProtocol() {
        System.out.println("LAB 3: Model Context Protocol (MCP) JSON-RPC 2.0 Client");

        // Simulated Remote MCP Server
        McpClient client = new McpClient(request -> {
            if ("tools/list".equals(request.method())) {
                return new McpMessage.Response("2.0", request.id(), Map.of(
                        "tools", List.of(
                                Map.of(
                                        "name", "queryPostgresDb",
                                        "description", "Executes read-only SQL queries against company database",
                                        "inputSchema", Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string")))
                                )
                        )
                ), null);
            } else if ("tools/call".equals(request.method())) {
                String tool = (String) request.params().get("name");
                return new McpMessage.Response("2.0", request.id(), Map.of(
                        "content", List.of(Map.of("type", "text", "text", "Rows returned: 42 users found."))
                ), null);
            }
            return new McpMessage.Response("2.0", request.id(), null, new McpMessage.Error(-32601, "Method not found", null));
        });

        // 1. Discover tools over MCP
        List<McpToolDefinition> tools = client.listTools();
        for (McpToolDefinition t : tools) {
            System.out.printf("  [Discovered MCP Tool] %s: %s%n", t.name(), t.description());
        }

        // 2. Execute tool over MCP
        Object result = client.callTool("queryPostgresDb", Map.of("query", "SELECT * FROM users"));
        System.out.println("  [Executed MCP Tool Result] " + result);
    }
}
