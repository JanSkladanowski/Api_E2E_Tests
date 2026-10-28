package com.example.e2etests.driver;

import com.example.e2etests.config.BrowserType;
import com.example.e2etests.config.UiTestConfig;
import org.openqa.selenium.WebDriver;

public interface BrowserDriverProvider {
    BrowserType browser();

    WebDriver create(UiTestConfig config);
}
