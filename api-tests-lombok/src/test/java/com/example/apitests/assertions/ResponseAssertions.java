package com.example.apitests.assertions;

import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ResponseAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseAssertions.class);

    private ResponseAssertions() {
    }

    public static void statusIs(Response response, int expectedStatus) {
        LOGGER.info("ASSERT HTTP status expected={} actual={}", expectedStatus, response.statusCode());
        assertEquals(expectedStatus, response.statusCode(),
                () -> "Unexpected HTTP response: expected status " + expectedStatus
                        + " but received " + response.statusLine());
    }

    public static void fieldErrorContains(Response response, String field, String expectedMessage) {
        String actualMessage = response.jsonPath().getString("fieldErrors." + field);
        LOGGER.info("ASSERT validation field={} expectedContains='{}' actual='{}'",
                field, expectedMessage, actualMessage);

        assertTrue(
                actualMessage != null && actualMessage.contains(expectedMessage),
                () -> "Expected validation message for field '" + field + "' to contain '"
                        + expectedMessage + "', but was '" + actualMessage + "'"
        );
    }

    public static void fieldEquals(Response response, String field, String expectedValue) {
        String actualValue = response.jsonPath().getString(field);
        LOGGER.info("ASSERT response field={} expected='{}' actual='{}'", field, expectedValue, actualValue);
        assertEquals(expectedValue, actualValue,
                () -> "Unexpected response field '" + field + "' for HTTP " + response.statusLine());
    }

    public static void issueWithIdHasValue(
            Response response,
            long expectedIssueId,
            String field,
            String expectedValue
    ) {
        List<Map<String, Object>> issues = response.jsonPath().getList("$");
        Map<String, Object> matchingIssue = issues.stream()
                .filter(issue -> ((Number) issue.get("id")).longValue() == expectedIssueId)
                .findFirst()
                .orElse(null);

        String actualValue = matchingIssue == null || matchingIssue.get(field) == null
                ? null
                : matchingIssue.get(field).toString();
        LOGGER.info("ASSERT filtered issue id={} field={} expected='{}' actual='{}'",
                expectedIssueId, field, expectedValue, actualValue);

        assertTrue(
                matchingIssue != null,
                () -> "Expected response to contain issue with id " + expectedIssueId
        );
        assertEquals(expectedValue, actualValue,
                () -> "Issue " + expectedIssueId + " had unexpected field '" + field + "'");
    }

    public static void issueWithIdIsAbsent(
            Response response,
            long issueIdExpectedToBeAbsent
    ) {
        List<Map<String, Object>> issues = response.jsonPath().getList("$");
        boolean issueIsAbsent = issues.stream()
                .noneMatch(issue -> ((Number) issue.get("id")).longValue() == issueIdExpectedToBeAbsent);

        LOGGER.info("ASSERT filtered issue id={} not match='{}'",
                issueIdExpectedToBeAbsent, issueIsAbsent);

        assertTrue(issueIsAbsent, () -> "Expected issue with ID " + issueIdExpectedToBeAbsent
                + " to be absent from the filtered response, but it was present");
    }

    public static void allReturnedIssuesHavePriority(
            Response response,
            String expectedPriority) {
        List<Map<String, Object>> issues = response.jsonPath().getList("$");
        boolean allMatch = issues.stream()
                .allMatch(issue -> expectedPriority.equals(issue.get("priority")));

        LOGGER.info("ASSERT all filtered issue priorities expected={} allMatch={}",
                expectedPriority, allMatch);
        assertTrue(allMatch,
                () -> "Expected all returned issues to have priority '"
                    + expectedPriority + "', but at least one did not");
    }
}
