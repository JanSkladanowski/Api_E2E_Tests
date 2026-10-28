package com.example.e2etests.driver;

import com.example.e2etests.config.BrowserType;
import com.example.e2etests.config.UiTestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

final class EdgeDriverProvider implements BrowserDriverProvider {
    @Override
    public BrowserType browser() {
        return BrowserType.EDGE;
    }

    @Override
    public WebDriver create(UiTestConfig config) {
        EdgeOptions options = new EdgeOptions();
        if (!config.browserBinary().isBlank()) {
            options.setBinary(config.browserBinary());
        }
        if (config.headless()) {
            options.addArguments("--headless=new");
        }
        return new EdgeDriver(options);
    }
}
