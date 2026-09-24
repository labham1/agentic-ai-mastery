package com.agentic.ai.tools;

import com.agentic.ai.core.ToolDefinition;
import com.agentic.ai.vector.Embedding;
import com.agentic.ai.vector.InMemoryVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Dynamic Tool Selector (Semantic Tool Retrieval).
 * 
 * Solves Context Bloat and Attention Hijacking when an enterprise agent has dozens
 * or hundreds of available tools.
 * 
 * Indexes tool semantic descriptions in an In-Memory Vector Store.
 * For each user query, retrieves ONLY the Top-K relevant tools to bind for that turn.
 */
public class DynamicToolSelector {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolSelector.class);
    private final InMemoryVectorStore<ToolDefinition> vectorStore = new InMemoryVectorStore<>();

    /**
     * Indexes a collection of tools into the semantic vector store.
     */
    public void indexTools(Collection<ToolDefinition> tools) {
        for (ToolDefinition tool : tools) {
            String semanticText = tool.name() + ": " + tool.description();
            Embedding embedding = InMemoryVectorStore.createPseudoEmbedding(semanticText);
            vectorStore.add(tool.name(), tool, embedding);
            log.info("Indexed semantic tool vector for [{}]", tool.name());
        }
    }

    /**
     * Retrieves the top-K most semantically relevant tools for a given user query.
     */
    public List<ToolDefinition> selectRelevantTools(String userQuery, int topK) {
        if (userQuery == null || userQuery.isBlank()) {
            return List.of();
        }

        Embedding queryEmbedding = InMemoryVectorStore.createPseudoEmbedding(userQuery);
        List<InMemoryVectorStore.SearchResult<ToolDefinition>> results =
                vectorStore.search(queryEmbedding, topK, 0.0);

        return results.stream()
                .map(InMemoryVectorStore.SearchResult::item)
                .toList();
    }

    public int totalIndexedTools() {
        return vectorStore.size();
    }
}
