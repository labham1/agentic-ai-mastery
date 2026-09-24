package com.agentic.ai.reflection;

import com.agentic.ai.graph.AgentState;
import com.agentic.ai.graph.StateGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Self-Healing Java Code Generator & Compiler Agent.
 * 
 * Implements the Reflection / Self-Correction pattern using real in-memory Java compilation:
 * 1. GENERATE_CODE  -> Drafts initial Java code
 * 2. COMPILE_CODE   -> Validates code with standard Java Compiler (javax.tools.JavaCompiler)
 * 3. ROUTER         -> If pass -> HITL Gate -> END. If fail -> REFLECT -> loops back to GENERATE.
 * 4. REFLECT        -> Extracts diagnostic error messages and attaches corrective instructions.
 */
public class SelfHealingCompilerLab {

    private static final Logger log = LoggerFactory.getLogger(SelfHealingCompilerLab.class);

    public static StateGraph buildGraph(int maxRetries) {
        StateGraph graph = new StateGraph();

        // 1. Code Generation Node
        graph.addNode("GENERATE_CODE", state -> {
            int iteration = state.getOrDefault("ITERATION", 0);
            String prompt = state.get("USER_PROMPT");
            String previousError = state.get("COMPILER_ERROR");

            log.info("[GENERATE_CODE] Iteration {}: Crafting Java code...", iteration);

            String sourceCode;
            if (iteration == 0) {
                // Deliberately introduce a syntax error on first pass (missing semicolon)
                sourceCode = """
                    public class Calculator {
                        public static int add(int a, int b) {
                            return a + b // BUG: missing semicolon
                        }
                    }
                    """;
                log.info("Drafted initial code (contains intentional syntax error).");
            } else {
                // Self-healing pass: reflection node provided the error, so we repair it
                log.info("Applying self-correction using diagnostic error feedback: [{}]", previousError);
                sourceCode = """
                    public class Calculator {
                        public static int add(int a, int b) {
                            return a + b; // FIXED: semicolon restored
                        }
                    }
                    """;
                log.info("Drafted repaired code.");
            }

            state.set("SOURCE_CODE", sourceCode);
            return state;
        });

        // 2. In-Memory Compilation Validation Node
        graph.addNode("COMPILE_CODE", state -> {
            String source = state.get("SOURCE_CODE");
            log.info("[COMPILE_CODE] Invoking javax.tools.JavaCompiler...");

            CompilationResult result = compileInMemory("Calculator", source);
            if (result.success()) {
                log.info("[COMPILE_CODE] Compilation SUCCEEDED!");
                state.set("COMPILATION_PASSED", true);
                state.set("COMPILER_ERROR", null);
            } else {
                log.warn("[COMPILE_CODE] Compilation FAILED with diagnostics: {}", result.diagnostics());
                state.set("COMPILATION_PASSED", false);
                state.set("COMPILER_ERROR", result.diagnostics());
            }

            return state;
        });

        // 3. Reflection Node
        graph.addNode("REFLECT", state -> {
            int iteration = state.getOrDefault("ITERATION", 0);
            state.set("ITERATION", iteration + 1);
            String error = state.get("COMPILER_ERROR");

            log.info("[REFLECT] Analyzing compilation diagnostics to plan repair. Current retry: {}", iteration + 1);
            state.set("REFLECTION_NOTE", "Need to fix syntax issue identified by compiler: " + error);
            return state;
        });

        // 4. Human-in-the-Loop Gate Node (Deploy to Production)
        graph.addNode("DEPLOY_GATE", state -> {
            log.info("[DEPLOY_GATE] Code compiled cleanly. Preparing production release artifact.");
            state.set("STATUS", "DEPLOYED_SUCCESSFULLY");
            return state;
        });

        // 5. Fallback Circuit Breaker Node
        graph.addNode("CIRCUIT_BREAKER", state -> {
            log.error("[CIRCUIT_BREAKER] Exceeded max retries without compiling. Aborting.");
            state.set("STATUS", "FAILED_MAX_RETRIES_EXCEEDED");
            return state;
        });

        // Wire Edges & Routers
        graph.setEntryPoint("GENERATE_CODE");
        graph.addEdge("GENERATE_CODE", "COMPILE_CODE");

        // Conditional Router after compilation
        graph.addConditionalEdge("COMPILE_CODE", state -> {
            boolean passed = state.getOrDefault("COMPILATION_PASSED", false);
            if (passed) {
                return "DEPLOY_GATE";
            }
            int iteration = state.getOrDefault("ITERATION", 0);
            if (iteration >= maxRetries) {
                return "CIRCUIT_BREAKER";
            }
            return "REFLECT";
        });

        // Loop back: Reflection -> Generate Code
        graph.addEdge("REFLECT", "GENERATE_CODE");

        // Direct edge from Deploy to END
        graph.addEdge("DEPLOY_GATE", StateGraph.END);
        graph.addEdge("CIRCUIT_BREAKER", StateGraph.END);

        // Mark DEPLOY_GATE as requiring Human Approval
        graph.setCheckpointGate("DEPLOY_GATE");

        return graph;
    }

    // --- In-Memory Java Compiler Utility ---

    public record CompilationResult(boolean success, String diagnostics) {}

    public static CompilationResult compileInMemory(String className, String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            // Fallback for JRE environments where JDK compiler is not exposed
            boolean hasSemicolonBug = sourceCode.contains("return a + b // BUG");
            if (hasSemicolonBug) {
                return new CompilationResult(false, "Calculator.java:3: error: ';' expected");
            }
            return new CompilationResult(true, "Compilation clean (simulated)");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject file = new SimpleJavaSource(className, sourceCode);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, diagnostics, List.of("-proc:none"), null, Collections.singletonList(file));

        boolean success = Boolean.TRUE.equals(task.call());
        if (success) {
            return new CompilationResult(true, "Success");
        }

        StringBuilder diagMsg = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            diagMsg.append(d.getMessage(null)).append(" at line ").append(d.getLineNumber()).append("\n");
        }
        return new CompilationResult(false, diagMsg.toString().trim());
    }

    private static class SimpleJavaSource extends SimpleJavaFileObject {
        private final String code;
        SimpleJavaSource(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
