package com.example.e2etests.tests;

import com.example.e2etests.pages.ApplicationPage;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationSmokeTest extends BaseUiTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSmokeTest.class);

    @Test
    @Tag("smoke")
    @Tag("e2e")
    @DisplayName("Application home page is available")
    void applicationHomePageShouldBeAvailable() {
        LOGGER.info("Opening application home page");
        ApplicationPage applicationPage = new ApplicationPage(driver, config);
        applicationPage.open();
        Allure.step("Verify application page title is IssueHub",
                () -> assertEquals("IssueHub", applicationPage.title()));
    }
}
