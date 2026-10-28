package com.example.e2etests.data;

import java.util.UUID;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static String uniqueIssueTitle(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
