package com.agentic.ai.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Enterprise Sliding Window Chat Memory.
 * 
 * Enforces dual constraints:
 * 1. Maximum message count
 * 2. Maximum token budget
 * 
 * CRITICAL ARCHITECTURAL RULE:
 * The initial SYSTEM message (defining persona, tools, and rules) is pinned
 * at index 0 and NEVER evicted during pruning.
 */
public class SlidingWindowMemory implements ChatMemory {

    private final int maxMessages;
    private final int maxTokens;
    private final List<ChatMessage> messageList = new ArrayList<>();
    private ChatMessage pinnedSystemMessage = null;

    public SlidingWindowMemory(int maxMessages, int maxTokens) {
        if (maxMessages < 2) {
            throw new IllegalArgumentException("maxMessages must be at least 2");
        }
        this.maxMessages = maxMessages;
        this.maxTokens = maxTokens;
    }

    public static SlidingWindowMemory withMaxMessages(int maxMessages) {
        return new SlidingWindowMemory(maxMessages, Integer.MAX_VALUE);
    }

    public static SlidingWindowMemory withMaxTokens(int maxTokens) {
        return new SlidingWindowMemory(Integer.MAX_VALUE, maxTokens);
    }

    @Override
    public synchronized void add(ChatMessage message) {
        if (message == null) return;

        // Pin the first SYSTEM message to preserve core agent rules
        if (message.role() == MessageRole.SYSTEM && pinnedSystemMessage == null && messageList.isEmpty()) {
            pinnedSystemMessage = message;
            messageList.add(message);
            return;
        }

        messageList.add(message);
        prune();
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        return Collections.unmodifiableList(new ArrayList<>(messageList));
    }

    @Override
    public synchronized void clear() {
        messageList.clear();
        if (pinnedSystemMessage != null) {
            messageList.add(pinnedSystemMessage);
        }
    }

    @Override
    public synchronized int totalEstimatedTokens() {
        return messageList.stream()
                .mapToInt(ChatMessage::estimatedTokens)
                .sum();
    }

    private void prune() {
        int evictionStartIndex = (pinnedSystemMessage != null) ? 1 : 0;

        // 1. Prune by maximum message count
        while (messageList.size() > maxMessages && messageList.size() > evictionStartIndex) {
            messageList.remove(evictionStartIndex);
        }

        // 2. Prune by maximum token budget
        while (totalEstimatedTokens() > maxTokens && messageList.size() > evictionStartIndex + 1) {
            messageList.remove(evictionStartIndex);
        }
    }
}
