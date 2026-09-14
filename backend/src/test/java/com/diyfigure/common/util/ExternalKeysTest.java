package com.diyfigure.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalKeysTest {

    @Test
    @DisplayName("空串、占位串、过短值都视为未配置")
    void placeholderAndBlankAreNotConfigured() {
        assertFalse(ExternalKeys.isConfigured(null));
        assertFalse(ExternalKeys.isConfigured(""));
        assertFalse(ExternalKeys.isConfigured("   "));
        assertFalse(ExternalKeys.isConfigured("your-openai-api-key"));
        assertFalse(ExternalKeys.isConfigured("your-meshy-api-key"));
        assertFalse(ExternalKeys.isConfigured("changeme"));
        assertFalse(ExternalKeys.isConfigured("short"));
    }

    @Test
    @DisplayName("看起来像真实密钥的值视为已配置")
    void realLookingKeyIsConfigured() {
        assertTrue(ExternalKeys.isConfigured("sk-proj-abcdefghijklmnopqrstuvwxyz"));
        assertTrue(HmacSigner.matches("secret-key-value", "a=1&b=2",
                HmacSigner.sign("secret-key-value", "a=1&b=2")));
        assertFalse(HmacSigner.matches("secret-key-value", "a=1&b=2", "deadbeef"));
    }
}
