package com.yosi.reviewcomparator.scraper;

import com.microsoft.playwright.*;
import com.yosi.reviewcomparator.Config;
import com.yosi.reviewcomparator.model.GoogleReview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scrapes the Google Maps place page using a persistent Chrome profile (a
 * copy of the real local Chrome profile, see CHROME_PROFILE_DIR).
 *
 * A fresh/anonymous browser context gets served Google's stripped-down
 * "limited view" (no reviews, only the aggregate rating) — this was
 * confirmed repeatedly. Launching against a real Chrome profile avoids that
 * and reliably renders the full reviews list.
 */
public class GoogleReviewScraper {

    private final Config config;
    private static final Pattern STARS_PATTERN = Pattern.compile("([0-9.]+) star");
    private static final Pattern HISTOGRAM_PATTERN = Pattern.compile("(\\d+) stars?, (\\d+) reviews?");
    private static final Pattern AGGREGATE_RATING_PATTERN = Pattern.compile("\\b([0-9]\\.[0-9])\\b");
    // Handles both layouts Google uses: "4.1 (11)" and "1,161 reviews" —
    // the comma-formatted count only shows up on the Reviews tab, not Overview.
    private static final Pattern AGGREGATE_COUNT_PAREN_PATTERN = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern AGGREGATE_COUNT_TEXT_PATTERN =
            Pattern.compile("([0-9][0-9,]*)\\s*[Rr]eviews?\\b");

    public GoogleReviewScraper(Config config) {
        this.config = config;
    }

    public static class AggregateResult {
        public Double rating;
        public Integer totalReviewCount;
    }

    public static class ScrapeResult {
        public AggregateResult aggregate;
        public List<GoogleReview> reviews;
    }

    /**
     * Single browser session that fetches both the aggregate stats and the
     * full review list. Scrolling stops as soon as the loaded card count
     * reaches the known aggregate total, instead of scrolling blind.
     */
    public ScrapeResult scrape() {
        ScrapeResult result = new ScrapeResult();

        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = openContext(playwright);
            Page page = openMapsPage(context);

            clickReviewsTab(page);
            clickSortBy(page, config.sortBy());
            result.aggregate = readAggregate(page);
            scrollReviewsPanel(page, result.aggregate.totalReviewCount);

            result.reviews = extractAllReviews(page);

            context.close();
        }

