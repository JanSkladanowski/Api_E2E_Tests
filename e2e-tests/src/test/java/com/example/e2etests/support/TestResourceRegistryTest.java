package com.example.e2etests.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("framework")
class TestResourceRegistryTest {

    @Test
    void executesActionsInReverseRegistrationOrder() {
        TestResourceRegistry resources = new TestResourceRegistry();
        List<String> completed = new ArrayList<>();
        resources.register("project 1", () -> completed.add("project"));
        resources.register("issue 2", () -> completed.add("issue"));

        resources.cleanup();
        resources.cleanup();

        assertEquals(List.of("issue", "project"), completed);
    }

    @Test
    void attemptsRemainingActionsAfterFailure() {
        TestResourceRegistry resources = new TestResourceRegistry();
        List<String> attempted = new ArrayList<>();
        resources.register("project 1", () -> attempted.add("project"));
        resources.register("issue 2", () -> {
            attempted.add("issue");
            throw new IllegalStateException("issue delete failed");
        });
        resources.register("comment 3", () -> attempted.add("comment"));

        IllegalStateException failure = assertThrows(IllegalStateException.class, resources::cleanup);

        assertEquals(List.of("comment", "issue", "project"), attempted);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("issue delete failed", failure.getSuppressed()[0].getMessage());
    }

    @Test
    void registriesDoNotShareActions() {
        TestResourceRegistry first = new TestResourceRegistry();
        TestResourceRegistry second = new TestResourceRegistry();
        List<String> completed = new ArrayList<>();
        first.register("first", () -> completed.add("first"));
        second.register("second", () -> completed.add("second"));

        first.cleanup();
        assertEquals(List.of("first"), completed);

        second.cleanup();
        assertEquals(List.of("first", "second"), completed);
    }

    @Test
    void rejectsInvalidRegistration() {
        TestResourceRegistry resources = new TestResourceRegistry();

        assertThrows(IllegalArgumentException.class, () -> resources.register(" ", () -> {}));
        assertThrows(NullPointerException.class, () -> resources.register("issue", null));
    }
}
