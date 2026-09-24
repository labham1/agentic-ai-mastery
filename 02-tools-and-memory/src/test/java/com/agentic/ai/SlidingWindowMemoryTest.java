package com.agentic.ai;

import com.agentic.ai.memory.ChatMessage;
import com.agentic.ai.memory.MessageRole;
import com.agentic.ai.memory.SlidingWindowMemory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SlidingWindowMemoryTest {

    @Test
    @DisplayName("Should pin initial SYSTEM message and evict oldest USER/AI messages")
    void shouldPinSystemMessageDuringPruning() {
        SlidingWindowMemory memory = new SlidingWindowMemory(3, 10000);

        memory.add(ChatMessage.system("SYSTEM_RULE"));
        memory.add(ChatMessage.user("User 1"));
        memory.add(ChatMessage.ai("AI 1"));

        assertThat(memory.size()).isEqualTo(3);

        // Add 4th message - should evict "User 1", keeping "SYSTEM_RULE" at index 0
        memory.add(ChatMessage.user("User 2"));

        List<ChatMessage> messages = memory.messages();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).role()).isEqualTo(MessageRole.SYSTEM);
        assertThat(messages.get(0).content()).isEqualTo("SYSTEM_RULE");
        assertThat(messages.get(1).content()).isEqualTo("AI 1");
        assertThat(messages.get(2).content()).isEqualTo("User 2");
    }

    @Test
    @DisplayName("Should prune messages when estimated token budget is exceeded")
    void shouldPruneOnTokenBudgetExceeded() {
        // Budget of 20 tokens (~80 characters total)
        SlidingWindowMemory memory = new SlidingWindowMemory(10, 20);

        memory.add(ChatMessage.system("SYS")); // ~1 token
        memory.add(ChatMessage.user("A short message")); // ~4 tokens
        memory.add(ChatMessage.ai("A very very very very long message that takes many tokens")); // ~15 tokens

        // Now total tokens will exceed 20
        memory.add(ChatMessage.user("Another message"));

        assertThat(memory.totalEstimatedTokens()).isLessThanOrEqualTo(20);
        // Ensure SYSTEM is still preserved
        assertThat(memory.messages().get(0).content()).isEqualTo("SYS");
    }

    @Test
    @DisplayName("Should clear non-system conversation history on clear()")
    void shouldPreserveSystemMessageOnClear() {
        SlidingWindowMemory memory = new SlidingWindowMemory(5, 1000);
        memory.add(ChatMessage.system("PINNED_SYSTEM"));
        memory.add(ChatMessage.user("Hello"));
        memory.add(ChatMessage.ai("Hi there"));

        memory.clear();

        assertThat(memory.size()).isEqualTo(1);
        assertThat(memory.messages().get(0).content()).isEqualTo("PINNED_SYSTEM");
    }
}
