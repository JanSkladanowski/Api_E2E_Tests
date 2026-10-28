package com.example.apitests.tests;

import com.example.apitests.assertions.ResponseAssertions;
import com.example.apitests.client.ApiClient;
import com.example.apitests.data.TestDataFactory;
import com.example.apitests.model.request.ChangeStatusRequest;
import com.example.apitests.model.request.CreateIssueRequest;
import com.example.apitests.support.TestResourceRegistry;
import com.example.apitests.support.TestResourceRegistry.CleanupHandle;

import io.restassured.response.Response;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Epic("IssueHub API")
@DisplayName("IssueHub API tests")
class IssueApiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssueApiTest.class);
    private static final long UNKNOWN_ISSUE_ID = -1L;

    private ApiClient apiClient;
    private final TestResourceRegistry resources = new TestResourceRegistry();
    private final Map<Long, CleanupHandle> issueCleanupHandles = new HashMap<>();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        MDC.put("testName", testInfo.getDisplayName());
        apiClient = new ApiClient();
        LOGGER.info("TEST START");
    }

    @AfterEach
    void cleanUp() {
        try {
            resources.cleanup();
        } finally {
            LOGGER.info("TEST TEARDOWN FINISHED");
            MDC.remove("testName");
        }
    }

    @Test
    @Feature("Health")
    @Story("Check service health")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Application health endpoint returns UP")
    @Description("Verifies that the health endpoint is available and reports the application as UP.")
    void applicationShouldBeHealthy() {
        Response response = apiClient.getHealth();
        ResponseAssertions.statusIs(response, 200);
        ResponseAssertions.fieldEquals(response, "status", "UP");
    }

    @Test
    @Feature("Issue management")
    @Story("Create an issue")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Create an issue with the requested title")
    @Description("Creates an issue with valid data and verifies its ID and title in the response.")
    void shouldCreateIssueWithRequestedTitle() {
        String title = TestDataFactory.uniqueIssueTitle();
        Response response = createIssue(title, "HIGH");

        ResponseAssertions.statusIs(response, 201);
        assertNotNull(response.jsonPath().getLong("id"));
        ResponseAssertions.fieldEquals(response, "title", title);
    }

    @Test
    @Feature("Issue management")
    @Story("Retrieve an issue")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Retrieve an issue created by this test")
    @Description("Creates an issue, retrieves it by its returned ID, and verifies the title.")
    void shouldRetrieveCreatedIssue() {
        String title = TestDataFactory.uniqueIssueTitle();
        Response created = createIssue(title, "HIGH");
        long issueId = created.jsonPath().getLong("id");
        Response retrieved = apiClient.getIssue(issueId);

        ResponseAssertions.statusIs(retrieved, 200);
        ResponseAssertions.fieldEquals(retrieved, "title", title);
    }

    @Test
    @Feature("Issue management")
    @Story("Delete an issue")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Delete an issue created by this test")
    @Description("Deletes a created issue, verifies HTTP 204 and confirms that a later GET returns NOT_FOUND.")
    void shouldDeleteCreatedIssue() {
        Response created = createIssue(TestDataFactory.uniqueIssueTitle(), "HIGH");
        ResponseAssertions.statusIs(created, 201);
        long issueId = created.jsonPath().getLong("id");

        Response deleted = apiClient.deleteIssue(issueId);
        Response afterDelete = apiClient.getIssue(issueId);
        if (afterDelete.statusCode() == 404) {
            markIssueDeleted(issueId);
        }

        ResponseAssertions.statusIs(deleted, 204);
        ResponseAssertions.statusIs(afterDelete, 404);
        ResponseAssertions.fieldEquals(afterDelete, "code", "NOT_FOUND");
    }

    @Test
    @Feature("Issue management")
    @Story("Change issue status")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Change a created issue status to IN_PROGRESS")
    @Description("Creates an issue, changes its status, and verifies the response status.")
    void shouldChangeCreatedIssueStatus() {
        Response created = createIssue(TestDataFactory.uniqueIssueTitle(), "HIGH");
        long issueId = created.jsonPath().getLong("id");
        Response changed = apiClient.changeIssueStatus(issueId, new ChangeStatusRequest("IN_PROGRESS"));

        ResponseAssertions.statusIs(changed, 200);
        ResponseAssertions.fieldEquals(changed, "status", "IN_PROGRESS");
    }

    @Test
    @Feature("Issue validation")
    @Story("Require a title")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Reject issue creation when title is missing")
    @Description("Sends a request without a title and verifies the validation response.")
    void shouldRejectIssueWithoutTitle() {
        CreateIssueRequest invalidRequest = CreateIssueRequest.builder()
                .description("Issue with invalid data")
                .priority("HIGH")
                .build();
        Response response = apiClient.createIssue(invalidRequest);

        ResponseAssertions.statusIs(response, 400);
        ResponseAssertions.fieldErrorContains(response, "title", "Title is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESOLVED", "CLOSED"})
    @Feature("Issue status rules")
    @Story("Reject invalid status transitions")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Reject an invalid status transition: {0}")
    @Description("Verifies that transitions not allowed from OPEN return HTTP 409 and the business-rule error code.")
    void shouldRejectInvalidStatusTransition(String invalidStatus) {
        Response created = createIssue(TestDataFactory.uniqueIssueTitle(), "HIGH");
        long issueId = created.jsonPath().getLong("id");
        Response response = apiClient.changeIssueStatus(issueId, new ChangeStatusRequest(invalidStatus));

        ResponseAssertions.statusIs(response, 409);
        ResponseAssertions.fieldEquals(response, "code", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    @Feature("Issue management")
    @Story("Handle unknown issue")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Return NOT_FOUND for an unknown issue ID")
    @Description("Requests an ID that does not exist and verifies the not-found response.")
    void shouldReturnNotFoundForUnknownIssue() {
        Response response = apiClient.getIssue(UNKNOWN_ISSUE_ID);
        ResponseAssertions.statusIs(response, 404);
        ResponseAssertions.fieldEquals(response, "code", "NOT_FOUND");
    }

    @Test
    @Feature("Issue filtering")
    @Story("Filter by priority")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Return the HIGH issue and exclude the LOW issue")
    @Description("Creates HIGH and LOW issues, filters by HIGH, and verifies inclusion, exclusion, and priority of every result.")
    void shouldFilterIssuesByPriorityWithoutReturningOtherCreatedIssue() {
        Response highPriorityIssue = createIssue(TestDataFactory.uniqueIssueTitle(), "HIGH");
        long highPriorityIssueId = highPriorityIssue.jsonPath().getLong("id");
        Response lowPriorityIssue = createIssue(TestDataFactory.uniqueIssueTitle(), "LOW");
        long lowPriorityIssueId = lowPriorityIssue.jsonPath().getLong("id");
        Response filtered = apiClient.findIssues(null, "HIGH");

        ResponseAssertions.statusIs(filtered, 200);
        ResponseAssertions.issueWithIdHasValue(filtered, highPriorityIssueId, "priority", "HIGH");
        ResponseAssertions.issueWithIdIsAbsent(filtered, lowPriorityIssueId);
        ResponseAssertions.allReturnedIssuesHavePriority(filtered, "HIGH");
    }

    @Test
    @Feature("Issue filtering")
    @Story("Filter by status")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Return a created issue when filtering by IN_PROGRESS")
    @Description("Changes a created issue to IN_PROGRESS, filters by that status, and verifies the issue is returned.")
    void shouldFilterIssuesByStatus() {
        Response created = createIssue(TestDataFactory.uniqueIssueTitle(), "HIGH");
        long issueId = created.jsonPath().getLong("id");
        ResponseAssertions.statusIs(
                apiClient.changeIssueStatus(issueId, new ChangeStatusRequest("IN_PROGRESS")), 200);
        Response filtered = apiClient.findIssues("IN_PROGRESS", null);

        ResponseAssertions.statusIs(filtered, 200);
        ResponseAssertions.issueWithIdHasValue(filtered, issueId, "status", "IN_PROGRESS");
    }

    private Response createIssue(String title, String priority) {
        LOGGER.info("TEST DATA requested: title='{}' priority={}", title, priority);
        Response response = apiClient.createIssue(TestDataFactory.validIssueRequest(title, priority));
        if (response.statusCode() == 201) {
            long issueId = response.jsonPath().getLong("id");
            CleanupHandle handle = resources.register(
                    "issue id=" + issueId, () -> apiClient.deleteIssueForCleanup(issueId));
            issueCleanupHandles.put(issueId, handle);
        }
        return response;
    }

    private void markIssueDeleted(long issueId) {
        CleanupHandle handle = issueCleanupHandles.remove(issueId);
        if (handle == null) {
            throw new IllegalStateException("Issue was not registered for cleanup: " + issueId);
        }
        handle.markCompleted();
    }
}
