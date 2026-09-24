package com.agentic.ai;

import com.agentic.ai.guardrails.PiiMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PiiMaskerTest {

    @Test
    @DisplayName("Should redact credit card numbers, emails, SSNs and API keys")
    void shouldMaskAllPiiTypes() {
        String raw = "User Bob (bob.jones@fintech.com) card 4111-2222-3333-4444 and SSN 123-45-6789 key sk-abcdef1234567890abcdef";

        PiiMasker.MaskingResult result = PiiMasker.maskPii(raw);

        assertThat(result.redactionCount()).isEqualTo(4);
        assertThat(result.sanitizedText())
                .doesNotContain("4111-2222-3333-4444")
                .doesNotContain("bob.jones@fintech.com")
                .doesNotContain("123-45-6789")
                .doesNotContain("sk-abcdef1234567890abcdef")
                .contains("[REDACTED_CREDIT_CARD]")
                .contains("[REDACTED_EMAIL]")
                .contains("[REDACTED_SSN]")
                .contains("[REDACTED_API_KEY]");
    }

    @Test
    @DisplayName("Should return untouched string when no PII is present")
    void shouldNotModifyCleanText() {
        String clean = "What is the capital of Japan and its current population?";
        PiiMasker.MaskingResult result = PiiMasker.maskPii(clean);

        assertThat(result.redactionCount()).isEqualTo(0);
        assertThat(result.sanitizedText()).isEqualTo(clean);
    }
}
