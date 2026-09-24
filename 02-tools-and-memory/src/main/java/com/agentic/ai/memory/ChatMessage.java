package com.agentic.ai.memory;

import java.time.Instant;

/**
 * Immutable chat message representation with token estimation.
 */
public record ChatMessage(
        MessageRole role,
        String content,
        Instant timestamp,
        int estimatedTokens
) {
    public ChatMessage(MessageRole role, String content) {
        this(role, content, Instant.now(), estimateTokenCount(content));
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(MessageRole.SYSTEM, content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(MessageRole.USER, content);
    }

    public static ChatMessage ai(String content) {
        return new ChatMessage(MessageRole.AI, content);
    }

    public static ChatMessage tool(String content) {
        return new ChatMessage(MessageRole.TOOL, content);
    }

    /**
     * Approximate token estimation rule of thumb: ~4 characters per token in English.
     * Minimum 1 token for non-empty text.
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
