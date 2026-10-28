package com.example.e2etests.client;

import java.util.OptionalLong;

public interface IssueCleanupClient {

    OptionalLong findIssueIdByExactTitle(String title);

    void deleteIssue(long issueId);
}
