package com.agentic.ai.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Summary Buffer Memory.
 * 
 * Instead of discarding older turns, compresses them into a continuous rolling summary
 * while keeping the most recent N turns intact in raw form.
 * 
 * Context Layout:
 * [Pinned System Message]
 * [System: "Summary of earlier conversation: ..."]
 * [Recent Raw Messages...]
 */
public class SummaryBufferMemory implements ChatMemory {

    @FunctionalInterface
    public interface Summarizer extends BiFunction<String, List<ChatMessage>, String> {
        /**
         * @param existingSummary The current rolling summary (or empty string)
         * @param newMessages The batch of messages being pruned from the raw buffer
         * @return The updated compressed summary
         */
        String summarize(String existingSummary, List<ChatMessage> newMessages);
    }

    private final int maxRecentMessages;
    private final Summarizer summarizer;
    private final List<ChatMessage> recentMessages = new ArrayList<>();
    private ChatMessage pinnedSystemMessage = null;
    private String currentSummary = "";

    public SummaryBufferMemory(int maxRecentMessages, Summarizer summarizer) {
        this.maxRecentMessages = maxRecentMessages;
        this.summarizer = (summarizer != null) ? summarizer : defaultDeterministicSummarizer();
    }

    public static Summarizer defaultDeterministicSummarizer() {
        return (existingSummary, newMessages) -> {
            StringBuilder sb = new StringBuilder(existingSummary);
            for (ChatMessage msg : newMessages) {
                if (!sb.isEmpty()) sb.append(" | ");
                sb.append(msg.role()).append(": ").append(msg.content());
            }
            return sb.toString();
        };
    }

    @Override
    public synchronized void add(ChatMessage message) {
        if (message == null) return;

        if (message.role() == MessageRole.SYSTEM && pinnedSystemMessage == null && recentMessages.isEmpty()) {
            pinnedSystemMessage = message;
            return;
        }

        recentMessages.add(message);

        // When recent buffer exceeds threshold, compress the oldest half of recent messages
        if (recentMessages.size() > maxRecentMessages) {
            int pruneCount = recentMessages.size() - maxRecentMessages;
            List<ChatMessage> toSummarize = new ArrayList<>(recentMessages.subList(0, pruneCount));
            recentMessages.subList(0, pruneCount).clear();

            this.currentSummary = summarizer.summarize(this.currentSummary, toSummarize);
        }
    }

    @Override
    public synchronized List<ChatMessage> messages() {
        List<ChatMessage> combined = new ArrayList<>();

        if (pinnedSystemMessage != null) {
            combined.add(pinnedSystemMessage);
        }

        if (!currentSummary.isBlank()) {
            combined.add(ChatMessage.system("Context Summary of earlier turns: " + currentSummary));
        }

        combined.addAll(recentMessages);
        return Collections.unmodifiableList(combined);
    }

    @Override
    public synchronized void clear() {
        recentMessages.clear();
        currentSummary = "";
    }

    @Override
    public synchronized int totalEstimatedTokens() {
        return messages().stream()
                .mapToInt(ChatMessage::estimatedTokens)
                .sum();
    }

    public synchronized String getCurrentSummary() {
        return currentSummary;
    }
}
