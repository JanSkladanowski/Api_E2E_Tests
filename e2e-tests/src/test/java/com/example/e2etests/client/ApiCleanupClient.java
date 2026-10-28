package com.example.e2etests.client;

import com.example.e2etests.config.UiTestConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.OptionalLong;

public final class ApiCleanupClient implements IssueCleanupClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCleanupClient.class);

    private final String apiBaseUrl;
    private final UiTestConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiCleanupClient(UiTestConfig config) {
        this.config = config;
        this.apiBaseUrl = config.apiBaseUrl();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.cleanupTimeout())
                .build();
    }

    @Override
    public OptionalLong findIssueIdByExactTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Cleanup title must not be blank");
        }
        String url = apiBaseUrl + "/api/v1/issues/by-title?title="
                + URLEncoder.encode(title, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(config.cleanupTimeout())
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            LOGGER.info("CLEANUP LOOKUP no issue found title='{}'", title);
            return OptionalLong.empty();
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Cleanup GET " + url
                    + " returned HTTP " + response.statusCode());
        }

        try {
            JsonNode issue = objectMapper.readTree(response.body());
            if (issue == null || !title.equals(issue.path("title").asText())) {
                throw new IllegalStateException("Cleanup lookup returned an issue with a different title: " + title);
            }
            JsonNode id = issue.path("id");
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() <= 0) {
                throw new IllegalStateException("Cleanup issue '" + title + "' has no valid ID");
            }
            LOGGER.info("CLEANUP LOOKUP matched title='{}' id={}", title, id.longValue());
            return OptionalLong.of(id.longValue());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not parse cleanup GET " + url, exception);
        }
    }

    @Override
    public void deleteIssue(long issueId) {
        String url = apiBaseUrl + "/api/v1/issues/" + issueId;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(config.cleanupTimeout())
                .DELETE()
                .build();
        HttpResponse<String> response = send(request);
            if (response.statusCode() != 204) {
                throw new IllegalStateException("Cleanup DELETE " + url
                    + " returned HTTP " + response.statusCode());
            }
    }

    private HttpResponse<String> send(HttpRequest request) {
        LOGGER.info("CLEANUP REQUEST method={} uri={}", request.method(), request.uri());
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.info("CLEANUP RESPONSE method={} uri={} status={}",
                    request.method(), request.uri(), response.statusCode());
            return response;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cleanup request was interrupted: " + request.uri(), exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not send cleanup request: " + request.uri(), exception);
        }
    }
}
