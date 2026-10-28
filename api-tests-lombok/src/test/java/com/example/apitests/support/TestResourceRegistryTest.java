package com.example.apitests.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestResourceRegistryTest {

    @Test
    void cleansUpInReverseRegistrationOrder() {
        TestResourceRegistry resources = new TestResourceRegistry();
        List<String> completed = new ArrayList<>();
        resources.register("project 1", () -> completed.add("project"));
        resources.register("issue 2", () -> completed.add("issue"));

        resources.cleanup();
        resources.cleanup();

        assertEquals(List.of("issue", "project"), completed);
    }

    @Test
    void continuesAfterFailureAndReportsEveryFailure() {
        TestResourceRegistry resources = new TestResourceRegistry();
        List<String> attempted = new ArrayList<>();
        resources.register("project 1", () -> {
            attempted.add("project");
            throw new IllegalStateException("project delete failed");
        });
        resources.register("issue 2", () -> {
            attempted.add("issue");
            throw new IllegalStateException("issue delete failed");
        });
        resources.register("comment 3", () -> attempted.add("comment"));

        IllegalStateException failure = assertThrows(IllegalStateException.class, resources::cleanup);

        assertEquals(List.of("comment", "issue", "project"), attempted);
        assertEquals(2, failure.getSuppressed().length);
        assertEquals("issue delete failed", failure.getSuppressed()[0].getMessage());
        assertEquals("project delete failed", failure.getSuppressed()[1].getMessage());
    }

    @Test
    void eachRegistryOwnsOnlyItsRegisteredActions() {
        TestResourceRegistry first = new TestResourceRegistry();
        TestResourceRegistry second = new TestResourceRegistry();
        List<String> completed = new ArrayList<>();
        first.register("first issue", () -> completed.add("first"));
        second.register("second issue", () -> completed.add("second"));

        first.cleanup();

        assertEquals(List.of("first"), completed);
        second.cleanup();
        assertEquals(List.of("first", "second"), completed);
    }

    @Test
    void skipsOnlyActionMarkedCompleted() {
        TestResourceRegistry resources = new TestResourceRegistry();
        List<String> attempted = new ArrayList<>();
        resources.register("project 1", () -> attempted.add("project"));
        TestResourceRegistry.CleanupHandle issue = resources.register(
                "issue 2", () -> attempted.add("issue"));

        issue.markCompleted();
        issue.markCompleted();
        resources.cleanup();

        assertEquals(List.of("project"), attempted);
    }

    @Test
    void rejectsBlankDescriptionAndMissingAction() {
        TestResourceRegistry resources = new TestResourceRegistry();

        assertThrows(IllegalArgumentException.class, () -> resources.register(" ", () -> {}));
        assertThrows(NullPointerException.class, () -> resources.register("issue 1", null));
    }
}
