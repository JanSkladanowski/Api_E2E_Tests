package com.example.e2etests.driver;

import com.example.e2etests.config.BrowserType;
import com.example.e2etests.config.UiTestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

final class FirefoxDriverProvider implements BrowserDriverProvider {
    @Override
    public BrowserType browser() {
        return BrowserType.FIREFOX;
    }

    @Override
    public WebDriver create(UiTestConfig config) {
        FirefoxOptions options = new FirefoxOptions();
        if (!config.browserBinary().isBlank()) {
            options.setBinary(config.browserBinary());
        }
        if (config.headless()) {
            options.addArguments("-headless");
        }
        return new FirefoxDriver(options);
    }
}
