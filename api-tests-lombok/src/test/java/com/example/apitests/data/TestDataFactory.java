package com.example.apitests.data;

import com.example.apitests.model.request.CreateIssueRequest;

import java.util.UUID;

public final class TestDataFactory {

    private static final String DEFAULT_ISSUE_DESCRIPTION = "Issue created by an automated test";

    private TestDataFactory() {
    }

    public static String uniqueValue(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return prefix + "-" + suffix;
    }

    public static String uniqueIssueTitle() {
        return uniqueValue("automated-issue");
    }

    public static CreateIssueRequest validIssueRequest(String title, String priority) {
        return CreateIssueRequest.builder()
                .title(title)
                .description(DEFAULT_ISSUE_DESCRIPTION)
                .priority(priority)
                .build();
    }
}
