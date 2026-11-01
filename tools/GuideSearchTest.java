import dev.mekanismponder.client.GuideSearchText;
import java.util.Locale;

/** Runs the actual pure production matcher without launching Minecraft. */
public final class GuideSearchTest {
    private static int checked;

    private static void match(String label, String text, String query, Locale locale, boolean expected) {
        String originalText = text;
        String originalQuery = query;
        if (GuideSearchText.contains(text, query, locale) != expected)
            throw new AssertionError(label);
        if (!text.equals(originalText) || !query.equals(originalQuery))
            throw new AssertionError(label + ": mutated source");
        checked++;
    }

    public static void main(String[] args) {
        Locale tr = Locale.forLanguageTag("tr-TR");
        // Reproduce the previous lowercase-only failure with the reported real term.
        if ("Küp".toLowerCase(tr).contains("Ku\u0308p".toLowerCase(tr)))
            throw new AssertionError("legacy reproduction no longer describes the old path");
        match("Turkish composed versus decomposed", "Küp", "Ku\u0308p", tr, true);
        match("Turkish reverse canonical form", "Ku\u0308p", "Küp", tr, true);
        match("Turkish dotted uppercase", "İLETİM", "iletim", tr, true);
        match("Turkish decomposed dotted uppercase", "I\u0307LETİM", "iletim", tr, true);
        match("Turkish dotless uppercase", "IŞIK", "ışık", tr, true);
        match("Turkish I does not become i", "I", "i", tr, false);
        match("Turkish I becomes dotless i", "I", "ı", tr, true);
        match("English case", "Energy Cube", "ENERGY", Locale.ENGLISH, true);
        match("Canonical French accent", "Énergie", "E\u0301NERGIE", Locale.FRENCH, true);
        match("Accents remain meaningful", "Énergie", "energie", Locale.FRENCH, false);
        match("Japanese canonical dakuten", "ガス", "カ\u3099ス", Locale.JAPANESE, true);
        Locale ar = Locale.forLanguageTag("ar");
        match("Arabic canonical hamza", "أكسجين", "ا\u0654كسجين", ar, true);
        match("Arabic logical order", "أكسجين", "نيجسكأ", ar, false);
        match("Registry ID remains literal", "mekanism:basic_energy_cube", "mekanism:basic_energy_cube", Locale.ROOT, true);
        if (!GuideSearchText.fold("الطاقة QIO", ar).equals("الطاقة qio"))
            throw new AssertionError("Arabic shaping or visual reversal was introduced");
        if (GuideSearchText.fold("Ａ", Locale.ROOT).equals("a"))
            throw new AssertionError("NFC must not introduce compatibility folding");
        checked += 2;
        System.out.println("Search regression: PASS (" + checked + " cases; legacy canonical mismatch reproduced)");
    }
}
