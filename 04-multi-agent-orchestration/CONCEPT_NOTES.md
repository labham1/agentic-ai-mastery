# Concept Notes: Module 04 — Multi-Agent Orchestration, Debate & MCP

## 1. Why Multi-Agent Systems? (The Context Pollution Problem)

When building an enterprise AI system to solve complex, multi-stage problems (e.g. "Research competitors, design a microservice architecture, write the code, and generate unit tests"), forcing a single agent to handle everything produces **severe degradation**:

1. **Context Pollution:** The system prompt becomes massive with dozens of conflicting instructions (e.g. "Be creative in research", "Be strictly deterministic in coding", "Be pedantic in testing").
2. **Attention Dilution:** LLMs struggle to attend to 50 tools and 30 pages of research notes simultaneously, leading to hallucinations.
3. **Lack of Separation of Concerns:** In traditional software engineering, we don't have one giant God Class; we have specialized services. Multi-agent systems apply this principle to LLMs.

---

## 2. Multi-Agent Architectural Patterns

```
PATTERN A: SUPERVISOR (CENTRALIZED)        PATTERN B: COLLABORATIVE DEBATE (PEER-TO-PEER)
          ┌────────────┐                                  ┌──────────────┐
          │ Supervisor │                                  │  Generator   │
          └─────┬──────┘                                  │   (Author)   │
      ┌─────────┼─────────┐                               └──────┬───────┘
      ▼         ▼         ▼                                      │ Proposal
 ┌────────┐ ┌────────┐ ┌────────┐                         Critique│ ◄─────┐
 │Research│ │ Coder  │ │   QA   │                                ▼       │
 └────────┘ └────────┘ └────────┘                         ┌──────────────┴┐
                                                          │    Critic     │
                                                          │   (Reviewer)  │
                                                          └───────────────┘
```

### Pattern A: The Supervisor (Orchestrator-Worker) Pattern
* **Mechanism:**
  1. The **Supervisor Agent** receives the high-level user goal.
  2. It plans the execution DAG and assigns scoped subtasks:
     * `ResearcherWorker`: Web search & data gathering $\rightarrow$ outputs concise research brief.
     * `DeveloperWorker`: Receives only the research brief $\rightarrow$ outputs clean Java code.
     * `QAWorker`: Receives only the code $\rightarrow$ writes and runs unit tests.
  3. The Supervisor inspects worker reports, decides if additional tasks are needed, and synthesizes the final response.
* **Advantage:** Maximum context isolation. The Coder never sees raw web search HTML; the QA agent only sees the code.

### Pattern B: Collaborative Debate / Peer Review Pattern
* **Mechanism:**
  * Used for high-stakes decisions, architecture design, and critical code reviews.
  * Agent 1 (**Advocate / Author**) generates a proposal.
  * Agent 2 (**Skeptic / Critic**) reviews it against strict criteria (performance, security, race conditions).
  * The debate continues until the Critic's consensus score exceeds a threshold ($\text{Score} \ge 8/10$) or hits a max round ceiling.

---

## 3. The Model Context Protocol (MCP)

### What is MCP?
Created by Anthropic and rapidly adopted by Microsoft, Google, and the open-source community, the **Model Context Protocol (MCP)** is an open standard that provides a universal, secure protocol for LLMs and agents to interact with external data sources and tools.

### Why MCP Solves the $M \times N$ Integration Problem:
* **Before MCP ($M \times N$):** Every agent framework (LangChain, CrewAI, AutoGen, Spring AI) had to write custom tool connectors for every database (Postgres, GitHub, Slack, AWS).
* **With MCP ($M + N$):** Any tool exposes a single MCP Server. Any agent connects as an MCP Client over a standardized protocol.

```
┌─────────────────────────────────┐
│     Agent Client (Spring AI)    │
└────────────────┬────────────────┘
                 │ JSON-RPC 2.0 (stdio / SSE)
                 ▼
┌─────────────────────────────────┐
│           MCP Server            │
│  - tools/list                   │
│  - tools/call                   │
│  - resources/read               │
└────────────────┬────────────────┘
                 ▼
      [External DB / API / Git]
```

### Standard MCP JSON-RPC 2.0 Payloads:
1. **Tool Discovery (`tools/list`):**
   ```json
   {
     "jsonrpc": "2.0",
     "id": 1,
     "method": "tools/list"
   }
   ```
2. **Tool Execution (`tools/call`):**
   ```json
   {
     "jsonrpc": "2.0",
     "id": 2,
     "method": "tools/call",
     "params": {
       "name": "queryDatabase",
       "arguments": { "sql": "SELECT COUNT(*) FROM users" }
     }
   }
   ```
