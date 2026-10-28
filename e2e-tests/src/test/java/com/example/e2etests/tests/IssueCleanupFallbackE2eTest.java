package com.example.e2etests.tests;

import com.example.e2etests.client.ApiCleanupClient;
import com.example.e2etests.data.TestDataFactory;
import com.example.e2etests.pages.IssuesPage;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueCleanupFallbackE2eTest extends BaseUiTest {

    @Test
    @Tag("e2e")
    @DisplayName("Fallback cleanup removes a UI-created issue when its ID was not registered")
    void removesIssueByExactTitleWhenRegistryHasNoId() {
        String title = TestDataFactory.uniqueIssueTitle("e2e-fallback");
        IssuesPage issuesPage = new IssuesPage(driver, config);
        ApiCleanupClient apiClient = new ApiCleanupClient(config);
        registerIssueForCleanup(title);

        issuesPage.open();
        issuesPage.createIssue(title, "Issue for fallback cleanup test", "HIGH");
        long issueId = issuesPage.findIssueIdByTitle(title);
        Allure.step("Verify the UI-created issue exists in the API", () ->
                assertEquals(issueId, apiClient.findIssueIdByExactTitle(title).orElseThrow()));

        // The ID is intentionally not registered: the cleanup registry only knows the title.
        Allure.step("Run fallback cleanup without a registered issue ID", this::cleanUpRegisteredResources);

        Allure.step("Verify fallback cleanup removed the issue", () ->
                assertTrue(apiClient.findIssueIdByExactTitle(title).isEmpty(),
                        "Issue should be absent after fallback cleanup: " + title));
    }
}
