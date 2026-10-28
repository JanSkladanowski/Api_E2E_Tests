package com.example.e2etests.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TestResourceRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestResourceRegistry.class);

    private final List<CleanupAction> actions = new ArrayList<>();

    public void register(String description, Runnable cleanup) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Cleanup description must not be blank");
        }
        actions.add(new CleanupAction(description, Objects.requireNonNull(cleanup, "cleanup")));
        LOGGER.info("CLEANUP REGISTER resource='{}'", description);
    }

    public void cleanup() {
        int registered = actions.size();
        LOGGER.info("CLEANUP START registeredActions={}", registered);
        RuntimeException cleanupFailure = null;
        int completed = 0;

        for (int index = actions.size() - 1; index >= 0; index--) {
            CleanupAction action = actions.get(index);
            try {
                action.cleanup().run();
                completed++;
                LOGGER.info("CLEANUP ACTION COMPLETE resource='{}'", action.description());
            } catch (RuntimeException exception) {
                if (cleanupFailure == null) {
                    cleanupFailure = new IllegalStateException("One or more test resources could not be cleaned up");
                }
                cleanupFailure.addSuppressed(exception);
                LOGGER.error("CLEANUP ACTION FAILED resource='{}' reason={}",
                        action.description(), exception.toString());
            }
        }
        actions.clear();

        if (cleanupFailure != null) {
            LOGGER.error("CLEANUP FAILED registeredActions={} completed={} failed={}",
                    registered, completed, cleanupFailure.getSuppressed().length);
            throw cleanupFailure;
        }
        LOGGER.info("CLEANUP COMPLETE completedActions={}", completed);
    }

    private record CleanupAction(String description, Runnable cleanup) {
    }
}
