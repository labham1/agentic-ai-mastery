package com.agentic.ai.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Tool Registry.
 * Responsible for discovering @Tool annotations via reflection, generating
 * standard JSON Schemas, and executing tools with robust type conversion and error boundaries.
 */
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Scans an object instance and registers all methods annotated with @Tool.
     */
    public void registerTools(Object toolInstance) {
        Class<?> clazz = toolInstance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                String toolName = method.getName();
                String description = toolAnnotation.value();

                List<ToolDefinition.ParameterDefinition> params = new ArrayList<>();
                for (Parameter param : method.getParameters()) {
                    String paramName = param.getName();
                    String paramDesc = "Parameter " + paramName;
                    boolean required = true;

                    if (param.isAnnotationPresent(ToolParam.class)) {
                        ToolParam pAnn = param.getAnnotation(ToolParam.class);
                        paramDesc = pAnn.value();
                        required = pAnn.required();
                    }

                    params.add(new ToolDefinition.ParameterDefinition(
                            paramName,
                            param.getType(),
                            paramDesc,
                            required
                    ));
                }

                method.setAccessible(true);
                ToolDefinition definition = new ToolDefinition(toolName, description, toolInstance, method, params);
                tools.put(toolName, definition);
                log.info("Registered Agent Tool: [{}] - {}", toolName, description);
            }
        }
    }

    /**
     * Executes a tool by name with arguments mapped from a JSON payload.
     * Guaranteed never to throw an unhandled exception to prevent crashing the agent cycle.
     */
    public ToolExecutionResult execute(String toolName, Map<String, Object> arguments) {
        long startTime = System.currentTimeMillis();
        ToolDefinition tool = tools.get(toolName);

        if (tool == null) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.failure(
                    toolName,
                    "Tool '" + toolName + "' not found. Available tools: " + tools.keySet(),
                    duration
            );
        }

        try {
            Method method = tool.method();
            List<ToolDefinition.ParameterDefinition> paramDefs = tool.parameters();
            Object[] args = new Object[paramDefs.size()];

            for (int i = 0; i < paramDefs.size(); i++) {
                ToolDefinition.ParameterDefinition pDef = paramDefs.get(i);
                Object rawValue = arguments != null ? arguments.get(pDef.name()) : null;

                if (rawValue == null && pDef.required()) {
                    return ToolExecutionResult.failure(
                            toolName,
                            "Missing required argument: '" + pDef.name() + "'",
                            System.currentTimeMillis() - startTime
                    );
                }

                if (rawValue != null) {
                    // Robust type coercion using Jackson
                    args[i] = objectMapper.convertValue(rawValue, pDef.type());
                } else {
                    args[i] = null;
                }
            }

            Object result = method.invoke(tool.targetInstance(), args);
            long duration = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.success(toolName, result, duration);

        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            log.error("Error executing tool [{}] with args {}: {}", toolName, arguments, cause.getMessage());
            return ToolExecutionResult.failure(
                    toolName,
                    "Execution error in " + toolName + ": " + cause.getMessage(),
                    duration
            );
        }
    }

    public List<Map<String, Object>> getAllJsonSchemas() {
        return tools.values().stream()
                .map(ToolDefinition::toJsonSchema)
                .toList();
    }

    public Collection<ToolDefinition> getTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}
