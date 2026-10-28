package com.example.apitests.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TestResourceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourceRegistry.class);

    private final List<CleanupHandle> actions = new ArrayList<>();

    public CleanupHandle register(String description, Runnable cleanup) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Cleanup description must not be blank");
        }
        CleanupHandle handle = new CleanupHandle(description, Objects.requireNonNull(cleanup, "cleanup"));
        actions.add(handle);
        LOGGER.info("CLEANUP REGISTER resource='{}'", description);
        return handle;
    }

    public void cleanup() {
        int registered = actions.size();
        LOGGER.info("CLEANUP START registeredActions={}", registered);
        RuntimeException cleanupFailure = null;
        int completed = 0;
        int skipped = 0;

        for (int index = actions.size() - 1; index >= 0; index--) {
            CleanupHandle action = actions.get(index);
            if (action.completed) {
                skipped++;
                LOGGER.info("CLEANUP SKIP already removed resource='{}'", action.description);
                continue;
            }
            try {
                action.cleanup.run();
                completed++;
                LOGGER.info("CLEANUP ACTION COMPLETE resource='{}'", action.description);
            } catch (RuntimeException exception) {
                if (cleanupFailure == null) {
                    cleanupFailure = new IllegalStateException("One or more test resources could not be cleaned up");
                }
                cleanupFailure.addSuppressed(exception);
                LOGGER.error("CLEANUP ACTION FAILED resource='{}' reason={}",
                        action.description, exception.toString());
            }
        }
        actions.clear();

        if (cleanupFailure != null) {
            LOGGER.error("CLEANUP FAILED registeredActions={} completed={} skipped={} failed={}",
                    registered,
                    completed,
                    skipped,
                    cleanupFailure.getSuppressed().length);
            throw cleanupFailure;
        }
        LOGGER.info("CLEANUP COMPLETE completedActions={} skippedActions={}", completed, skipped);
    }

    public static final class CleanupHandle {
        private final String description;
        private final Runnable cleanup;
        private boolean completed;

        private CleanupHandle(String description, Runnable cleanup) {
            this.description = description;
            this.cleanup = cleanup;
        }

        public void markCompleted() {
            if (!completed) {
                completed = true;
                LOGGER.info("CLEANUP MARK COMPLETE resource='{}'", description);
            }
        }
    }
}
