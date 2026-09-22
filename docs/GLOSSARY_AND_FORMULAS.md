# Agentic AI Glossary & Mathematical Foundations

Key terms, formulas, and concepts essential for technical interviews and deep understanding.

---

## 1. Probabilistic Foundations

### Temperature ($T$)
Controls the randomness of the model's token sampling:
$$P(w_i) = \frac{\exp(z_i / T)}{\sum_j \exp(z_j / T)}$$
* **$T \rightarrow 0$ (Argmax / Greedy):** Highly deterministic. Ideal for tool calling, JSON schema compliance, and code generation.
* **$T \approx 0.7 - 1.0$:** Creative, diverse outputs (story writing, brainstorming).
* **For Agent Tool Calling:** Always set $T \le 0.2$ to minimize malformed JSON arguments.

### Top-p (Nucleus Sampling)
Selects tokens from the smallest set whose cumulative probability exceeds $p$:
$$\sum_{i \in V^{(p)}} P(w_i) \ge p$$
* Filters out long-tail improbable tokens, keeping outputs coherent.

---

## 2. Vector & Semantic Memory Mathematics

### Cosine Similarity
Measures the directional alignment of two embedding vectors $\mathbf{u}$ and $\mathbf{v}$:
$$\text{CosineSimilarity}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2} = \frac{\sum_{i=1}^d u_i v_i}{\sqrt{\sum_{i=1}^d u_i^2} \sqrt{\sum_{i=1}^d v_i^2}}$$
* Range: $[-1, 1]$ (normalized embeddings typically yield $[0, 1]$).
* Used in vector search to retrieve relevant memories or documents for an agent.

### Euclidean Distance ($L_2$)
$$d(\mathbf{u}, \mathbf{v}) = \sqrt{\sum_{i=1}^d (u_i - v_i)^2}$$
* When vectors are $L_2$-normalized, Euclidean distance ranking is identical to Cosine Similarity ranking.

---

## 3. Core Architectural Terminology

* **Context Window:** The maximum number of tokens (prompt + output) an LLM can attend to in a single forward pass (e.g. 1M–2M tokens for Gemini 1.5/2.0).
* **Lost in the Middle Effect:** The phenomenon where LLMs recall information placed at the beginning and end of long contexts significantly better than information buried in the middle.
* **Function / Tool Calling:** A fine-tuned capability where the LLM does not answer directly, but emits a structured JSON payload requesting execution of an external function.
* **JSON Schema:** The standard format describing function parameters, types, and required fields passed to the LLM.
* **Hallucination:** When an LLM generates factually false or unverifiable statements with high confidence.
* **Grounding:** Tying LLM outputs directly to verifiable source documents or tool observations.
* **Human-in-the-Loop (HITL):** A safety design pattern where an agent pauses its state machine and waits for human authorization before executing sensitive operations (e.g., executing writes, purchasing, modifying databases).
* **MCP (Model Context Protocol):** An open standard protocol created by Anthropic allowing AI applications to connect to external data sources and tools uniformly over JSON-RPC.
