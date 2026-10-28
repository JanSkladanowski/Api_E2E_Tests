package com.example.issuehub.issue;

import java.time.Instant;

public record Issue(
        Long id,
        String title,
        String description,
        IssuePriority priority,
        IssueStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
