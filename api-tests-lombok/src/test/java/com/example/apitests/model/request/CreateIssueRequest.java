package com.example.apitests.model.request;

import lombok.Builder;

@Builder
public record CreateIssueRequest(
        String title,
        String description,
        String priority
) {
}
