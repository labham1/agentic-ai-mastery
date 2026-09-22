# Agentic AI Mastery (Java Enterprise Edition)

A production-grade, multi-module repository designed to take you from foundational concepts to Staff-level Agentic AI architecture in **Java**.

---

## 🧭 Repository Structure

```
agentic-ai-mastery/
├── pom.xml                                      # Master Aggregator POM
├── README.md                                    # This guide
├── docs/                                        # Staff-Level Interview Prep & System Design
│   ├── SYSTEM_DESIGN_INTERVIEW_CHEATSHEET.md    # 7-step blueprint to ace AI Agent design questions
│   ├── AGENTIC_PATTERNS_TAXONOMY.md             # ReAct, Plan-and-Solve, Reflection, Multi-Agent
│   └── GLOSSARY_AND_FORMULAS.md                 # Mathematical & conceptual foundations
│
├── 01-react-foundations/                        # Module 1: The Core ReAct Engine
│   ├── CONCEPT_NOTES.md                         # Deep dive into ReAct, Function Calling & JSON Schemas
│   ├── INTERVIEW_QA.md                          # 10+ hard interview questions & staff answers
│   └── src/                                     # Tool Registry, Autonomous Loop & LangChain4j runner
│
├── 02-tools-and-memory/                         # (Phase 2) Dynamic Tools, Sliding Window & RAG
├── 03-stateful-agent-graphs/                    # (Phase 3) Cyclic Graphs & Self-Healing Code
├── 04-multi-agent-orchestration/                # (Phase 4) Supervisor, Debate & MCP Protocols
└── 05-enterprise-production/                    # (Phase 5) Spring AI, Guardrails & OpenTelemetry
```

---

## 🚀 Getting Started

### 1. Open in Your IDE
Open the folder `agentic-ai-mastery` in **IntelliJ IDEA**, **Eclipse**, or **VS Code**.
- As a Maven project, your IDE will automatically import the parent `pom.xml` and submodules.
- Requires **JDK 17** or higher.

### 2. Configure Your Free Gemini API Key
Get your free API key at [Google AI Studio](https://aistudio.google.com/).

In your IDE's Run/Debug Configuration, set the environment variable:
```bash
GEMINI_API_KEY=your_gemini_api_key_here
```
*(Or set it globally in Windows Environment Variables).*

### 3. Run Your First Agent
Open `01-react-foundations/src/main/java/com/agentic/ai/AgentRunner.java` and click **Run**!
You can test tool-calling, multi-step reasoning, and watch the agent decide and execute Java methods in real time.

### 4. Run Unit Tests (No API Key Required)
Run `ToolRegistryTest.java` to see how Java reflection inspects methods, validates `@Tool` annotations, generates JSON Schema definitions, and executes methods dynamically.
