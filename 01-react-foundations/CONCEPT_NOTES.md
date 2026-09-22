# Concept Notes: Module 01 — The ReAct Pattern & Function Calling

## 1. What is an "Agent" vs. a Normal LLM?
A standard LLM is a **stateless, single-turn probability function**:
$$\text{Output} = \text{LLM}(\text{Input Prompt})$$

An **Agent** is a software system where the LLM is given an **execution loop** and **agency over its tools**:
1. It **observes** the environment.
2. It **reasons** about what to do next.
3. It **acts** by invoking external tools/APIs.
4. It **evaluates** the tool results and decides whether to continue or conclude.

---

## 2. The ReAct Architecture (Reason + Act)

Introduced by Yao et al. (ICLR 2023), ReAct solves two critical limitations of raw LLMs:
1. **Hallucination:** An LLM without tools guesses numbers, facts, and dates.
2. **Lack of Dynamic Context:** An LLM cannot query your company database or read your disk.

### The ReAct Cycle:
```
                      ┌───────────────────────────┐
                      │    User Input / Goal      │
                      └─────────────┬─────────────┘
                                    │
                       ┌────────────▼─────────────┐
                       │  Step 1: Thought         │
                       │  (LLM reasoning trace)   │
                       └────────────┬─────────────┘
                                    │
                                    ├──────────────────────────┐
                 Goal Not Complete? │                          │ Goal Completed
                                    ▼                          ▼
                       ┌──────────────────────────┐  ┌──────────────────┐
                       │  Step 2: Action          │  │ Final Answer to  │
                       │  (Tool & Arguments)      │  │ User             │
                       └────────────┬─────────────┘  └──────────────────┘
                                    │
                                    ▼
                       ┌──────────────────────────┐
                       │  Step 3: Observation     │
                       │  (Tool Execution Result) │
                       └────────────┬─────────────┘
                                    │
                                    └──── Loops back to Step 1
```

---

## 3. How Function / Tool Calling Works Under the Hood

### The Contract (JSON Schema)
When you register a Java method like:
```java
@Tool("Calculates the square root of a given number")
public double calculateSquareRoot(@ToolParam("Number to compute") double number)
```

The runtime translates this Java method into a standard **JSON Schema**:
```json
{
  "name": "calculateSquareRoot",
  "description": "Calculates the square root of a given number",
  "parameters": {
    "type": "object",
    "properties": {
      "number": {
        "type": "number",
        "description": "Number to compute"
      }
    },
    "required": ["number"]
  }
}
```

### The LLM Generation
1. This JSON Schema is sent to the LLM alongside your prompt.
2. The LLM does **NOT** execute the code. The LLM only generates a structured token stream matching the schema:
   ```json
   {
     "tool": "calculateSquareRoot",
     "arguments": {
       "number": 144
     }
   }
   ```
3. Your Java host application parses this JSON, locates the matching method via reflection, invokes `calculateSquareRoot(144)`, and captures the result `12.0`.
4. Your application appends the result as a `TOOL_EXECUTION_RESULT` message back into the conversation history, and calls the LLM again.

---

## 4. Key Design Patterns in Java for Agentic AI

### 1. Strategy Pattern (Tool Invocation)
Each tool is an implementation of an executable strategy. The LLM acts as the dynamic strategy selector based on user intent.

### 2. Command Pattern (Action Decoupling)
Tool calls are serialized as command objects containing:
* `toolName`: The method to invoke
* `arguments`: Map of arguments
* `executionId`: Tracing identifier

### 3. Circuit Breaker / Halting Guard
Because an agent operates in an open while-loop, **you must enforce a hard upper bound** (`maxIterations`, e.g., 10 steps). Without this, an agent can get trapped in an infinite loop burning money and API quotas.

---

## 5. Failure Modes to Guard Against

1. **Hallucinated Tool Names:** The LLM calls a tool that does not exist in the registry.
   * *Defense:* Catch `ToolNotFoundException` and return: `"Error: Tool 'foo' does not exist. Available tools are: [bar, baz]"`. The LLM will self-correct in the next step.
2. **Schema Mismatches / Bad Arguments:** The LLM passes a string when an integer was required.
   * *Defense:* Robust type coercion using Jackson (`ObjectMapper.convertValue()`).
3. **Tool Exceptions (500, Timeout):** If an API fails, never crash the agent process.
   * *Defense:* Catch all `Throwable` inside tool execution and return the error message as a string observation so the LLM can try an alternative plan.