        return result;
    }

    public AggregateResult scrapeAggregate() {
        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = openContext(playwright);
            Page page = openMapsPage(context);
            clickReviewsTab(page);
            clickSortBy(page, config.sortBy());
            AggregateResult result = readAggregate(page);
            context.close();
            return result;
        }
    }

    public List<GoogleReview> scrapeReviews() {
        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = openContext(playwright);
            Page page = openMapsPage(context);
            clickReviewsTab(page);
            clickSortBy(page, config.sortBy());
            AggregateResult aggregate = readAggregate(page);
            scrollReviewsPanel(page, aggregate.totalReviewCount);
            List<GoogleReview> reviews = extractAllReviews(page);
            context.close();
            return reviews;
        }
    }

    /**
     * Opens a persistent-profile browser context that the caller owns and
     * must close — for batch runs that navigate to many URLs across a
     * single browser session instead of launching/closing per location
     * (launching 100+ times back-to-back on the same profile directory is
     * unreliable — the previous process doesn't always release its lock
     * before the next launch, confirmed by repeated testing).
     */
    public Page openPersistentPage(Playwright playwright) {
        BrowserContext context = openContext(playwright);
        Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(config.playwrightTimeoutMs());
        return page;
    }

    /**
     * Reads the aggregate stats for one URL using an already-open page
     * (see openPersistentPage). Does not open/close a browser context.
     */
    public AggregateResult scrapeAggregateOnPage(Page page, String url) {
        page.navigate(url);
        page.waitForTimeout(3000);
        clickReviewsTab(page);
        clickSortBy(page, config.sortBy());
        return readAggregate(page);
    }

    /**
     * Full aggregate + review-list scrape for one URL using an already-open
     * page (see openPersistentPage). Does not open/close a browser context —
     * for batch runs across many locations in one browser session.
     */
    public ScrapeResult scrapeOnPage(Page page, String url) {
        ScrapeResult result = new ScrapeResult();

        page.navigate(url);
        page.waitForTimeout(3000);
        clickReviewsTab(page);
        clickSortBy(page, config.sortBy());
        result.aggregate = readAggregate(page);
        scrollReviewsPanel(page, result.aggregate.totalReviewCount);
        result.reviews = extractAllReviews(page);

        return result;
    }

    /**
     * Reads rating + total count from the Reviews tab, where Google shows the
     * comma-formatted count ("1,161 reviews") reliably. Falls back to the
     * Overview-style "(N)" format and the star histogram if that text isn't found.
     */
    private AggregateResult readAggregate(Page page) {
        AggregateResult result = new AggregateResult();
        String pageText = page.locator("div[role='main']").first().innerText();

        Matcher ratingMatcher = AGGREGATE_RATING_PATTERN.matcher(pageText);
        if (ratingMatcher.find()) {
            result.rating = Double.parseDouble(ratingMatcher.group(1));
        }

        Matcher textCountMatcher = AGGREGATE_COUNT_TEXT_PATTERN.matcher(pageText);
        if (textCountMatcher.find()) {
            result.totalReviewCount = Integer.parseInt(textCountMatcher.group(1).replace(",", ""));
            return result;
        }

        Matcher parenCountMatcher = AGGREGATE_COUNT_PAREN_PATTERN.matcher(pageText);
        if (parenCountMatcher.find()) {
            result.totalReviewCount = Integer.parseInt(parenCountMatcher.group(1));
            return result;
        }

        int total = 0;
        boolean foundHistogram = false;
        Matcher histogramMatcher = HISTOGRAM_PATTERN.matcher(pageText);
        while (histogramMatcher.find()) {
            total += Integer.parseInt(histogramMatcher.group(2));
            foundHistogram = true;
        }
        result.totalReviewCount = foundHistogram ? total : null;

        return result;
    }

    private List<GoogleReview> extractAllReviews(Page page) {
        List<GoogleReview> reviews = new ArrayList<>();
        Locator cards = page.locator("div.jftiEf");
        int count = cards.count();

        for (int i = 0; i < count; i++) {
            GoogleReview review = extractReview(cards.nth(i));
            if (review != null) {
                reviews.add(review);
            }
        }

        return reviews;
    }

    private BrowserContext openContext(Playwright playwright) {
        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                .setChannel("chrome")
                .setHeadless(config.playwrightHeadless());

        if (!config.playwrightHeadless()) {
            options.setArgs(java.util.List.of("--start-maximized"));
        }

        return playwright.chromium().launchPersistentContext(
                java.nio.file.Path.of(config.chromeProfileDir()), options);
    }

    private Page openMapsPage(BrowserContext context) {
        Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
        page.setDefaultTimeout(config.playwrightTimeoutMs());
        page.navigate(config.googleMapsUrl());
        page.waitForTimeout(3000);
        return page;
    }

    private void clickReviewsTab(Page page) {
        Locator reviewsTab = page.locator("button:has-text('Reviews')").first();
        if (reviewsTab.count() > 0) {
            reviewsTab.click();
            page.waitForTimeout(1500);
        }
    }

    /**
     * Clicks Google's "Sort" control and picks the given option
     * (newest, highest, lowest, relevance). Best-effort — if the sort
     * menu isn't found (layout variance), scraping continues with
     * whatever default order Google already applied.
     */
    private void clickSortBy(Page page, String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return;
        }

        Locator sortButton = page.locator("button[aria-label='Sort reviews']").first();
        if (sortButton.count() == 0) {
            sortButton = page.locator("button:has-text('Sort')").first();
        }
        if (sortButton.count() == 0) {
            return;
        }

        try {
            sortButton.click();
            page.waitForTimeout(800);

            String optionText = switch (sortBy.toLowerCase()) {
                case "newest" -> "Newest";
                case "highest" -> "Highest rating";
                case "lowest" -> "Lowest rating";
                default -> "Most relevant";
            };

            Locator option = page.locator("div[role='menuitemradio']:has-text('" + optionText + "')").first();
            if (option.count() > 0) {
                option.click();
                page.waitForTimeout(1500);
            }
        } catch (Exception ignored) {
            // Sort menu layout may differ — fall back to default order.
        }
    }

    /**
     * Scrolls the reviews panel via direct DOM scrollTop manipulation
     * (faster and more reliable than simulated mouse-wheel events). Stops
     * as soon as the loaded card count reaches the known total from the
     * aggregate check, falling back to a stable-count heuristic when the
     * total is unknown.
     */
    // Finds the actual scrolling ancestor of a review card at runtime — the
    // container's CSS class is obfuscated and changes, but it always has
    // overflowY:auto/scroll with scrollHeight > clientHeight, so we detect
    // it structurally instead of guessing a class name.
    private static final String FIND_SCROLLABLE_JS = """
            () => {
                const card = document.querySelector('div.jftiEf');
                if (!card) return false;
                let el = card.parentElement;
                while (el) {
                    const style = getComputedStyle(el);
                    if ((style.overflowY === 'auto' || style.overflowY === 'scroll')
                            && el.scrollHeight > el.clientHeight) {
                        el.setAttribute('data-review-scroll-target', 'true');
                        return true;
                    }
                    el = el.parentElement;
                }
                return false;
            }
            """;

    private void scrollReviewsPanel(Page page, Integer targetCount) {
        if (!(Boolean) page.evaluate(FIND_SCROLLABLE_JS)) {
            return;
        }
        Locator scrollable = page.locator("[data-review-scroll-target='true']").first();

        int effectiveTarget = targetCount != null ? targetCount : Integer.MAX_VALUE;
        if (config.maxReviews() > 0) {
            effectiveTarget = Math.min(effectiveTarget, config.maxReviews());
        }

        // MAX_SCROLL_ATTEMPTS is a per-run floor, not a hard ceiling — a fixed
        // cap silently truncated large listings (500-1000+ reviews) in earlier
        // testing. When the true total is known, guarantee at least one
        // attempt per review (worst case: one new card per scroll), so a big
        // listing gets the cycles it actually needs without manual tuning.
        int maxAttempts = config.maxScrollAttempts();
        if (targetCount != null) {
            maxAttempts = Math.max(maxAttempts, targetCount);
        }

        int previousCount = -1;
        int idleRounds = 0;
        int idleLimit = config.scrollIdleLimit();

        for (int i = 0; i < maxAttempts; i++) {
            int currentCount = page.locator("div.jftiEf").count();

            if (currentCount >= effectiveTarget) {
                break;
            }
            if (currentCount == previousCount) {
                idleRounds++;
                if (idleLimit > 0 && idleRounds >= idleLimit) {
                    break;
                }
            } else {
                idleRounds = 0;
            }
            previousCount = currentCount;

            scrollable.evaluate("el => el.scrollTop = el.scrollHeight");
            // Back off as stalls persist — Google's lazy-load can pause under
            // load; waiting longer during a stall avoids mistaking a slow
            // batch for the end of the list, without slowing the common case
            // where cards are loading steadily.
            int wait = idleRounds == 0 ? 500 : Math.min(500 + idleRounds * 300, 3000);
            page.waitForTimeout(wait);
        }
    }

    private GoogleReview extractReview(Locator card) {
        GoogleReview review = new GoogleReview();

        Locator nameLocator = card.locator("div.d4r55");
        review.reviewerName = nameLocator.count() > 0 ? nameLocator.innerText().trim() : null;

        Locator ratingLocator = card.locator("span.kvMYJc");
        if (ratingLocator.count() > 0) {
            review.starRating = parseStars(ratingLocator.getAttribute("aria-label"));
        }

        Locator commentLocator = card.locator("span.wiI7pd");
        // Cards with an owner reply have a second "More" button for the reply
        // text — .first() targets the review's own comment toggle, which
        // always renders before the reply's. A bare (unscoped) locator here
        // matches both and throws a strict-mode error on .click(), which was
        // being silently swallowed by the catch below.
        Locator moreButton = card.locator("button:has-text('More')").first();

        // Retry the click itself, not just the wait — a single click can miss
        // if the button hasn't finished animating into place yet.
        for (int attempt = 0; attempt < 3 && moreButton.count() > 0; attempt++) {
            String beforeExpand = commentLocator.count() > 0 ? commentLocator.innerText() : "";
            try {
                moreButton.scrollIntoViewIfNeeded();
                moreButton.click(new Locator.ClickOptions().setForce(true).setTimeout(3000));
            } catch (Exception ignored) {
                // Button may already be gone/expanded; safe to ignore.
            }

            boolean expanded = false;
            for (int i = 0; i < 10; i++) {
                card.page().waitForTimeout(150);
                String afterExpand = commentLocator.count() > 0 ? commentLocator.innerText() : "";
                if (!afterExpand.equals(beforeExpand)) {
                    expanded = true;
                    break;
                }
            }
            if (expanded || moreButton.count() == 0) {
                break;
            }
        }

        review.comment = commentLocator.count() > 0 ? commentLocator.innerText().trim() : null;

        Locator timeLocator = card.locator("span.rsqaWe");
        review.relativeTime = timeLocator.count() > 0 ? timeLocator.innerText().trim() : null;

        return review;
    }

    private Integer parseStars(String ariaLabel) {
        if (ariaLabel == null) {
            return null;
        }
        Matcher matcher = STARS_PATTERN.matcher(ariaLabel);
        if (matcher.find()) {
            return (int) Double.parseDouble(matcher.group(1));
        }
        return null;
    }
}
