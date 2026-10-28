package com.example.apitests.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTestConfigTest {

    @Test
    @ResourceLock("system-properties")
    void publicEntryPointUsesAndValidatesJvmOverride() {
        String previous = System.getProperty("baseUrl");
        try {
            System.setProperty("baseUrl", "https://jvm.example.test/");
            assertEquals("https://jvm.example.test", ApiTestConfig.baseUrl());

            System.setProperty("baseUrl", " ");
            assertThrows(IllegalArgumentException.class, ApiTestConfig::baseUrl);
        } finally {
            if (previous == null) {
                System.clearProperty("baseUrl");
            } else {
                System.setProperty("baseUrl", previous);
            }
        }
    }

    @Test
    void systemPropertyTakesPrecedenceOverEnvironmentAndFile() {
        assertEquals("https://jvm.example.test:8443",
                ApiTestConfig.resolveBaseUrl("https://jvm.example.test:8443/",
                        "https://env.example.test", "http://file.example.test"));
    }

    @Test
    void environmentTakesPrecedenceOverFile() {
        assertEquals("https://env.example.test",
                ApiTestConfig.resolveBaseUrl(null, "https://env.example.test", "http://file.example.test"));
    }

    @Test
    void fileValueIsUsedWhenThereAreNoOverrides() {
        assertEquals("http://file.example.test",
                ApiTestConfig.resolveBaseUrl(null, null, "http://file.example.test"));
    }

    @Test
    void blankOverrideFailsInsteadOfSilentlyUsingAnotherEnvironment() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiTestConfig.resolveBaseUrl(" ", "https://env.example.test", "http://file.example.test"));
        assertTrue(exception.getMessage().contains("-DbaseUrl"));
    }

    @Test
    void invalidUrlFailsWithItsSource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ApiTestConfig.resolveBaseUrl(null, "ftp://env.example.test", "http://file.example.test"));
        assertTrue(exception.getMessage().contains("API_BASE_URL"));
    }
}
