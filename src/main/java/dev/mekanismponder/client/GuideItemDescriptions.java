package dev.mekanismponder.client;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import dev.mekanismponder.GuideResources;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Language-aware descriptions appended without replacing the item's normal tooltip. */
public final class GuideItemDescriptions {
    private static final String RESOURCE = "/assets/mekanismponder/guide/item_descriptions.json";
    private static final String DESCRIPTION_KEY = "mekanismponder.description.";
    private static final int MAX_WIDTH = 240;

    private GuideItemDescriptions() {}

    private static final class Holder {
        private static final Index INDEX = load();
    }

    private record Index(List<Entry> descriptions, Map<ResourceLocation, Entry> byItem) {}

    public record Entry(String id, List<String> items, GuideBook.Localized text) {
        public Entry {
            if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid description ID: " + id);
            items = List.copyOf(Objects.requireNonNull(items, "Description item list"));
            if (items.isEmpty()) throw new IllegalArgumentException("Description has no items: " + id);
            items.forEach(GuideResources::parse);
            Objects.requireNonNull(text, "Description text");
        }

        public String translationKey() { return DESCRIPTION_KEY + id; }
    }

    public static List<Entry> descriptions() { return Holder.INDEX.descriptions(); }

    public static void append(ItemStack stack, List<Component> tooltip, boolean expanded) {
        if (stack.isEmpty()) return;
        Entry entry = Holder.INDEX.byItem().get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (entry == null) return;
        List<Component> additions = expanded ? expandedDescription(entry) : collapsedHint();
        tooltip.addAll(Math.min(1, tooltip.size()), additions);
    }

    private static List<Component> collapsedHint() {
        return List.of(Component.translatable("mekanismponder.tooltip.hold_for_description",
                Component.translatable("key.keyboard.left.shift"))
                .withStyle(style -> style.withColor(GuidePresentation.Intent.HINT.textColor())));
    }

    private static List<Component> expandedDescription(Entry entry) {
        String text = Component.translatable(entry.translationKey()).getString();
        var client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int width = Math.max(1, Math.min(MAX_WIDTH, screenWidth - 32));
        var result = new ArrayList<Component>();
        for (var unit : GuidePresentation.units(entry.translationKey(), text)) {
            for (var line : GuideTextLayout.wrap(client.font, unit.text(), width))
                result.add(Component.literal(line).withStyle(style -> style.withColor(unit.intent().textColor())));
        }
        return List.copyOf(result);
    }

    private static Index load() {
        try {
            var stream = Objects.requireNonNull(GuideItemDescriptions.class.getResourceAsStream(RESOURCE),
                    "Missing item descriptions: " + RESOURCE);
            Entry[] entries;
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                entries = new Gson().fromJson(reader, Entry[].class);
            }
            if (entries == null) throw new IllegalStateException("Item descriptions are empty");
            var descriptions = List.copyOf(Arrays.asList(entries));
            var ids = new HashSet<String>();
            var itemIds = new HashSet<ResourceLocation>();
            var byItem = new HashMap<ResourceLocation, Entry>();
            for (var entry : descriptions) {
                if (!ids.add(entry.id())) throw new IllegalStateException("Duplicate item description ID: " + entry.id());
                for (var item : entry.items()) {
                    var itemId = GuideResources.parse(item);
                    if (!itemIds.add(itemId)) throw new IllegalStateException("Duplicate described item: " + itemId);
                    byItem.put(itemId, entry);
                }
            }
            return new Index(descriptions, Map.copyOf(byItem));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load Mekanism Ponder item descriptions", exception);
        }
    }
}
