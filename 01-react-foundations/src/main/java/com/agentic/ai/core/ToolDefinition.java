package com.agentic.ai.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the metadata and reflection handle of an agent tool.
 */
public record ToolDefinition(
        String name,
        String description,
        Object targetInstance,
        Method method,
        List<ParameterDefinition> parameters
) {
    public record ParameterDefinition(
            String name,
            Class<?> type,
            String description,
            boolean required
    ) {}

    /**
     * Translates this Java tool into standard JSON Schema structure required by LLMs.
     */
    public Map<String, Object> toJsonSchema() {
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        List<String> requiredNames = new java.util.ArrayList<>();

        for (ParameterDefinition param : parameters) {
            Map<String, Object> paramMeta = new java.util.LinkedHashMap<>();
            paramMeta.put("type", mapJavaTypeToJsonType(param.type()));
            paramMeta.put("description", param.description());
            properties.put(param.name(), paramMeta);

            if (param.required()) {
                requiredNames.add(param.name());
            }
        }

        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("name", name);
        schema.put("description", description);

        Map<String, Object> paramsObj = new java.util.LinkedHashMap<>();
        paramsObj.put("type", "object");
        paramsObj.put("properties", properties);
        paramsObj.put("required", requiredNames);

        schema.put("parameters", paramsObj);
        return schema;
    }

    private static String mapJavaTypeToJsonType(Class<?> type) {
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return "integer";
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "number";
        } else if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        } else if (type.isArray() || List.class.isAssignableFrom(type)) {
            return "array";
        }
        return "string";
    }
}
