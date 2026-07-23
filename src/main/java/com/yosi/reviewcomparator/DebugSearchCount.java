package com.yosi.reviewcomparator;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

public class DebugSearchCount {
    public static void main(String[] args) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(
                    new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setDefaultTimeout(30000);
            page.navigate("https://www.google.com/search?q=Test+Bussiness+reviews");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.waitForTimeout(2000);

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Path.of("./output/search-debug.png")).setFullPage(true));

            String html = page.content();
            java.nio.file.Files.writeString(java.nio.file.Path.of("./output/search-debug.html"), html);
            System.out.println("Saved. HTML length: " + html.length());

            browser.close();
        }
    }
}
