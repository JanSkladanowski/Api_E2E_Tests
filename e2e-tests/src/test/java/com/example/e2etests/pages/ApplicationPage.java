package com.example.e2etests.pages;

import com.example.e2etests.config.UiTestConfig;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ApplicationPage {

    private static final By APPLICATION_TITLE = By.cssSelector("[data-testid='application-title']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final UiTestConfig config;

    public ApplicationPage(WebDriver driver, UiTestConfig config) {
        this.driver = driver;
        this.config = config;
        this.wait = new WebDriverWait(driver, config.waitTimeout());
    }

    @Step("Open application home page")
    public ApplicationPage open() {
        driver.get(config.uiBaseUrl());
        wait.until(ExpectedConditions.visibilityOfElementLocated(APPLICATION_TITLE));
        return this;
    }

    @Step("Read application page title")
    public String title() {
        return driver.getTitle();
    }
}
