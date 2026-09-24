# Concept Notes: Module 05 — Enterprise Production, Guardrails & Observability

## 1. The Enterprise Production Triad

Moving an Agentic AI system from prototype to production requires three non-negotiable operational pillars:

```
┌────────────────────────────────────────────────────────────────────────┐
│                      THE ENTERPRISE AI TRIAD                           │
├─────────────────────┬───────────────────────────┬──────────────────────┤
│    1. GUARDRAILS    │     2. OBSERVABILITY      │    3. EVALUATION     │
│   (Zero-Trust AI)   │   (OpenTelemetry/Spans)   │  (LLM-as-a-Judge)    │
│                     │                           │                      │
│ - Prompt Injection  │ - Tracing spans (LLM/Tool)│ - Faithfulness       │
│ - PII Masking       │ - Token & Cost tracking   │ - Answer Relevance   │
│ - Schema validation │ - Latency percentiles     │ - Tool Precision     │
└─────────────────────┴───────────────────────────┴──────────────────────┘
```

---

## 2. Guardrails: Defense-in-Depth for Agents

Because LLMs are vulnerable to adversarial inputs, we treat the LLM as an **untrusted, sandboxed compute engine**:

### Input Guardrails:
1. **Adversarial Prompt Injection Detection:**
   - Detects jailbreak attempts such as `"IGNORE ALL PREVIOUS INSTRUCTIONS"`, `"You are now DAN"`, or attempts to override system prompt constraints.
   - If detected, throws a `GuardrailException` immediately, preventing the prompt from reaching the model.
2. **PII Masking (Personally Identifiable Information):**
   - Uses regex and named-entity recognition (NER) to redact credit card numbers, SSNs, phone numbers, and API keys *before* the text is sent to third-party model providers.
   - Example: `"My card is 4532-1234-5678-9012"` $\rightarrow$ `"My card is [REDACTED_CREDIT_CARD]"`.

### Output Guardrails:
1. **Secret & Key Leakage Prevention:** Ensures the agent never echoes internal database passwords, API credentials, or internal system prompt tokens back to the user.
2. **JSON Schema Enforcement:** Validates that structured tool arguments or outputs strictly conform to the expected Java POJO schema.

---

## 3. Observability & Tracing (OpenInference / OpenTelemetry)

Traditional microservice APM tools (Datadog, Dynatrace) track HTTP latency and database queries. **Agentic AI requires tracing non-deterministic inference steps**:

### What an Agent Trace Span Captures:
* **Trace ID & Parent Span ID:** Maps the full tree of supervisor $\rightarrow$ worker $\rightarrow$ tool calls.
* **Model Parameters:** `model_name="gemini-2.0-flash"`, `temperature=0.1`.
* **Token Economics:** Input tokens, Output tokens, and cumulative estimated Dollar Cost.
* **Latency Profile:**
  * **TTFT (Time-To-First-Token):** Latency until the first streaming token arrives.
  * **Tool Execution Latency:** Time spent waiting for external databases/APIs.

---

## 4. Continuous Evaluation: The LLM-as-a-Judge Pattern

How do you unit-test a system whose output is natural language and non-deterministic?

### The RAG & Agent Triad Metrics:
1. **Faithfulness (Groundedness):**
   $$\text{Faithfulness} = \frac{|\text{Claims in Answer supported by Tool Context}|}{|\text{Total Claims in Answer}|}$$
   * Catches hallucinations where the model makes claims unsupported by the database.
2. **Answer Relevance:**
   * Evaluates whether the agent actually answered the specific question asked by the user, without wandering.
3. **Tool Selection Precision:**
   $$\text{Tool Precision} = \frac{|\text{Necessary Tool Calls Executed}|}{|\text{Total Tool Calls Executed}|}$$
   * Penalizes agents that make superfluous, expensive API calls.

---

## 5. The Spring AI Advisor Pattern (Aspect-Oriented Interceptors)

Spring AI introduces the **`Advisor` (or Interceptor)** pattern, applying Spring's battle-tested AOP (Aspect-Oriented Programming) concepts to agent calls:

```
User Request
     │
     ▼
[Input Guardrails Advisor]    ──► Redacts PII & blocks prompt injections
     │
     ▼
[Observability Advisor]       ──► Starts OpenTelemetry root span & timers
     │
     ▼
[Chat Memory Advisor]         ──► Injects sliding window / summary buffer
     │
     ▼
┌─────────────────────────┐
│     Foundation LLM      │
└────────────┬────────────┘
             │
             ▼
[Output Guardrails Advisor]   ──► Checks for toxicity & schema compliance
             │
             ▼
[Trace Finalizer Advisor]     ──► Records token usage, latency & closes span
             │
             ▼
Final Response to Client
```
