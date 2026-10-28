package com.example.e2etests.pages;

import com.example.e2etests.config.UiTestConfig;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IssuesPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssuesPage.class);

    private static final By PAGE_TITLE = By.cssSelector("[data-testid='issues-page-title']");
    private static final By TITLE_INPUT = By.id("issue-title");
    private static final By DESCRIPTION_INPUT = By.id("issue-description");
    private static final By PRIORITY_SELECT = By.id("issue-priority");
    private static final By SUBMIT_BUTTON = By.cssSelector("[data-testid='create-issue-submit']");
    private static final By ISSUE_ROWS = By.cssSelector("[data-testid='issue-row']");
    private static final By PRIORITY_FILTER = By.cssSelector("[data-testid='priority-filter']");

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final UiTestConfig config;

    public IssuesPage(WebDriver driver, UiTestConfig config) {
        this.driver = driver;
        this.config = config;
        this.wait = new WebDriverWait(driver, config.waitTimeout());
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    @Step("Open issues page")
    public IssuesPage open() {
        String url = config.uiBaseUrl() + "/issues.html";
        LOGGER.info("UI ACTION open issues page url={}", url);
        driver.get(url);
        wait.until(ExpectedConditions.visibilityOfElementLocated(PAGE_TITLE));
        wait.until(ExpectedConditions.visibilityOfElementLocated(TITLE_INPUT));
        LOGGER.info("UI PAGE ready title=Issues");
        return this;
    }

    @Step("Create issue '{0}' with decription '{1}' and priority '{2}'")
    public IssuesPage createIssue(String title, String description, String priority) {
        LOGGER.info("UI ACTION create issue title='{}' priority={}", title, priority);
        driver.findElement(TITLE_INPUT).sendKeys(title);
        driver.findElement(DESCRIPTION_INPUT).sendKeys(description);

        new Select(driver.findElement(PRIORITY_SELECT)).selectByValue(priority);

        driver.findElement(SUBMIT_BUTTON).click();
        wait.until(currentDriver -> containsIssueWithTitle(title));
        LOGGER.info("UI RESULT issue visible after creation title='{}'", title);
        return this;
    }

    @Step("Find ID of issue '{0}'")
    public long findIssueIdByTitle(String title) {
        long issueId = wait.withMessage("Issue with title '" + title + "' was not found")
                .until(currentDriver -> {
                    for (WebElement row : currentDriver.findElements(ISSUE_ROWS)) {
                        String rowTitle = row.findElement(
                                By.cssSelector("[data-testid='issue-title-cell']")).getText();
                        if (title.equals(rowTitle)) {
                            return Long.parseLong(row.getDomAttribute("data-issue-id"));
                        }
                    }
                    return null;
                });
        LOGGER.info("UI RESULT issue found title='{}' id={}", title, issueId);
        return issueId;
    }

    @Step("Filter issues by priority '{0}'")
    public IssuesPage filterByPriority(String priority) {
        LOGGER.info("UI ACTION filter issues priority={}", priority);
        new Select(driver.findElement(PRIORITY_FILTER)).selectByValue(priority);
        return this;
    }

    public boolean containsIssueWithTitle(String title) {
        return driver.findElements(ISSUE_ROWS).stream()
                .anyMatch(row -> title.equals(row.findElement(
                        By.cssSelector("[data-testid='issue-title-cell']")).getText()));
    }

    @Step("Read priorities of visible issues")
    public List<String> visibleIssuePriorities() {
        return driver.findElements(ISSUE_ROWS).stream()
                .map(row -> row.findElement(
                        By.cssSelector("[data-testid='issue-priority-cell']")).getText())
                .toList();
    }

    @Step("Wait for issue '{0}' to be visible")
    public boolean waitUntilIssueWithTitleIsVisible(String title) {
        return wait.until(driver -> containsIssueWithTitle(title));
    }

    @Step("Wait for issue '{0}' to be absent")
    public boolean waitUntilIssueWithTitleIsAbsent(String title) {
        return wait.until(driver -> !containsIssueWithTitle(title));
    }
}
