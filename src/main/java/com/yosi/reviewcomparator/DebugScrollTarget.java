package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;

public class DebugScrollTarget {
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

            // Find every element that actually has scrollable overflow and
            // contains at least one review card, so we scroll the real one.
            Object result = page.evaluate("""
                () => {
                    const cards = document.querySelectorAll('div.jftiEf');
                    if (cards.length === 0) return 'NO_CARDS_FOUND';
                    const card = cards[0];
                    const found = [];
                    let el = card.parentElement;
                    let depth = 0;
                    while (el && depth < 15) {
                        const style = getComputedStyle(el);
                        const scrollable = (style.overflowY === 'auto' || style.overflowY === 'scroll')
                            && el.scrollHeight > el.clientHeight;
                        found.push({
                            depth,
                            tag: el.tagName,
                            class: el.className,
                            overflowY: style.overflowY,
                            scrollHeight: el.scrollHeight,
                            clientHeight: el.clientHeight,
                            scrollable
                        });
                        el = el.parentElement;
                        depth++;
                    }
                    return JSON.stringify(found, null, 2);
                }
                """);

            System.out.println(result);
            context.close();
        }
    }
}
