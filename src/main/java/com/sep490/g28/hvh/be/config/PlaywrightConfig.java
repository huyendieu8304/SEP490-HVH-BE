package com.sep490.g28.hvh.be.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaywrightConfig {
    @Bean(destroyMethod = "close")
    public Browser playwrightBrowser() {
        Playwright playwright = Playwright.create();
        return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

}
