package dev.mekanismponder.client;

import java.text.Normalizer;
import java.util.Locale;

/** Canonical matching of logical display text; never rewrites stored translations or IDs. */
public final class GuideSearchText {
    private GuideSearchText() {}

    public static boolean contains(String text, String query, Locale locale) {
        return fold(text, locale).contains(fold(query, locale));
    }

    public static String fold(String text, Locale locale) {
        // Compose before locale-sensitive casing so decomposed Turkish I + dot
        // follows the same rule as İ. Compose again after casing for stable keys.
        String canonical = Normalizer.normalize(text, Normalizer.Form.NFC);
        return Normalizer.normalize(canonical.toLowerCase(locale), Normalizer.Form.NFC);
    }
}
