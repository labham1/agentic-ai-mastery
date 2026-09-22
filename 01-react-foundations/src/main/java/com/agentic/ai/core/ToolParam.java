package com.agentic.ai.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for an individual parameter of a Tool method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ToolParam {
    /**
     * Description of the parameter passed to the LLM schema.
     */
    String value();

    /**
     * Whether this parameter is mandatory. Defaults to true.
     */
    boolean required() default true;
}
