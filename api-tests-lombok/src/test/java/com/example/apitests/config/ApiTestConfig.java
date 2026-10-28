package com.example.apitests.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public final class ApiTestConfig {

    private static final String CONFIG_FILE = "api-test.properties";
    private static final String BASE_URL_KEY = "baseUrl";
    private static final String DEFAULT_BASE_URL = loadDefaultBaseUrl();

    private ApiTestConfig() {
    }

    public static String baseUrl() {
        return resolveBaseUrl(System.getProperty(BASE_URL_KEY), System.getenv("API_BASE_URL"), DEFAULT_BASE_URL);
    }

    static String resolveBaseUrl(String systemProperty, String environmentVariable, String fileDefault) {
        if (systemProperty != null) {
            return validatedUrl(systemProperty, "-D" + BASE_URL_KEY);
        }
        if (environmentVariable != null) {
            return validatedUrl(environmentVariable, "API_BASE_URL");
        }
        return validatedUrl(fileDefault, CONFIG_FILE + ":" + BASE_URL_KEY);
    }

    private static String loadDefaultBaseUrl() {
        Properties properties = new Properties();

        try (InputStream input = ApiTestConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("Configuration file not found: " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load configuration file: " + CONFIG_FILE, exception);
        }

        String baseUrl = properties.getProperty(BASE_URL_KEY);
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Property '" + BASE_URL_KEY + "' is missing or blank in " + CONFIG_FILE);
        }
        return validatedUrl(baseUrl, CONFIG_FILE + ":" + BASE_URL_KEY);
    }

    private static String validatedUrl(String value, String source) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Base URL from " + source + " must not be blank");
        }

        String normalized = value.trim().replaceFirst("/+$", "");
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid base URL from " + source, exception);
        }
        if (uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Base URL from " + source + " must be an absolute HTTP(S) URL");
        }
        return normalized;
    }
}
