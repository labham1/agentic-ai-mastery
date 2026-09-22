package com.agentic.ai;

import com.agentic.ai.core.ToolExecutionResult;
import com.agentic.ai.core.ToolRegistry;
import com.agentic.ai.tools.SystemTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ToolRegistryTest {

    private ToolRegistry registry;
    private SystemTools systemTools;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
        systemTools = new SystemTools();
        registry.registerTools(systemTools);
    }

    @Test
    @DisplayName("Should discover and register all @Tool annotated methods")
    void shouldRegisterAllTools() {
        assertThat(registry.hasTool("getCurrentDateTime")).isTrue();
        assertThat(registry.hasTool("calculate")).isTrue();
        assertThat(registry.hasTool("calculateSquareRoot")).isTrue();
        assertThat(registry.hasTool("listDirectory")).isTrue();
        assertThat(registry.hasTool("getWeather")).isTrue();
        assertThat(registry.getTools()).hasSize(5);
    }

    @Test
    @DisplayName("Should generate valid LLM JSON Schema for registered tools")
    void shouldGenerateJsonSchemas() {
        List<Map<String, Object>> schemas = registry.getAllJsonSchemas();
        assertThat(schemas).isNotEmpty();

        Map<String, Object> calcSchema = schemas.stream()
                .filter(s -> "calculate".equals(s.get("name")))
                .findFirst()
                .orElseThrow();

        assertThat(calcSchema).containsKey("description");
        assertThat(calcSchema).containsKey("parameters");

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) calcSchema.get("parameters");
        assertThat(params.get("type")).isEqualTo("object");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) params.get("properties");
        assertThat(properties).containsKeys("a", "b", "operation");
    }

    @Test
    @DisplayName("Should execute registered tool with automatic type conversion")
    void shouldExecuteToolSuccessfully() {
        // Pass integers; Jackson should coerce to double for the calculate method
        Map<String, Object> args = Map.of("a", 15, "b", 3, "operation", "divide");
        ToolExecutionResult result = registry.execute("calculate", args);

        assertThat(result.success()).isTrue();
        assertThat(result.result()).isEqualTo(5.0);
        assertThat(result.errorMessage()).isNull();
        assertThat(result.executionDurationMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.toObservationString()).isEqualTo("5.0");
    }

    @Test
    @DisplayName("Should gracefully handle arithmetic error inside tool without throwing unhandled exception")
    void shouldHandleToolExceptionGracefully() {
        // Division by zero
        Map<String, Object> args = Map.of("a", 10, "b", 0, "operation", "divide");
        ToolExecutionResult result = registry.execute("calculate", args);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Division by zero");
        assertThat(result.toObservationString()).startsWith("Tool Execution Error:");
    }

    @Test
    @DisplayName("Should report missing required parameters without crashing")
    void shouldHandleMissingParameters() {
        // Missing 'b' and 'operation'
        Map<String, Object> args = Map.of("a", 10);
        ToolExecutionResult result = registry.execute("calculate", args);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Missing required argument");
    }

    @Test
    @DisplayName("Should return failure when requested tool is not found")
    void shouldHandleUnknownTool() {
        ToolExecutionResult result = registry.execute("nonExistentTool", Map.of());

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Tool 'nonExistentTool' not found");
    }
}
