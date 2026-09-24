package com.agentic.ai;

import com.agentic.ai.core.ToolRegistry;
import com.agentic.ai.memory.ChatMessage;
import com.agentic.ai.memory.SlidingWindowMemory;
import com.agentic.ai.memory.SummaryBufferMemory;
import com.agentic.ai.tools.DynamicToolSelector;
import com.agentic.ai.tools.SystemTools;
import com.agentic.ai.vector.Embedding;
import com.agentic.ai.vector.InMemoryVectorStore;

import java.util.List;

/**
 * Interactive Lab Runner for Module 02: Memory & Context Windows.
 * 
 * Runs full offline simulations of:
 * 1. Pinned System Message & Sliding Window Token Pruning
 * 2. Rolling Summary Buffer Memory
 * 3. In-Memory Vector Cosine Similarity Search
 * 4. Dynamic Two-Stage Tool Retrieval
 */
public class MemoryLabRunner {

    public static void main(String[] args) {
        System.out.println("""
            ================================================================
               AGENTIC AI MASTERY - MODULE 02: TOOLS & MEMORY LAB
            ================================================================
            """);

        demoSlidingWindow();
        System.out.println("\n----------------------------------------------------------------\n");

        demoSummaryBuffer();
        System.out.println("\n----------------------------------------------------------------\n");

        demoVectorSearch();
        System.out.println("\n----------------------------------------------------------------\n");

        demoDynamicToolRetrieval();
        System.out.println("\n================================================================");
    }

    private static void demoSlidingWindow() {
        System.out.println("LAB 1: Sliding Window Memory with Pinned System Message");
        System.out.println("Constraint: maxMessages = 4 (including pinned system message)");

        SlidingWindowMemory memory = new SlidingWindowMemory(4, 1000);
        memory.add(ChatMessage.system("SYSTEM: You are a financial analyst agent."));
        memory.add(ChatMessage.user("User Turn 1: Check Apple revenue."));
        memory.add(ChatMessage.ai("AI Turn 1: Apple revenue is $90B."));
        memory.add(ChatMessage.user("User Turn 2: What about Microsoft?"));

        System.out.println("\n[State at Turn 2 - Total Messages: " + memory.size() + "]");
        printMessages(memory.messages());

        System.out.println("\n--> Adding Turn 3 (AI response + new user turn) - Watch oldest user turn get evicted while SYSTEM stays pinned:");
        memory.add(ChatMessage.ai("AI Turn 2: Microsoft revenue is $60B."));
        memory.add(ChatMessage.user("User Turn 3: Compare their profit margins."));

        System.out.println("\n[State at Turn 3 - Total Messages: " + memory.size() + "]");
        printMessages(memory.messages());
    }

    private static void demoSummaryBuffer() {
        System.out.println("LAB 2: Rolling Summary Buffer Memory");
        System.out.println("Constraint: maxRecentMessages = 2 (older turns roll into running summary)");

        SummaryBufferMemory memory = new SummaryBufferMemory(2, null);
        memory.add(ChatMessage.system("SYSTEM: Code Review Assistant"));
        memory.add(ChatMessage.user("Found a NullPointerException in UserService.java"));
        memory.add(ChatMessage.ai("Checked line 42, user.getAddress() returned null."));
        memory.add(ChatMessage.user("Added Optional.ofNullable() check."));
        memory.add(ChatMessage.ai("Compiled successfully and tests passed."));

        System.out.println("\n[Summary Buffer Output to LLM]:");
        printMessages(memory.messages());
    }

    private static void demoVectorSearch() {
        System.out.println("LAB 3: In-Memory Vector Store with Cosine Similarity");
        InMemoryVectorStore<String> store = new InMemoryVectorStore<>();

        store.add("doc1", "Spring Boot microservice deployment on Kubernetes with Docker",
                InMemoryVectorStore.createPseudoEmbedding("Spring Boot microservice deployment on Kubernetes with Docker"));
        store.add("doc2", "Deep learning transformer architecture self-attention mechanism",
                InMemoryVectorStore.createPseudoEmbedding("Deep learning transformer architecture self-attention mechanism"));
        store.add("doc3", "PostgreSQL database indexing B-tree performance tuning",
                InMemoryVectorStore.createPseudoEmbedding("PostgreSQL database indexing B-tree performance tuning"));

        String query = "How to run Java containers on Kubernetes clusters?";
        System.out.println("Query: \"" + query + "\"");
        Embedding queryEmb = InMemoryVectorStore.createPseudoEmbedding(query);

        List<InMemoryVectorStore.SearchResult<String>> results = store.search(queryEmb, 3);
        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            System.out.printf("  Rank %d: [Score: %.4f] %s%n", i + 1, r.score(), r.item());
        }
    }

    private static void demoDynamicToolRetrieval() {
        System.out.println("LAB 4: Dynamic Tool Retrieval (Solving Context Drift / Bloat)");
        ToolRegistry registry = new ToolRegistry();
        registry.registerTools(new SystemTools());

        DynamicToolSelector selector = new DynamicToolSelector();
        selector.indexTools(registry.getTools());

        String[] testQueries = {
                "What is the weather outside in London?",
                "Can you calculate 144 divided by 12?",
                "Show me what files are stored in this directory"
        };

        for (String q : testQueries) {
            System.out.println("\nUser Query: \"" + q + "\"");
            var selected = selector.selectRelevantTools(q, 1);
            if (!selected.isEmpty()) {
                var tool = selected.get(0);
                System.out.printf("  ==> Dynamically Selected Tool: [%s] (Description: %s)%n",
                        tool.name(), tool.description());
            }
        }
    }

    private static void printMessages(List<ChatMessage> list) {
        for (ChatMessage m : list) {
            System.out.printf("   [%s] %s (~%d tokens)%n", m.role(), m.content(), m.estimatedTokens());
        }
    }
}
