package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;

public class DebugTruncatedReview {
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

            Locator reviewsTab = page.locator("button:has-text('Reviews')").first();
            if (reviewsTab.count() > 0) {
                reviewsTab.click();
                page.waitForTimeout(2000);
            }

            // Scroll until we find the target reviewer's card
            page.evaluate("""
                    () => {
                        const c = document.querySelector('div.jftiEf');
                        if (!c) return;
                        let el = c.parentElement;
                        while (el) {
                            const s = getComputedStyle(el);
                            if ((s.overflowY === 'auto' || s.overflowY === 'scroll') && el.scrollHeight > el.clientHeight) {
                                el.setAttribute('data-scroll', 'true');
                                return;
                            }
                            el = el.parentElement;
                        }
                    }
                    """);
            Locator scrollEl = page.locator("[data-scroll='true']").first();

            boolean found = false;
            for (int i = 0; i < 60; i++) {
                Locator target = page.locator("div.jftiEf:has-text('Mary Boteler')").first();
                if (target.count() > 0) {
                    found = true;
                    break;
                }
                scrollEl.evaluate("el => el.scrollTop = el.scrollHeight");
                page.waitForTimeout(400);
            }

            System.out.println("Found card: " + found);
            if (found) {
                Locator card = page.locator("div.jftiEf:has-text('Mary Boteler')").first();
                String html = (String) card.evaluate("el => el.outerHTML");
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("./output/truncated-card.html"), html);
                System.out.println("Card HTML written to output/truncated-card.html");
                System.out.println("More button count: " + card.locator("button:has-text('More')").count());
                System.out.println("wiI7pd text: " + card.locator("span.wiI7pd").innerText());
            }

            context.close();
        }
    }
}
