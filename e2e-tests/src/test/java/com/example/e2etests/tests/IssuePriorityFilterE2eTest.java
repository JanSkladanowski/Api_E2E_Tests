package com.example.e2etests.tests;

import com.example.e2etests.data.TestDataFactory;
import com.example.e2etests.pages.IssuesPage;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuePriorityFilterE2eTest extends BaseUiTest {

    @Test
    @Tag("e2e")
    @DisplayName("User can filter issues by priority")
    void userCanFilterIssuesByPriority() {
        String highTitle = TestDataFactory.uniqueIssueTitle("e2e-high");
        String lowTitle = TestDataFactory.uniqueIssueTitle("e2e-low");
        IssuesPage issuesPage = new IssuesPage(driver, config);
        registerIssueForCleanup(highTitle);
        registerIssueForCleanup(lowTitle);

        issuesPage.open();
        issuesPage.createIssue(highTitle, "High-priority issue for filter test", "HIGH");
        long highIssueId = issuesPage.findIssueIdByTitle(highTitle);
        registerCreatedIssueId(highTitle, highIssueId);
        Allure.step("Verify HIGH issue was created and is visible", () -> {
            assertTrue(highIssueId > 0, "HIGH issue should have a positive ID");
            assertTrue(issuesPage.containsIssueWithTitle(highTitle),
                    "HIGH issue should be visible: " + highTitle);
        });

        issuesPage.createIssue(lowTitle, "Low-priority issue for filter test", "LOW");
        long lowIssueId = issuesPage.findIssueIdByTitle(lowTitle);
        registerCreatedIssueId(lowTitle, lowIssueId);
        Allure.step("Verify both issues are visible before filtering", () -> {
            assertTrue(lowIssueId > 0, "LOW issue should have a positive ID");
            assertTrue(issuesPage.containsIssueWithTitle(lowTitle),
                    "LOW issue should be visible: " + lowTitle);
            assertTrue(issuesPage.containsIssueWithTitle(highTitle),
                    "HIGH issue should still be visible before filtering: " + highTitle);
        });

        issuesPage.filterByPriority("HIGH");
        Allure.step("Verify LOW issue is excluded by HIGH filter",
                () -> assertTrue(issuesPage.waitUntilIssueWithTitleIsAbsent(lowTitle),
                        "LOW issue should disappear after filtering by HIGH"));
        Allure.step("Verify HIGH issue remains visible after filtering",
                () -> assertTrue(issuesPage.waitUntilIssueWithTitleIsVisible(highTitle),
                        "HIGH issue should remain visible after filtering"));
        List<String> visiblePriorities = issuesPage.visibleIssuePriorities();
        Allure.step("Verify every returned issue has HIGH priority", () -> {
            assertFalse(visiblePriorities.isEmpty(), "The filtered list should not be empty");
            assertTrue(visiblePriorities.stream().allMatch("HIGH"::equals),
                    "Expected only HIGH issues, but visible priorities were " + visiblePriorities);
        });
    }
}
