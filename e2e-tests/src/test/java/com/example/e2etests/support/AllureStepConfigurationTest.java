package com.example.e2etests.support;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AllureStepConfigurationTest {

    @Test
    @Tag("framework")
    void annotatedMethodIsRecordedAsASeparateStep() {
        String testId = Allure.getLifecycle().getCurrentTestCase().orElseThrow();

        String stepId = currentStepId("example", "description", "HIGH");

        Allure.step("Verify annotated method created a separate report step",
                () -> assertNotEquals(testId, stepId, "@Step should create a separate Allure step"));
    }

    @Step("Check annotated step for {0} with priority {2}")
    String currentStepId(String title, String description, String priority) {
        return Allure.getLifecycle().getCurrentTestCaseOrStep().orElseThrow();
    }
}
