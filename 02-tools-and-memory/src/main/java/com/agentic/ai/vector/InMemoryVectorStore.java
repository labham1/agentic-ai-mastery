package com.agentic.ai.vector;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise In-Memory Vector Database with K-Nearest Neighbors (KNN) search.
 * 
 * Supports both high-dimensional model embeddings (e.g. 768 or 1536 dim)
 * and includes a deterministic offline hash-embedder for standalone testing.
 */
public class InMemoryVectorStore<T> {

    public record VectorEntry<T>(String id, T item, Embedding embedding) {}

    public record SearchResult<T>(T item, double score, String id) implements Comparable<SearchResult<T>> {
        @Override
        public int compareTo(SearchResult<T> o) {
            // Descending order by similarity score
            return Double.compare(o.score, this.score);
        }
    }

    private final Map<String, VectorEntry<T>> storage = new ConcurrentHashMap<>();

    public void add(String id, T item, Embedding embedding) {
        if (id == null || item == null || embedding == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        storage.put(id, new VectorEntry<>(id, item, embedding));
    }

    public void remove(String id) {
        storage.remove(id);
    }

    public void clear() {
        storage.clear();
    }

    public int size() {
        return storage.size();
    }

    /**
     * Executes K-Nearest Neighbors (KNN) similarity search using Cosine Similarity.
     */
    public List<SearchResult<T>> search(Embedding queryEmbedding, int topK, double minScore) {
        if (queryEmbedding == null || topK <= 0) {
            return Collections.emptyList();
        }

        List<SearchResult<T>> results = new ArrayList<>();
        for (VectorEntry<T> entry : storage.values()) {
            double score = queryEmbedding.cosineSimilarity(entry.embedding());
            if (score >= minScore) {
                results.add(new SearchResult<>(entry.item(), score, entry.id()));
            }
        }

        Collections.sort(results);
        if (results.size() > topK) {
            return results.subList(0, topK);
        }
        return results;
    }

    public List<SearchResult<T>> search(Embedding queryEmbedding, int topK) {
        return search(queryEmbedding, topK, -1.0);
    }

    /**
     * Deterministic Lexical / Hash-based Feature Embedder.
     * Generates a normalized 64-dimensional float vector based on n-grams and token hashes.
     * Perfect for offline unit tests and demonstration without requiring a cloud embedding model.
     */
    public static Embedding createPseudoEmbedding(String text) {
        if (text == null || text.isBlank()) {
            float[] zero = new float[64];
            return new Embedding(zero);
        }

        float[] vector = new float[64];
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = normalized.split("\\s+");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String token : tokens) {
                if (token.isBlank()) continue;
                byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < 32; i++) {
                    int bucket = Math.abs(hash[i]) % 64;
                    vector[bucket] += 1.0f;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // L2 Normalize vector
        double sum = 0.0;
        for (float v : vector) sum += v * v;
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= (float) norm;
            }
        }

        return new Embedding(vector);
    }
}
