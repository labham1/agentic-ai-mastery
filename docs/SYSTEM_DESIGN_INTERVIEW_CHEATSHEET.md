# AI Agent System Design: Staff-Level Interview Framework

In senior/staff level interviews, you will often be asked questions like:
> *"Design an autonomous Customer Support Agent for an airline."*  
> *"Design an AI Coding Assistant that can autonomously fix failing CI tests."*  
> *"Design a financial analysis agent that can query multiple databases and summarize risks."*

Use this **7-Step Blueprint** to systematically structure and ace your response.

---

## The 7-Step Agent System Design Framework

```
[1. Requirements & Scope] ──► [2. Core Agent Architecture] ──► [3. Tooling & APIs]
                                                                      │
[7. Cost & Latency Budgets] ◄── [6. Security & Guardrails] ◄── [4. Memory Subsystem]
                                       ▲                              │
                                       └────── [5. Evaluation & Obs] ─┘
```

---

### Step 1: Clarify Scope, Failure Modes & Autonomy Level
Before writing any architecture, clarify:
1. **Degree of Autonomy:**
   - Level 1: Human asks, LLM suggests, Human executes.
   - Level 2: Human asks, Agent executes read-only tools automatically, writes require confirmation (**Human-in-the-Loop**).
   - Level 3: Fully autonomous goal execution with automated guardrails.
2. **Deterministic vs. Probabilistic boundaries:** What MUST NEVER be left to LLM chance? (e.g., money transfers, credit card refunds).
3. **Throughput & Latency SLIs:** Is this real-time (under 2s) or async batch (background worker)?

---

### Step 2: Choose the Agent Pattern
Defend *why* you chose a specific pattern over others:
* **Single ReAct Loop:** Simple single-agent tasks with 3–5 tools.
* **Plan-and-Solve:** Tasks requiring long horizon planning where a high-level plan is generated first, then executed step-by-step.
* **StateGraph (Cyclic):** Tasks with clear state transitions, retries, and reflection (e.g. Code $\rightarrow$ Test $\rightarrow$ Fix).
* **Multi-Agent (Supervisor / Swarm):** Complex workflows where single prompt context degrades or specialized roles are needed.

---

### Step 3: Tool & Interface Design (The Agent's "Hands")
* **Strict Schema Definition:** JSON Schema / Pydantic / Java Bean validation.
* **Tool Idempotency:** How do you prevent double billing if an agent retries an API call? (Use Idempotency Keys).
* **Failure Boundaries:** If a tool returns a 500 error or times out, how does the agent recover? (Return descriptive error message in tool result so the agent can pivot, rather than throwing an unhandled exception).
* **Read-Write Separation:** Safe tools (read-only) vs. Sensitive tools (write/delete).

---

### Step 4: Memory Subsystem Design
Explain how context is managed across short, working, and long-term horizons:
* **Short-Term (Context Window):** Sliding window or token-aware buffer with summary compression.
* **Working Memory (Scratchpad):** The current plan, sub-goals completed, and observations.
* **Long-Term Memory (Persistent/Semantic):**
  * Vector Database (pgvector, Chroma, Pinecone) for unstructured semantic recall.
  * Relational Database (PostgreSQL) for user profile, transaction state, and audit trails.

---

### Step 5: Evaluation & Observability (How do you know it works?)
* **Tracing:** OpenTelemetry / LangSmith spans for every step: `[User Request] -> [LLM Call (tokens/latency)] -> [Tool Execution] -> [Next LLM Call]`.
* **Evaluation Metrics:**
  * **Tool Selection Accuracy:** Did it choose the correct tool?
  * **Parameter Precision:** Were arguments valid?
  * **Goal Completion Rate:** Did the agent terminate successfully within $N$ steps?
  * **LLM-as-a-Judge:** Using a stronger model (e.g. Gemini 1.5/2.0 Pro) to score agent outputs against a gold test set.

---

### Step 6: Security, Guardrails & Human-in-the-Loop (HITL)
* **Prompt Injection Defense:** Input classification before the agent sees it.
* **Output Guardrails:** Validating that agent output adheres to company policy and doesn't leak secrets.
* **Least Privilege:** Giving the agent API keys with minimal scopes.
* **Human-in-the-Loop Checkpoints:**
  ```
  Agent Plans Action ──► Risk Threshold Exceeded? ──► Pause Graph Execution
                                                            │
                                                     Notify Human Reviewer
                                                            │
                                                     Approved? ──► Resume
  ```

---

### Step 7: Cost & Latency Optimization
* **Model Cascading / Routing:** Use a fast, cheap model (e.g., Gemini 2.0 Flash or Groq Llama-3-8B) for routing, tool calling, and classification; use a large reasoning model (e.g. Gemini Pro / Claude 3.5 Sonnet) only for complex synthesis.
* **Semantic Caching:** Cache common tool responses or embedding queries in Redis.
* **Parallel Tool Calling:** When an agent requests multiple independent tools in one step, execute them concurrently using `CompletableFuture` in Java.
