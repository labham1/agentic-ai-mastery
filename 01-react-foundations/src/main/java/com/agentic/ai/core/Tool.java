package com.agentic.ai.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Java method as an executable Agent Tool.
 * The runtime inspects this annotation to generate JSON Schemas for LLM function calling.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {
    /**
     * Clear, concise description of what the tool accomplishes.
     * The LLM uses this description to decide when to invoke the tool.
     */
    String value();
}
