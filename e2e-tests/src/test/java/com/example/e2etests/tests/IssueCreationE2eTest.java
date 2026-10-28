package com.example.e2etests.tests;

import com.example.e2etests.data.TestDataFactory;
import com.example.e2etests.pages.IssuesPage;
import io.qameta.allure.Allure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueCreationE2eTest extends BaseUiTest {

    @Test
    @Tag("smoke")
    @Tag("e2e")
    @DisplayName("User can create an issue from the UI")
    void userCanCreateIssueFromTheUi() {
        String title = TestDataFactory.uniqueIssueTitle("e2e-issue");
        IssuesPage issuesPage = new IssuesPage(driver, config);
        registerIssueForCleanup(title);

        issuesPage.open();
        issuesPage.createIssue(title, "Issue created from the UI", "HIGH");
        long createdIssueId = issuesPage.findIssueIdByTitle(title);
        registerCreatedIssueId(title, createdIssueId);
        Allure.step("Verify created issue has a positive ID",
                () -> assertTrue(createdIssueId > 0, "Created issue should have a positive ID"));
        Allure.step("Verify created issue is visible",
                () -> assertTrue(issuesPage.containsIssueWithTitle(title),
                        "Created issue should be visible: " + title));
    }
}
