package com.yosi.reviewcomparator.util;

import java.util.regex.Pattern;

public class TextNormalizer {

    // Matches :shortcode: style emoji placeholders, e.g. :tada:, :heart_eyes:
    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(":[a-zA-Z0-9_+-]+:");

    // Matches emoji, symbols, dingbats, arrows, currency, and letterlike symbols
    // (covers both real Unicode emoji from Google and the "©®™★" style symbols
    // that show up alongside them).
    private static final Pattern UNICODE_EMOJI_PATTERN = Pattern.compile(
            "[\\x{1F000}-\\x{1FFFF}\\x{2000}-\\x{2BFF}\\x{2100}-\\x{214F}\\x{20A0}-\\x{20CF}"
                    + "\\x{00A9}\\x{00AE}\\uFE0F]");

    // The DB stores a literal '?' wherever an emoji/symbol failed to encode —
    // treat it the same as an emoji: strip it rather than compare it.
    private static final Pattern MOJIBAKE_PLACEHOLDER_PATTERN = Pattern.compile("\\?");

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String result = SHORTCODE_PATTERN.matcher(text).replaceAll("");
        result = UNICODE_EMOJI_PATTERN.matcher(result).replaceAll("");
        result = MOJIBAKE_PLACEHOLDER_PATTERN.matcher(result).replaceAll("");
        result = result.replaceAll("\\s+", " ").trim().toLowerCase();
        return result;
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase();
    }
}
