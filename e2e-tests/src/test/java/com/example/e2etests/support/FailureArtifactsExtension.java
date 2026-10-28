package com.example.e2etests.support;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

public final class FailureArtifactsExtension implements TestExecutionExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailureArtifactsExtension.class);
    private static final Path ARTIFACT_DIRECTORY = Path.of("target", "e2e-failures");

    private final Supplier<WebDriver> driverSupplier;

    public FailureArtifactsExtension(Supplier<WebDriver> driverSupplier) {
        this.driverSupplier = driverSupplier;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable {
        try {
            captureArtifacts(context);
        } catch (RuntimeException artifactFailure) {
            LOGGER.warn("Could not capture E2E failure artifacts", artifactFailure);
        }
        throw failure;
    }

    private void captureArtifacts(ExtensionContext context) {
        WebDriver driver = driverSupplier.get();
        if (driver == null) {
            return;
        }

        String name = context.getRequiredTestMethod().getName() + "-" + UUID.randomUUID();
        try {
            Files.createDirectories(ARTIFACT_DIRECTORY);
        } catch (IOException exception) {
            LOGGER.warn("Could not create E2E artifact directory {}", ARTIFACT_DIRECTORY, exception);
        }

        if (driver instanceof TakesScreenshot screenshotDriver) {
            Path screenshot = ARTIFACT_DIRECTORY.resolve(name + ".png");
            try {
                byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                attach("Screenshot on failure", "image/png", screenshotBytes, ".png");
                save(screenshot, screenshotBytes);
            } catch (RuntimeException exception) {
                LOGGER.warn("Could not capture E2E screenshot", exception);
            }
        }

        Path pageSource = ARTIFACT_DIRECTORY.resolve(name + ".html");
        try {
            byte[] htmlBytes = driver.getPageSource().getBytes(StandardCharsets.UTF_8);
            attach("Page source on failure", "text/html", htmlBytes, ".html");
            save(pageSource, htmlBytes);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not capture E2E page source", exception);
        }
    }

    private static void attach(String name, String type, byte[] bytes, String extension) {
        try {
            Allure.addAttachment(name, type, new ByteArrayInputStream(bytes), extension);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not attach {} to Allure", name, exception);
        }
    }

    private static void save(Path path, byte[] bytes) {
        try {
            Files.write(path, bytes);
            LOGGER.error("E2E FAILURE artifact={}", path.toAbsolutePath());
        } catch (IOException exception) {
            LOGGER.warn("Could not save E2E artifact {}", path, exception);
        }
    }
}
