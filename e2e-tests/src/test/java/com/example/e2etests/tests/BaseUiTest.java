package com.example.e2etests.tests;

import io.qameta.allure.Allure;
import com.example.e2etests.client.ApiCleanupClient;
import com.example.e2etests.config.UiTestConfig;
import com.example.e2etests.data.IssueCleanupRegistry;
import com.example.e2etests.driver.DriverFactory;
import com.example.e2etests.support.FailureArtifactsExtension;
import com.example.e2etests.support.TestResourceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class BaseUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseUiTest.class);

    protected WebDriver driver;
    protected UiTestConfig config;
    private TestResourceRegistry resources;
    private IssueCleanupRegistry issueCleanupRegistry;

    @RegisterExtension
    protected final FailureArtifactsExtension failureArtifacts = new FailureArtifactsExtension(() -> driver);

    @BeforeEach
    void startBrowser(TestInfo testInfo) {
        String testName = testInfo.getTestMethod()
                .map(method -> method.getName())
                .orElse(testInfo.getDisplayName());
        MDC.put("testName", testName);

        config = UiTestConfig.load();
        Allure.parameter("browser", config.browser().name());
        Allure.parameter("headless", config.headless());
        Allure.parameter("uiBaseUrl", config.uiBaseUrl());
        resources = new TestResourceRegistry();
        issueCleanupRegistry = new IssueCleanupRegistry(resources, new ApiCleanupClient(config));
        LOGGER.info("TEST START uiBaseUrl={} apiBaseUrl={}", config.uiBaseUrl(), config.apiBaseUrl());
        LOGGER.info("BROWSER START browser={} headless={}", config.browser(), config.headless());
        driver = DriverFactory.create(config);
    }

    protected void registerIssueForCleanup(String title) {
        issueCleanupRegistry.registerTitle(title);
    }

    protected void registerCreatedIssueId(String title, long issueId) {
        issueCleanupRegistry.registerCreatedIssueId(title, issueId);
    }

    protected void registerResourceForCleanup(String description, Runnable cleanup) {
        resources.register(description, cleanup);
    }

    protected void cleanUpRegisteredResources() {
        resources.cleanup();
    }

    @AfterEach
    void tearDown() {
        RuntimeException teardownFailure = null;
        try {
            if (driver != null) {
                LOGGER.info("BROWSER STOP REQUEST browser={}", config.browser());
                try {
                    driver.quit();
                    LOGGER.info("BROWSER STOPPED browser={}", config.browser());
                } catch (RuntimeException exception) {
                    LOGGER.error("BROWSER STOP FAILED browser={}", config.browser(), exception);
                    teardownFailure = exception;
                }
            }

            if (resources != null) {
                try {
                    cleanUpRegisteredResources();
                } catch (RuntimeException exception) {
                    if (teardownFailure == null) {
                        teardownFailure = exception;
                    } else {
                        teardownFailure.addSuppressed(exception);
                    }
                }
            }
        } finally {
            LOGGER.info("TEST TEARDOWN FINISHED");
            MDC.remove("testName");
        }
        if (teardownFailure != null) {
            throw teardownFailure;
        }
    }
}
