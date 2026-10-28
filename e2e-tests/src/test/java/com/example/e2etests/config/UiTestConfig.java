package com.example.e2etests.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;

public record UiTestConfig(
        String uiBaseUrl,
        String apiBaseUrl,
        BrowserType browser,
        boolean headless,
        String browserBinary,
        int windowWidth,
        int windowHeight,
        Duration waitTimeout,
        Duration cleanupTimeout
) {
    private static final String CONFIG_FILE = "ui-test.properties";

    public static UiTestConfig load() {
        Properties properties = new Properties();
        try (InputStream input = UiTestConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Missing test configuration: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + CONFIG_FILE, exception);
        }

        BrowserType browser = BrowserType.from(value(properties, "browser"));
        String browserBinary = browser == BrowserType.CHROME
                ? value(properties, "browserBinary", "chromeBinary")
                : value(properties, "browserBinary");

        return new UiTestConfig(
                url(value(properties, "uiBaseUrl", "baseUrl")),
                url(value(properties, "apiBaseUrl")),
                browser,
                booleanValue(value(properties, "headless"), "headless"),
                browserBinary,
                positiveInt(value(properties, "windowWidth"), "windowWidth"),
                positiveInt(value(properties, "windowHeight"), "windowHeight"),
                Duration.ofSeconds(positiveInt(value(properties, "waitSeconds"), "waitSeconds")),
                Duration.ofSeconds(positiveInt(value(properties, "cleanupTimeoutSeconds"), "cleanupTimeoutSeconds"))
        );
    }

    private static String value(Properties properties, String key, String... legacyKeys) {
        String override = System.getProperty(key);
        if (override != null) {
            return override.trim();
        }
        for (String legacyKey : legacyKeys) {
            override = System.getProperty(legacyKey);
            if (override != null) {
                return override.trim();
            }
        }
        String configured = properties.getProperty(key);
        if (configured == null) {
            throw new IllegalStateException("Missing property '" + key + "' in " + CONFIG_FILE);
        }
        return configured.trim();
    }

    private static String url(String value) {
        URI uri = URI.create(value);
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Expected an absolute HTTP URL, received: " + value);
        }
        return value.replaceFirst("/+$", "");
    }

    private static boolean booleanValue(String value, String key) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Property '" + key + "' must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static int positiveInt(String value, String key) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // A single useful configuration error is reported below.
        }
        throw new IllegalArgumentException("Property '" + key + "' must be a positive integer");
    }
}
