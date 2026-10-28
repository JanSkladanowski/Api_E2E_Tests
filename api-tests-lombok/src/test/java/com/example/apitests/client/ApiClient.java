package com.example.apitests.client;

import com.example.apitests.config.ApiTestConfig;
import com.example.apitests.model.request.ChangeStatusRequest;
import com.example.apitests.model.request.CreateIssueRequest;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;

public class ApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClient.class);

    private final RequestSpecification requestSpecification;

    public ApiClient() {
        String baseUrl = ApiTestConfig.baseUrl();
        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                .addFilter(new AllureRestAssured()
                        .setRequestAttachmentName("HTTP request")
                        .setResponseAttachmentName("HTTP response"))
                .build();

        LOGGER.info("API client configured for {}", baseUrl);
    }

    public Response getHealth() {
        return execute("GET /api/health", () -> given()
                .spec(requestSpecification)
                .when()
                .get("/api/health"));
    }

    public Response createIssue(CreateIssueRequest request) {
        LOGGER.info("REQUEST DATA create issue: title='{}', priority={}", request.title(), request.priority());
        return execute("POST /api/v1/issues", () -> given()
                .spec(requestSpecification)
                .body(request)
                .when()
                .post("/api/v1/issues"));
    }

    public Response deleteIssue(long issueId) {
        return execute("DELETE /api/v1/issues/" + issueId, () -> given()
                .spec(requestSpecification)
                .when()
                .delete("/api/v1/issues/{id}", issueId));
    }

    public void deleteIssueForCleanup(long issueId) {
        Response response = deleteIssue(issueId);
        if (response.statusCode() != 204) {
            throw new IllegalStateException(
                    "Could not clean up issue " + issueId + "; received HTTP " + response.statusCode()
            );
        }
    }

    public Response getIssue(long issueId) {
        return execute("GET /api/v1/issues/" + issueId, () -> given()
                .spec(requestSpecification)
                .when()
                .get("/api/v1/issues/{id}", issueId));
    }

    public Response findIssues(String status, String priority) {
        String operation = "GET /api/v1/issues?status=" + valueOrAny(status)
                + "&priority=" + valueOrAny(priority);

        Response response = execute(operation, () -> {
            var request = given().spec(requestSpecification);
            if (status != null) {
                request.queryParam("status", status);
            }
            if (priority != null) {
                request.queryParam("priority", priority);
            }
            return request.when().get("/api/v1/issues");
        });

        if (response.statusCode() == 200) {
            List<?> returnedIssues = response.jsonPath().getList("$");
            LOGGER.info("HTTP RESPONSE SUMMARY operation='{}' returnedIssues={}", operation, returnedIssues.size());
        }

        return response;
    }

    public Response changeIssueStatus(long issueId, ChangeStatusRequest request) {
        LOGGER.info("REQUEST DATA change issue status: issueId={}, requestedStatus={}", issueId, request.status());
        return execute("PATCH /api/v1/issues/" + issueId + "/status", () -> given()
                .spec(requestSpecification)
                .body(request)
                .when()
                .patch("/api/v1/issues/{id}/status", issueId));
    }

    private Response execute(String operation, Supplier<Response> request) {
        long startedAt = System.nanoTime();
        LOGGER.info("HTTP REQUEST {}", operation);

        try {
            Response response = request.get();
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.info("HTTP RESPONSE {} status={} durationMs={} contentType={}",
                    operation,
                    response.statusCode(),
                    durationMillis,
                    response.contentType());

            return response;
        } catch (RuntimeException exception) {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.error("HTTP REQUEST FAILED {} durationMs={} exception={}",
                    operation, durationMillis, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private String valueOrAny(String value) {
        return value == null ? "<any>" : value;
    }
}
