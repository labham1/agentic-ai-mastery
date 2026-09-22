package com.agentic.ai;

import com.agentic.ai.agent.AutonomousAgent;
import com.agentic.ai.core.ToolExecutionResult;
import com.agentic.ai.core.ToolRegistry;
import com.agentic.ai.tools.SystemTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Scanner;

/**
 * Main Interactive Application.
 * 
 * To run with Gemini:
 * Set environment variable: GEMINI_API_KEY=your_key_here
 * (Get free key from https://aistudio.google.com/)
 */
public class AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

    public static void main(String[] args) {
        System.out.println("""
            ================================================================
               AGENTIC AI MASTERY - MODULE 01: REACT FOUNDATIONS
            ================================================================
            """);

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("gemini.api.key");
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("""
                [!] GEMINI_API_KEY is not detected in your environment.
                
                You can get a FREE key in 30 seconds at:
                --> https://aistudio.google.com/
                
                Then set it in your editor's Run Configuration:
                Environment Variable: GEMINI_API_KEY=your_key
                
                Running OFFLINE ENGINE SIMULATION now to demonstrate
                Dynamic Tool Reflection & JSON Schema Generation...
                ----------------------------------------------------------------
                """);
            runOfflineSimulation();
            return;
        }

        System.out.println("[+] GEMINI_API_KEY detected. Initializing Google Gemini 2.0 Flash model...");

        try {
            // 1. Initialize the LLM Client
            ChatLanguageModel chatModel = GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName("gemini-2.0-flash")
                    .temperature(0.1) // Low temperature for deterministic tool calling
                    .logRequestsAndResponses(true) // Traces raw LLM calls and tool executions in console
                    .build();

            // 2. Instantiate Tools
            SystemTools systemTools = new SystemTools();

            // 3. Assemble the Autonomous Agent using LangChain4j AiServices
            AutonomousAgent agent = AiServices.builder(AutonomousAgent.class)
                    .chatLanguageModel(chatModel)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(15))
                    .tools(systemTools)
                    .build();

            System.out.println("""
                [✓] Agent Initialized Successfully!
                Available Tools:
                  - getCurrentDateTime()
                  - calculate(a, b, operation)
                  - calculateSquareRoot(number)
                  - listDirectory(path)
                  - getWeather(city)
                
                Try asking:
                  - 'What is the weather in London right now?'
                  - 'Calculate the square root of 256 and add 44 to it'
                  - 'What time is it, and what files are in C:\\Users?'
                
                Type 'exit' to quit.
                ================================================================
                """);

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nYou > ");
                if (!scanner.hasNextLine()) break;
                String input = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                    System.out.println("Goodbye!");
                    break;
                }
                if (input.isEmpty()) continue;

                System.out.println("\n[Agent Thinking & Executing Tools...]");
                try {
                    String response = agent.execute(input);
                    System.out.println("\nAgent > " + response);
                } catch (Exception e) {
                    System.err.println("\n[Error executing agent]: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to start agent application", e);
        }
    }

    private static void runOfflineSimulation() {
        try {
            ToolRegistry registry = new ToolRegistry();
            registry.registerTools(new SystemTools());

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

            System.out.println("1. Generated LLM JSON Schemas for Registered Tools:");
            System.out.println(mapper.writeValueAsString(registry.getAllJsonSchemas()));

            System.out.println("\n2. Simulating Tool Invocations via Registry:");
            
            // Invoke getWeather
            ToolExecutionResult weatherResult = registry.execute("getWeather", Map.of("city", "Tokyo"));
            System.out.printf("   [Tool Call: getWeather('Tokyo')] -> Result: %s (took %d ms)%n",
                    weatherResult.toObservationString(), weatherResult.executionDurationMs());

            // Invoke calculate
            ToolExecutionResult calcResult = registry.execute("calculate", Map.of("a", 100, "b", 25, "operation", "divide"));
            System.out.printf("   [Tool Call: calculate(100, 25, 'divide')] -> Result: %s (took %d ms)%n",
                    calcResult.toObservationString(), calcResult.executionDurationMs());

            // Invoke with bad arguments (Error boundary demonstration)
            ToolExecutionResult errorResult = registry.execute("calculate", Map.of("a", 100, "b", 0, "operation", "divide"));
            System.out.printf("   [Tool Call: calculate(100, 0, 'divide')] -> Handled Observation: %s (Success: %s)%n",
                    errorResult.toObservationString(), errorResult.success());

            System.out.println("\n[✓] Simulation Complete. Set your GEMINI_API_KEY to start interactive chatting!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
