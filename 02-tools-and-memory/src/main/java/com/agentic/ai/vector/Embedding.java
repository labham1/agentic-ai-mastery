package com.agentic.ai.vector;

import java.util.Arrays;

/**
 * High-performance vector representation with mathematical operations for semantic search.
 */
public record Embedding(float[] vector) {

    public Embedding {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Vector cannot be null or empty");
        }
    }

    public int dimension() {
        return vector.length;
    }

    /**
     * Computes the Dot Product between this vector and another.
     */
    public double dotProduct(Embedding other) {
        checkDimensions(other);
        double sum = 0.0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * other.vector[i];
        }
        return sum;
    }

    /**
     * Computes Euclidean L2 Norm: sqrt(sum(x_i^2))
     */
    public double l2Norm() {
        double sum = 0.0;
        for (float val : vector) {
            sum += val * val;
        }
        return Math.sqrt(sum);
    }

    /**
     * Computes Cosine Similarity in the range [-1.0, 1.0]:
     * cos(theta) = (u . v) / (||u|| * ||v||)
     */
    public double cosineSimilarity(Embedding other) {
        double dot = dotProduct(other);
        double normA = l2Norm();
        double normB = other.l2Norm();

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (normA * normB);
    }

    private void checkDimensions(Embedding other) {
        if (this.vector.length != other.vector.length) {
            throw new IllegalArgumentException(
                    "Dimension mismatch: expected " + vector.length + " but got " + other.vector.length);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Embedding that)) return false;
        return Arrays.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vector);
    }

    @Override
    public String toString() {
        return "Embedding[dim=" + vector.length + "]";
    }
}
