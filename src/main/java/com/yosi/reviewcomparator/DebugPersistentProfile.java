package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;

public class DebugPersistentProfile {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        String userDataDir = "C:\\Users\\Preethi\\AppData\\Local\\Google\\ChromeAutomationProfile";

        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = playwright.chromium().launchPersistentContext(
                    java.nio.file.Path.of(userDataDir),
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel("chrome")
                            .setHeadless(false));

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            page.setDefaultTimeout(15000);

            try {
                page.navigate(config.googleMapsUrl());
            } catch (Exception e) {
                System.out.println("Navigate exception (continuing anyway): " + e.getMessage());
            }

            page.waitForTimeout(5000);

            try {
                System.out.println("Title: " + page.title());
                System.out.println("URL: " + page.url());
            } catch (Exception e) {
                System.out.println("Title/URL read failed: " + e.getMessage());
            }

            try {
                Locator reviewsTab = page.locator("button:has-text('Reviews')").first();
                if (reviewsTab.count() > 0) {
                    reviewsTab.click();
                    page.waitForTimeout(3000);

                    Locator sidebar = page.locator("div[role='main']").first();
                    for (int i = 0; i < 15; i++) {
                        sidebar.hover();
                        page.mouse().wheel(0, 1000);
                        page.waitForTimeout(700);
                    }
                    page.waitForTimeout(1500);
                }
            } catch (Exception e) {
                System.out.println("Reviews tab click/scroll failed: " + e.getMessage());
            }

            try {
                page.screenshot(new Page.ScreenshotOptions()
                        .setPath(java.nio.file.Path.of("./output/persistent-debug.png")).setFullPage(true));
                System.out.println("Screenshot saved.");
            } catch (Exception e) {
                System.out.println("Screenshot failed: " + e.getMessage());
            }

            try {
                String html = page.content();
                System.out.println("Contains 'Reviews': " + html.contains("Reviews"));
                System.out.println("Contains jftiEf: " + html.contains("jftiEf"));
                java.nio.file.Files.writeString(java.nio.file.Path.of("./output/persistent-debug.html"), html);
            } catch (Exception e) {
                System.out.println("HTML capture failed: " + e.getMessage());
            }

            context.close();
        }
    }
}
