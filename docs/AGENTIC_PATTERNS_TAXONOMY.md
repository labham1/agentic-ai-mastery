# Agentic Patterns Taxonomy

A reference guide to the core architectural patterns in Agentic AI, comparing their mechanics, strengths, and failure modes.

---

## Pattern Comparison Matrix

| Pattern | Flow Control | Autonomy | Latency | Best For | Failure Mode |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Simple Prompt/RAG** | Linear ($1$ step) | None | Lowest | Fact retrieval, Q&A | Out-of-date data, hallucination |
| **ReAct (Reason + Act)** | Dynamic Loop ($1..N$) | Moderate | Medium | Interactive tasks, search, calc | Infinite loops, wandering |
| **Plan-and-Solve** | Plan first, then batch act | High | Medium | Multi-step deterministic tasks | Fragile if step 1 fails |
| **Reflection / Self-Correction** | Cyclic feedback loop | High | High | Code generation, writing | Loops on stubborn errors |
| **Supervisor Multi-Agent** | Centralized delegation | Very High | High | Complex domain separation | Supervisor bottleneck |
| **Collaborative Debate** | Peer-to-peer critique | Very High | Highest | Research, high-stakes decisions | Deadlock / circular debate |

---

## 1. The ReAct Pattern (Yao et al., 2022)
**Concept:** Interleaving **Reasoning** (thoughts) and **Acting** (tool execution).

```
User Query
    │
┌───▼────────────────────────────────────────┐
│ Loop:                                      │
│  1. Thought: "I need to check stock price" │
│  2. Action: call getStockPrice("GOOG")     │
│  3. Observation: "$182.50"                 │
│  4. Thought: "Now I have the answer"       │
│  5. Final Answer: "$182.50"                │
└────────────────────────────────────────────┘
```

* **Why it works:** Decomposes complex problems into bite-sized actions while observing real-time feedback.
* **Failure Modes:** Agent can get stuck calling the same failing tool repeatedly if error messages are non-descriptive.

---

## 2. Plan-and-Solve Pattern
**Concept:** Separate planning from execution.
1. **Planner Agent:** Creates a structured list of tasks: `[Task 1, Task 2, Task 3]`.
2. **Executor Agent:** Iterates through each task sequentially, passing intermediate outputs forward.
3. **Replanner (Optional):** Revises the remaining tasks if any task fails.

* **Best For:** Complex research reports, multi-stage data pipelines.
* **Advantage:** Greatly reduces token usage compared to continuous ReAct loops.

---

## 3. Self-Correction & Reflection Pattern
**Concept:** Incorporates an automated critique step before presenting the output to the user.

```
Generator Agent ──► Drafts Code / Artifact
                           │
                           ▼
Evaluator / Linter ─► Runs Unit Tests / Compiler
                           │
                 Failed? ──┴──► Passes Error to Generator (Reflection)
                           │
                 Passed? ──► Return to User
```

* **Best For:** Code generation, SQL query generation, formal schema compliance.

---

## 4. Multi-Agent Supervisor Pattern
**Concept:** A lead agent acts as an orchestrator / router, delegating tasks to worker agents with domain-specific system prompts and dedicated toolsets.

```
                       ┌──────────────────────┐
                       │   Supervisor Agent   │
                       └──────────┬───────────┘
               ┌──────────────────┼──────────────────┐
               ▼                  ▼                  ▼
      ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
      │ Research Agent │ │  Coding Agent  │ │    QA Agent    │
      │  (Web Tools)   │ │  (IDE Tools)   │ │ (Compiler/Test)│
      └────────────────┘ └────────────────┘ └────────────────┘
```

* **Advantage:** Prevents "context pollution". The coder agent doesn't need to know the raw HTML scraped by the research agent; it only receives the summarized spec.
