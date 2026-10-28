package com.example.issuehub.issue;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        IssueStatus status
) {
}
