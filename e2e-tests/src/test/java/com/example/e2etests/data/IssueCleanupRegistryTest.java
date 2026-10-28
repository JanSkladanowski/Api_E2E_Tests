package com.example.e2etests.data;

import com.example.e2etests.client.IssueCleanupClient;
import com.example.e2etests.support.TestResourceRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("framework")
class IssueCleanupRegistryTest {

    @Test
    void deletesConfirmedIssueByIdWithoutListingAllIssues() {
        FakeCleanupClient client = new FakeCleanupClient();
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-first");
        registry.registerCreatedIssueId("e2e-first", 42L);

        resources.cleanup();

        assertEquals(List.of(42L), client.deletedIds);
        assertEquals(0, client.lookupCalls);
    }

    @Test
    void findsUnconfirmedIssueByUniqueTitle() {
        FakeCleanupClient client = new FakeCleanupClient();
        client.matches.put("e2e-unconfirmed", 73L);
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-unconfirmed");

        resources.cleanup();

        assertEquals(List.of(73L), client.deletedIds);
        assertEquals(1, client.lookupCalls);
    }

    @Test
    void neverDeletesAmbiguousTitleMatches() {
        FakeCleanupClient client = new FakeCleanupClient();
        client.ambiguousTitles.add("e2e-ambiguous");
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-ambiguous");

        IllegalStateException failure = assertThrows(IllegalStateException.class, resources::cleanup);

        assertTrue(client.deletedIds.isEmpty());
        assertTrue(failure.getSuppressed()[0].getMessage().contains("Ambiguous cleanup title"));
    }

    @Test
    void attemptsRemainingDeletesAfterOneFails() {
        FakeCleanupClient client = new FakeCleanupClient();
        client.failingIds.add(2L);
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-first");
        registry.registerCreatedIssueId("e2e-first", 1L);
        registry.registerTitle("e2e-second");
        registry.registerCreatedIssueId("e2e-second", 2L);

        IllegalStateException failure = assertThrows(IllegalStateException.class, resources::cleanup);

        assertEquals(List.of(2L, 1L), client.attemptedIds);
        assertEquals(List.of(1L), client.deletedIds);
        assertEquals(1, failure.getSuppressed().length);
    }

    @Test
    void deletesConfirmedIssueEvenIfFallbackLookupFails() {
        FakeCleanupClient client = new FakeCleanupClient();
        client.failLookup = true;
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-unknown");
        registry.registerTitle("e2e-confirmed");
        registry.registerCreatedIssueId("e2e-confirmed", 91L);

        assertThrows(IllegalStateException.class, resources::cleanup);

        assertEquals(List.of(91L), client.deletedIds);
        assertEquals(1, client.lookupCalls);
    }

    @Test
    void continuesToConfirmedIssueWhenEarlierCleanupActionLookupFails() {
        FakeCleanupClient client = new FakeCleanupClient();
        client.failLookup = true;
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry issues = new IssueCleanupRegistry(resources, client);
        issues.registerTitle("e2e-confirmed");
        issues.registerCreatedIssueId("e2e-confirmed", 91L);
        issues.registerTitle("e2e-unknown");

        assertThrows(IllegalStateException.class, resources::cleanup);

        assertEquals(List.of(91L), client.deletedIds);
        assertEquals(1, client.lookupCalls);
    }

    @Test
    void acceptsNoMatchOnlyForUnconfirmedCreation() {
        FakeCleanupClient client = new FakeCleanupClient();
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry registry = new IssueCleanupRegistry(resources, client);
        registry.registerTitle("e2e-not-created");

        resources.cleanup();

        assertTrue(client.deletedIds.isEmpty());
        assertEquals(1, client.lookupCalls);
    }

    @Test
    void cleansUpIssueBeforePreviouslyRegisteredOtherResource() {
        FakeCleanupClient client = new FakeCleanupClient();
        TestResourceRegistry resources = new TestResourceRegistry();
        resources.register("project 10", () -> client.cleanupOrder.add("project"));
        IssueCleanupRegistry issues = new IssueCleanupRegistry(resources, client);
        issues.registerTitle("e2e-issue");
        issues.registerCreatedIssueId("e2e-issue", 42L);

        resources.cleanup();

        assertEquals(List.of(42L), client.deletedIds);
        assertEquals(List.of("issue 42", "project"), client.cleanupOrder);
        assertEquals(0, client.lookupCalls);
    }

    @Test
    void registeringSameTitleTwiceDoesNotDeleteIssueTwice() {
        FakeCleanupClient client = new FakeCleanupClient();
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry issues = new IssueCleanupRegistry(resources, client);
        issues.registerTitle("e2e-once");
        issues.registerTitle("e2e-once");
        issues.registerCreatedIssueId("e2e-once", 42L);

        resources.cleanup();

        assertEquals(List.of(42L), client.attemptedIds);
    }

    @Test
    void rejectsConflictingIssueIdWithoutChangingRegisteredCleanup() {
        FakeCleanupClient client = new FakeCleanupClient();
        TestResourceRegistry resources = new TestResourceRegistry();
        IssueCleanupRegistry issues = new IssueCleanupRegistry(resources, client);
        issues.registerTitle("e2e-conflict");
        issues.registerCreatedIssueId("e2e-conflict", 42L);

        assertThrows(IllegalStateException.class,
                () -> issues.registerCreatedIssueId("e2e-conflict", 43L));
        resources.cleanup();

        assertEquals(List.of(42L), client.deletedIds);
    }

    private static final class FakeCleanupClient implements IssueCleanupClient {
        private final Map<String, Long> matches = new HashMap<>();
        private final Set<String> ambiguousTitles = new HashSet<>();
        private final Set<Long> failingIds = new HashSet<>();
        private final List<Long> attemptedIds = new ArrayList<>();
        private final List<Long> deletedIds = new ArrayList<>();
        private final List<String> cleanupOrder = new ArrayList<>();
        private int lookupCalls;
        private boolean failLookup;

        @Override
        public OptionalLong findIssueIdByExactTitle(String title) {
            lookupCalls++;
            if (failLookup) {
                throw new IllegalStateException("Issue lookup failed");
            }
            if (ambiguousTitles.contains(title)) {
                throw new IllegalStateException("Ambiguous cleanup title '" + title + "'");
            }
            Long id = matches.get(title);
            return id == null ? OptionalLong.empty() : OptionalLong.of(id);
        }

        @Override
        public void deleteIssue(long issueId) {
            attemptedIds.add(issueId);
            cleanupOrder.add("issue " + issueId);
            if (failingIds.contains(issueId)) {
                throw new IllegalStateException("Could not delete issue " + issueId);
            }
            deletedIds.add(issueId);
        }
    }
}
