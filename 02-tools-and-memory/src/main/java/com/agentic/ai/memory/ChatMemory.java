package com.agentic.ai.memory;

import java.util.List;

/**
 * Common contract for managing conversation context in Agentic AI.
 */
public interface ChatMemory {

    /**
     * Adds a message to the memory, triggering pruning or compression if limits are reached.
     */
    void add(ChatMessage message);

    /**
     * Returns an unmodifiable list of currently retained messages in chronological order.
     */
    List<ChatMessage> messages();

    /**
     * Clears all non-system conversation history.
     */
    void clear();

    /**
     * Current total estimated token count across all messages in memory.
     */
    int totalEstimatedTokens();

    /**
     * Number of retained messages.
     */
    default int size() {
        return messages().size();
    }
}
