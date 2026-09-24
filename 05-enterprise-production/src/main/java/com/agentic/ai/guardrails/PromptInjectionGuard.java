package com.agentic.ai.guardrails;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enterprise Adversarial Prompt Injection & Jailbreak Defense Guard.
 * 
 * Inspects incoming prompts for known jailbreak heuristics, system prompt overrides,
 * and malicious delimiter manipulation.
 */
public class PromptInjectionGuard {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(?:all\\s+)?(?:previous|prior|above)\\s+(?:instructions|prompts|rules)"),
            Pattern.compile("(?i)disregard\\s+(?:all\\s+)?(?:previous|prior|above)\\s+(?:instructions|system\\s+commands)"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(?:in\\s+)?(?:developer\\s+mode|dan|jailbroken|unrestricted)"),
            Pattern.compile("(?i)bypass\\s+(?:all\\s+)?(?:safety|content|ethical)\\s+(?:filters|guidelines|policies)"),
            Pattern.compile("(?i)reveal\\s+(?:your\\s+)?(?:system\\s+prompt|hidden\\s+instructions|internal\\s+rules)"),
            Pattern.compile("(?i)override\\s+(?:system\\s+prompt|core\\s+directive)")
    );

    /**
     * Inspects prompt text. Throws GuardrailException if an injection pattern is detected.
     */
    public static void validateInput(String prompt) {
        if (prompt == null || prompt.isBlank()) return;

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                throw new GuardrailException(
                        "PROMPT_INJECTION_DETECTED",
                        "Security violation: Adversarial prompt injection or system override attempt detected."
                );
            }
        }
    }

    /**
     * Non-throwing inspection method returning boolean safety status.
     */
    public static boolean isSafe(String prompt) {
        try {
            validateInput(prompt);
            return true;
        } catch (GuardrailException e) {
            return false;
        }
    }
}
