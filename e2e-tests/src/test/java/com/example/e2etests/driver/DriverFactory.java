package com.example.e2etests.driver;

import com.example.e2etests.config.UiTestConfig;
import com.example.e2etests.config.BrowserType;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

import java.util.List;

public final class DriverFactory {

    private static final List<BrowserDriverProvider> PROVIDERS = List.of(
            new ChromeDriverProvider(),
            new FirefoxDriverProvider(),
            new EdgeDriverProvider()
    );

    private DriverFactory() {
    }

    public static WebDriver create(UiTestConfig config) {
        BrowserDriverProvider provider = providerFor(config.browser());

        WebDriver driver = provider.create(config);
        try {
            driver.manage().window().setSize(new Dimension(config.windowWidth(), config.windowHeight()));
            return driver;
        } catch (RuntimeException exception) {
            try {
                driver.quit();
            } catch (RuntimeException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    static BrowserDriverProvider providerFor(BrowserType browser) {
        BrowserDriverProvider provider = PROVIDERS.stream()
                .filter(candidate -> candidate.browser() == browser)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No driver provider for browser " + browser));
        return provider;
    }
}
