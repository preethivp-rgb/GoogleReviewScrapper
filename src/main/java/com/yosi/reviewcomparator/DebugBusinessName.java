package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;

public class DebugBusinessName {
    public static void main(String[] args) throws Exception {
        Config config = new Config();

        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = playwright.chromium().launchPersistentContext(
                    java.nio.file.Path.of(config.chromeProfileDir()),
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel("chrome")
                            .setHeadless(false)
                            .setArgs(java.util.List.of("--start-maximized")));

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            page.setDefaultTimeout(30000);
            page.navigate(config.googleMapsUrl());
            page.waitForTimeout(3000);

            System.out.println("Page title: " + page.title());
            System.out.println("URL: " + page.url());

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Path.of("./output/business-name-check.png")));

            context.close();
        }
    }
}
