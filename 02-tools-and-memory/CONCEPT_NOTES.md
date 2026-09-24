# Concept Notes: Module 02 — Tools, Context Windows & Memory Architectures

## 1. The Memory Spectrum in Agentic AI

An LLM is inherently stateless. Every HTTP call to Gemini, OpenAI, or Claude has zero recollection of previous calls unless the host application feeds that context back in.

In production agentic architectures, memory is stratified into **three distinct tiers**:

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. SHORT-TERM WORKING MEMORY (Context Window)                          │
│    - Fast, ephemeral, limited by token capacity & budget               │
│    - Holds immediate conversation turns, scratchpad, active tool logs  │
├────────────────────────────────────────────────────────────────────────┤
│ 2. WORKING / ENTITY MEMORY (Structured State)                          │
│    - Key-Value / Relational store (User profile, session variables)    │
│    - Holds deterministic facts: "user_name = Alice", "tier = Enterprise"│
├────────────────────────────────────────────────────────────────────────┤
│ 3. LONG-TERM SEMANTIC MEMORY (Vector Store / RAG)                      │
│    - Unbounded storage, indexed by semantic embeddings                 │
│    - Retrieved via Cosine Similarity on demand                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Short-Term Memory Strategies

When conversations exceed token limits or budget thresholds, how do you prevent context overflow?

### Strategy A: Sliding Message Window
* **Mechanism:** Retain only the last $N$ messages (e.g. last 10 messages).
* **Pros:** $O(1)$ complexity, zero extra LLM calls.
* **Cons:** Hard amnesia. If the user stated their goal in turn 1, the agent forgets it by turn 11.

### Strategy B: Token-Aware Pruning
* **Mechanism:** Calculate approximate token counts. Keep messages until the total fits within a designated budget (e.g. 4,000 tokens), dropping the oldest non-system messages first.
* **Pros:** Respects precise model token limits.
* **Cons:** Still suffers from eventual amnesia for older facts.

### Strategy C: Summary Buffer Memory (Enterprise Standard)
* **Mechanism:** As conversation history grows beyond a threshold, an LLM call condenses the oldest turns into a concise running summary:
  $$\text{New Summary} = \text{LLM}(\text{Old Summary} + \text{Expiring Turns})$$
* **Context Layout sent to LLM:**
  ```
  [System Message]
  [Current Running Summary of Past Events]
  [Recent Raw Messages (last 4 turns)]
  [Latest User Input]
  ```
* **Pros:** Preserves crucial long-term context indefinitely with minimal token overhead.
* **Cons:** Requires periodic background LLM summarization calls (latency & cost trade-off).

---

## 3. The "Lost in the Middle" Effect & Attention Hijacking

Even though models like Gemini 2.0 Flash support massive 1M+ token context windows, **dumping raw logs into the prompt causes severe issues**:
1. **Lost in the Middle (Liu et al., Stanford/UC Berkeley):** LLM attention retrieval accuracy is high at the very start ($< 10\%$) and very end ($> 90\%$) of the context, but drops sharply in the middle.
2. **Latency Inflation:** Time-to-First-Token (TTFT) scales linearly with prompt size. A 100K token prompt takes seconds to process compared to milliseconds for a 2K prompt.
3. **Cost Escalation:** Prompt caching helps, but repeatedly sending large histories burns tokens on every turn.

---

## 4. Dynamic Tool Loading (Semantic Tool Retrieval)

### The Problem: Tool Selection Degeneracy
When an enterprise agent has 50+ tools (e.g., Jira, Slack, GitHub, Database, AWS, CRM), registering all 50 tools in every prompt creates two critical failures:
1. **Context Bloat:** 50 JSON schemas consume 10,000+ tokens before the user even speaks.
2. **Attention Confusion:** Tool descriptions overlap. The LLM hallucinates arguments or calls `findUserInSlack` instead of `findUserInDb`.

### The Solution: Two-Stage Dynamic Tool Retrieval
```
User Query: "What is the status of issue PROJ-123?"
     │
     ▼
┌──────────────────────────────────────────────┐
│ 1. Vector Search against Tool Registry Index │
│    - Embed query & compare to tool embeddings│
│    - Returns Top-3 relevant tools            │
└──────────────────────┬───────────────────────┘
                       │ Top tools: [getJiraIssue, searchGithub, queryDb]
                       ▼
┌──────────────────────────────────────────────┐
│ 2. Bind ONLY those 3 tools to Agent Context  │
│    - Minimal token consumption               │
│    - 99%+ tool selection accuracy            │
└──────────────────────────────────────────────┘
```

---

## 5. In-Memory Vector Search Mathematics

To retrieve relevant tools or memories, text is converted into high-dimensional vectors ($\mathbb{R}^d$) via an embedding model.

### Cosine Similarity:
$$\text{sim}(\mathbf{u}, \mathbf{v}) = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{u}\|_2 \|\mathbf{v}\|_2}$$

* For normalized vectors where $\|\mathbf{u}\| = 1$ and $\|\mathbf{v}\| = 1$:
  $$\text{sim}(\mathbf{u}, \mathbf{v}) = \sum_{i=1}^d u_i v_i \quad \text{(Dot Product)}$$
* In this module, we build an **`InMemoryVectorStore` in pure Java** that implements dot product ranking for instant, offline similarity search.
