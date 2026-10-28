package com.example.e2etests.data;

import com.example.e2etests.client.IssueCleanupClient;
import com.example.e2etests.support.TestResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

public final class IssueCleanupRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueCleanupRegistry.class);

    private final TestResourceRegistry resources;
    private final IssueCleanupClient client;
    private final Map<String, RegisteredIssue> issuesByTitle = new LinkedHashMap<>();

    public IssueCleanupRegistry(TestResourceRegistry resources, IssueCleanupClient client) {
        this.resources = resources;
        this.client = client;
    }

    public void registerTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Cleanup title must not be blank");
        }
        if (issuesByTitle.containsKey(title)) {
            return;
        }

        RegisteredIssue issue = new RegisteredIssue(title);
        resources.register("issue title=" + title, () -> cleanupIssue(issue));
        issuesByTitle.put(title, issue);
    }

    public void registerCreatedIssueId(String title, long issueId) {
        RegisteredIssue issue = issuesByTitle.get(title);
        if (issue == null) {
            throw new IllegalArgumentException("Issue title was not registered for cleanup: " + title);
        }
        if (issueId <= 0) {
            throw new IllegalArgumentException("Cleanup issue ID must be positive");
        }
        if (issue.id != null && issue.id != issueId) {
            throw new IllegalStateException("Issue title has already been assigned a different ID: " + title);
        }
        issue.id = issueId;
        LOGGER.info("CLEANUP CONFIRM title='{}' id={}", title, issueId);
    }

    private void cleanupIssue(RegisteredIssue issue) {
        if (issue.id != null) {
            client.deleteIssue(issue.id);
            return;
        }

        OptionalLong match = client.findIssueIdByExactTitle(issue.title);
        if (match.isEmpty()) {
            LOGGER.info("CLEANUP NO MATCH unconfirmedTitle='{}'", issue.title);
        } else {
            client.deleteIssue(match.getAsLong());
        }
    }

    private static final class RegisteredIssue {
        private final String title;
        private Long id;

        private RegisteredIssue(String title) {
            this.title = title;
        }
    }
}
