package com.example.issuehub.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must contain between 3 and 100 characters")
        String title,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Priority is required")
        IssuePriority priority
) {
}
