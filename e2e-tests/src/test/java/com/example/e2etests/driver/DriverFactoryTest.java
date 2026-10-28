package com.example.e2etests.driver;

import com.example.e2etests.config.BrowserType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("framework")
class DriverFactoryTest {

    @ParameterizedTest
    @EnumSource(BrowserType.class)
    void hasProviderForEveryConfiguredBrowser(BrowserType browser) {
        assertEquals(browser, DriverFactory.providerFor(browser).browser());
    }
}
