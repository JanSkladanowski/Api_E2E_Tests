package com.example.e2etests.client;

import com.example.e2etests.config.BrowserType;
import com.example.e2etests.config.UiTestConfig;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("framework")
class ApiCleanupClientTest {

    @Test
    void looksUpEncodedExactTitleWithoutRequestingTheIssueList() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        AtomicReference<String> requestedQuery = new AtomicReference<>();
        server.createContext("/api/v1/issues/by-title", exchange -> {
            requestedQuery.set(exchange.getRequestURI().getRawQuery());
            if (!"title=e2e+issue+%26+one".equals(requestedQuery.get())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] body = "{\"id\":73,\"title\":\"e2e issue & one\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            ApiCleanupClient client = clientFor(server);
            assertEquals(73L, client.findIssueIdByExactTitle("e2e issue & one").orElseThrow());
            assertEquals("title=e2e+issue+%26+one", requestedQuery.get());
            assertTrue(client.findIssueIdByExactTitle("missing").isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsAmbiguousLookupInsteadOfDeletingAnyIssue() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/api/v1/issues/by-title", exchange -> {
            exchange.sendResponseHeaders(409, -1);
            exchange.close();
        });
        server.start();
        try {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> clientFor(server).findIssueIdByExactTitle("duplicate title"));
            assertTrue(failure.getMessage().contains("HTTP 409"));
        } finally {
            server.stop(0);
        }
    }

    private ApiCleanupClient clientFor(HttpServer server) {
        UiTestConfig config = new UiTestConfig(
                "http://localhost:5173",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                BrowserType.CHROME,
                true,
                "",
                1440,
                900,
                Duration.ofSeconds(3),
                Duration.ofSeconds(3)
        );
        return new ApiCleanupClient(config);
    }
}
