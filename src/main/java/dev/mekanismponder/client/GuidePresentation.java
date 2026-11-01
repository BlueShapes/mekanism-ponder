package dev.mekanismponder.client;

import com.google.gson.Gson;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.client.Minecraft;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

/** Author-owned intentions follow each locale's explicit display boundaries. */
public final class GuidePresentation {
    private static final String RESOURCE = "/assets/mekanismponder/guide/presentation.json";

    public enum Intent {
        NORMAL(PonderPalette.WHITE), WARNING(PonderPalette.RED), HINT(PonderPalette.GREEN);

        private final PonderPalette palette;
        Intent(PonderPalette palette) { this.palette = palette; }
        public PonderPalette palette() { return palette; }

        /** Match native Ponder's light text tint for our appended tooltip lines. */
        public int textColor() {
            // TextColor stores 24-bit RGB, whereas Catnip Color includes alpha.
            return palette.getColorObject().mixWith(new Color(0xffffffdd), .5f).getRGB() & 0xffffff;
        }
    }

    public record Unit(String text, Intent intent) {}
    private record Data(Map<String, Map<String, List<Intent>>> locales) {}
    private static final class Holder { private static final Data DATA = load(); }
    private GuidePresentation() {}

    public static List<Unit> units(String key, String text) {
        var locale = Minecraft.getInstance().getLanguageManager().getSelected();
        var entries = Holder.DATA.locales().getOrDefault(locale, Holder.DATA.locales().get("en_us"));
        var intentions = entries.get(key);
        var units = GuideTextLayout.captionPages(text);
        // Resource packs can change authored boundaries. Never apply mismatched indices.
        boolean matches = intentions != null && intentions.size() == units.size();
        return IntStream.range(0, units.size()).mapToObj(index ->
                new Unit(units.get(index), matches ? intentions.get(index) : Intent.NORMAL)).toList();
    }

    private static Data load() {
        try (var reader = new InputStreamReader(Objects.requireNonNull(
                GuidePresentation.class.getResourceAsStream(RESOURCE), RESOURCE), StandardCharsets.UTF_8)) {
            var data = new Gson().fromJson(reader, Data.class);
            if (data == null || data.locales() == null || !data.locales().containsKey("en_us"))
                throw new IllegalStateException("Missing presentation locale data");
            return data;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load guide text intentions", exception);
        }
    }
}
