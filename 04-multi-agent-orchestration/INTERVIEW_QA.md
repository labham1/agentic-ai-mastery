# Interview Q&A: Module 04 — Multi-Agent Systems, Debate & MCP

Staff & Principal-level interview questions on multi-agent choreography, debate convergence, and the Model Context Protocol.

---

### Q1: Compare Multi-Agent Orchestration (Supervisor) vs. Choreography (Swarm / Handoff). When do you use each?
**Staff Answer:**
* **Orchestration (Supervisor Pattern):**
  * A centralized coordinator agent acts as the Single Source of Truth. It decomposes goals, assigns tasks, receives worker outputs, and enforces sequential or parallel DAG execution.
  * *Pros:* High predictability, strict auditing, easier error recovery, centralized token tracking.
  * *Cons:* Supervisor can become a bottleneck; extra LLM hops add latency.
  * *Best For:* Enterprise workflows with compliance rules, code generation pipelines, legal analysis.
* **Choreography (Swarm / Agent-to-Agent Handoff):**
  * Decentralized. Each agent has the ability to transfer execution control directly to another agent (e.g., `handoffToSupportTier2()`).
  * *Pros:* Lower latency for simple routing; flexible peer interactions.
  * *Cons:* Harder to debug, prone to circular ping-pong loops, difficult to track global state.
  * *Best For:* Customer support triage, chat routing.

---

### Q2: How do you prevent endless debate or deadlocks between two collaborating agents?
**Staff Answer:**
* **Mechanisms to guarantee convergence:**
  1. **Numerical Consensus Scoring:** The Critic agent must emit a structured evaluation schema: `{ "score": 1-10, "blockers": [], "strengths": [] }`. The loop terminates as soon as $\text{score} \ge \text{threshold}$ (e.g. 8/10) or $\text{blockers.isEmpty()}$.
  2. **Strict Maximum Round Ceiling:** Enforce a hard round limit (e.g. $R \le 3$). If consensus is not reached by round 3, the supervisor or human is invoked to break the tie.
  3. **Diminishing Delta Halting:** If the delta between the proposals in round $k$ and round $k-1$ is below a convergence $\epsilon$, terminate the debate to avoid wasting tokens on trivial semantics.

---

### Q3: What is the Model Context Protocol (MCP), and why is it superior to traditional OpenAPI/Swagger specs for agents?
**Staff Answer:**
* **OpenAPI Limitations:** OpenAPI defines static HTTP REST endpoints for human developers. It does not standardize real-time resource subscriptions, progress notifications, or bidirectional streaming between an LLM client and host tools.
* **MCP Advantages:**
  1. **Standardized AI Context Primitives:** MCP natively models **Tools** (executable functions), **Resources** (passive context like files, database schemas), and **Prompts** (pre-configured templates).
  2. **Protocol Agnostic Transports:** Supports lightweight local subprocess communication via `stdio` and network communication via HTTP Server-Sent Events (SSE).
  3. **Security Boundaries:** Built-in client-side permission consent mechanisms before executing sensitive tool calls.

---

### Q4: How do you prevent "Context Explosion" when passing state between multiple agents?
**Staff Answer:**
* **Anti-Pattern:** Passing the entire cumulative conversation history of all agents to every worker. This quickly exceeds context limits and pollutes worker attention.
* **Production Best Practice: Scoped Handoff Schemas (Data Transfer Objects):**
  * Define strict, typed DTOs between agent boundaries:
    ```
    ResearcherAgent  ──► [ResearchBriefDTO] ──► DeveloperAgent
    DeveloperAgent   ──► [CodeArtifactDTO]  ──► QAEngineerAgent
    QAEngineerAgent  ──► [TestReportDTO]    ──► SupervisorAgent
    ```
  * Each worker receives **only** the DTO output of the upstream agent plus its own domain prompt, maintaining minimal context size ($< 1\text{K}$ tokens per turn).

---

### Q5: How do you handle Privilege Escalation and Security in Multi-Agent Handoffs?
**Staff Answer:**
* **The Vulnerability (Confused Deputy Problem):** An untrusted user gives a benign prompt to a low-privilege `TriageAgent`. The `TriageAgent` calls `handoffToDatabaseAdminAgent(sqlQuery)`, inadvertently executing unauthorized queries.
* **Defenses:**
  1. **Principal Propagation:** Pass the authenticated human user's Security Context (JWT / claims) across all agent boundaries. The downstream agent must verify that the *human user*, not just the upstream agent, has permission to execute the action.
  2. **Tool Scope Restriction:** Each agent worker must be initialized with its own sandboxed tool registry containing only the least privileges required for its specific role.
