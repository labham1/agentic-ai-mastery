package com.agentic.ai.guardrails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enterprise PII (Personally Identifiable Information) Redaction Engine.
 * 
 * Sanitizes sensitive personal and financial data before text is transmitted
 * to external foundation LLM APIs.
 */
public class PiiMasker {

    // Regex patterns for common sensitive identifiers
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|(?:[0-9]{4}[-\\s]?){3}[0-9]{4})\\b"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern SSN_PATTERN = Pattern.compile(
            "\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b"
    );

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
            "\\b(?:sk-[a-zA-Z0-9]{20,}|AIza[0-9A-Za-z-_]{35})\\b"
    );

    public record MaskingResult(String sanitizedText, int redactionCount) {}

    public static MaskingResult maskPii(String input) {
        if (input == null || input.isBlank()) {
            return new MaskingResult(input, 0);
        }

        int count = 0;
        String text = input;

        // 1. Redact Credit Cards
        Matcher ccMatcher = CREDIT_CARD_PATTERN.matcher(text);
        if (ccMatcher.find()) {
            count++;
            text = ccMatcher.replaceAll("[REDACTED_CREDIT_CARD]");
        }

        // 2. Redact SSNs
        Matcher ssnMatcher = SSN_PATTERN.matcher(text);
        if (ssnMatcher.find()) {
            count++;
            text = ssnMatcher.replaceAll("[REDACTED_SSN]");
        }

        // 3. Redact API Keys
        Matcher keyMatcher = API_KEY_PATTERN.matcher(text);
        if (keyMatcher.find()) {
            count++;
            text = keyMatcher.replaceAll("[REDACTED_API_KEY]");
        }

        // 4. Redact Emails
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        if (emailMatcher.find()) {
            count++;
            text = emailMatcher.replaceAll("[REDACTED_EMAIL]");
        }

        return new MaskingResult(text, count);
    }
}
