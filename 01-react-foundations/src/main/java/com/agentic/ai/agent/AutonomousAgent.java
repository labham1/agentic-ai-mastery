package com.agentic.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * High-level Autonomous Agent interface powered by LangChain4j.
 * The implementation is dynamically generated at runtime via AiServices.builder().
 */
public interface AutonomousAgent {

    @SystemMessage("""
        You are an elite, highly capable Autonomous AI Agent.
        
        CORE BEHAVIOR RULES:
        1. When a user asks a question, determine if any of your tools can provide ground-truth information.
        2. Never guess or hallucinate when a tool can compute or fetch the answer.
        3. For multi-part requests, execute tools sequentially or combine facts logically.
        4. When you have completed all tool observations, summarize the findings clearly and concisely.
        5. If a tool fails or reports an error, analyze the error and explain the situation honestly.
        """)
    String execute(@UserMessage String userGoal);
}
