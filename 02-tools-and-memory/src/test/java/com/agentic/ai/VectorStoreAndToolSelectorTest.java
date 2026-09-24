package com.agentic.ai;

import com.agentic.ai.core.ToolRegistry;
import com.agentic.ai.tools.DynamicToolSelector;
import com.agentic.ai.tools.SystemTools;
import com.agentic.ai.vector.Embedding;
import com.agentic.ai.vector.InMemoryVectorStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class VectorStoreAndToolSelectorTest {

    @Test
    @DisplayName("Should correctly calculate Cosine Similarity and Dot Product")
    void shouldCalculateVectorMathematics() {
        // Orthogonal vectors
        Embedding v1 = new Embedding(new float[]{1.0f, 0.0f});
        Embedding v2 = new Embedding(new float[]{0.0f, 1.0f});
        assertThat(v1.cosineSimilarity(v2)).isEqualTo(0.0, offset(0.0001));

        // Identical vectors
        Embedding v3 = new Embedding(new float[]{3.0f, 4.0f});
        assertThat(v3.cosineSimilarity(v3)).isEqualTo(1.0, offset(0.0001));

        // Parallel opposite vectors
        Embedding v4 = new Embedding(new float[]{-3.0f, -4.0f});
        assertThat(v3.cosineSimilarity(v4)).isEqualTo(-1.0, offset(0.0001));
    }

    @Test
    @DisplayName("Should store and rank items by cosine similarity in InMemoryVectorStore")
    void shouldStoreAndRankItems() {
        InMemoryVectorStore<String> store = new InMemoryVectorStore<>();

        store.add("A", "Apple fruit", new Embedding(new float[]{1.0f, 0.1f}));
        store.add("B", "Banana fruit", new Embedding(new float[]{0.9f, 0.2f}));
        store.add("C", "Car automobile", new Embedding(new float[]{0.0f, 1.0f}));

        Embedding query = new Embedding(new float[]{1.0f, 0.0f});
        var results = store.search(query, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).item()).isEqualTo("Apple fruit");
        assertThat(results.get(1).item()).isEqualTo("Banana fruit");
    }

    @Test
    @DisplayName("Should dynamically retrieve semantically relevant tools for user queries")
    void shouldDynamicallySelectTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTools(new SystemTools());

        DynamicToolSelector selector = new DynamicToolSelector();
        selector.indexTools(registry.getTools());

        assertThat(selector.totalIndexedTools()).isEqualTo(5);

        // Query 1: Weather
        var weatherTools = selector.selectRelevantTools("What is the current weather forecast for Tokyo?", 1);
        assertThat(weatherTools).isNotEmpty();
        assertThat(weatherTools.get(0).name()).isEqualTo("getWeather");

        // Query 2: Directory listing
        var fileTools = selector.selectRelevantTools("Show me what files are stored in this directory", 1);
        assertThat(fileTools).isNotEmpty();
        assertThat(fileTools.get(0).name()).isEqualTo("listDirectory");
    }
}
