package com.example.e2etests.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("framework")
class UiTestConfigTest {

    @Test
    @ResourceLock("system-properties")
    void systemPropertyOverridesBrowserFromPropertiesFile() {
        String previous = System.getProperty("browser");
        try {
            System.setProperty("browser", "firefox");
            assertEquals(BrowserType.FIREFOX, UiTestConfig.load().browser());
        } finally {
            restore("browser", previous);
        }
    }

    @Test
    @ResourceLock("system-properties")
    void rejectsInvalidTimeout() {
        String previous = System.getProperty("waitSeconds");
        try {
            System.setProperty("waitSeconds", "0");
            assertThrows(IllegalArgumentException.class, UiTestConfig::load);
        } finally {
            restore("waitSeconds", previous);
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
