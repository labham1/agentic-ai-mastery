package com.agentic.ai.guardrails;

/**
 * Thrown when an input or output violates enterprise AI security policies.
 */
public class GuardrailException extends RuntimeException {

    private final String violationCategory;

    public GuardrailException(String violationCategory, String message) {
        super(message);
        this.violationCategory = violationCategory;
    }

    public String getViolationCategory() {
        return violationCategory;
    }
}
