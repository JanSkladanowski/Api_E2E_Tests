package com.example.e2etests.config;

import java.util.Locale;

public enum BrowserType {
    CHROME,
    FIREFOX,
    EDGE;

    public static BrowserType from(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Unsupported browser '" + value + "'. Choose chrome, firefox or edge.", exception);
        }
    }
}
