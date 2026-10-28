package com.example.e2etests.driver;

import com.example.e2etests.config.BrowserType;
import com.example.e2etests.config.UiTestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

final class ChromeDriverProvider implements BrowserDriverProvider {
    @Override
    public BrowserType browser() {
        return BrowserType.CHROME;
    }

    @Override
    public WebDriver create(UiTestConfig config) {
        ChromeOptions options = new ChromeOptions();
        if (!config.browserBinary().isBlank()) {
            options.setBinary(config.browserBinary());
        }
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        return new ChromeDriver(options);
    }
}
