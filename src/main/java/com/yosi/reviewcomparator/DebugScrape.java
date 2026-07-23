package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

public class DebugScrape {
    public static void main(String[] args) {
        Config config = new Config();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setDefaultTimeout(30000);
            page.navigate(config.googleMapsUrl());
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(3000);

            page.setViewportSize(1280, 900);

            // Scroll the left info panel (Maps' sidebar is an independently-scrolling div)
            Locator sidebar = page.locator("div[role='main']").first();
            System.out.println("Sidebar count: " + sidebar.count());
            for (int i = 0; i < 10; i++) {
                sidebar.hover();
                page.mouse().wheel(0, 800);
                page.waitForTimeout(800);
            }
            page.waitForTimeout(2000);

            Locator moreReviews = page.locator("text=/More reviews/i").first();
            System.out.println("More reviews link count: " + moreReviews.count());
            if (moreReviews.count() > 0) {
                try {
                    moreReviews.click();
                    page.waitForTimeout(3000);
                } catch (Exception e) {
                    System.out.println("Click failed: " + e.getMessage());
                }
            }

            System.out.println("URL: " + page.url());
            System.out.println("Title: " + page.title());
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Path.of("./output/debug.png")).setFullPage(true));

            String html = page.content();
            System.out.println("HTML length: " + html.length());
            java.nio.file.Files.writeString(java.nio.file.Path.of("./output/debug.html"), html);

            System.out.println("Contains 'Reviews': " + html.contains("Reviews"));
            System.out.println("Contains consent: " + html.toLowerCase().contains("consent"));
            System.out.println("Contains jftiEf: " + html.contains("jftiEf"));

            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
