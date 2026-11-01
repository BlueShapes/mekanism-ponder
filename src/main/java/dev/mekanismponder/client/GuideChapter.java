package dev.mekanismponder.client;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public enum GuideChapter {
    START("start", "Getting Started", "Power and the first materials", "mekanism:metallurgic_infuser"),
    TRANSPORT("transport", "Transport", "Routes for items, fluids and chemicals", "mekanism:basic_logistical_transporter"),
    PROCESSING("processing", "Processing", "Ore and raw ore yields, and parallel processing in factories", "mekanism:enrichment_chamber"),
    CHEMISTRY("chemistry", "Chemistry", "Electrolysis, evaporation and chemical reactions", "mekanism:chemical_oxidizer"),
    ADVANCED("advanced", "Advanced Systems", "Energy storage, mining, QIO and teleportation", "mekanism:induction_casing"),
    TROUBLESHOOTING("troubleshooting", "Troubleshooting", "Power shortages and stalled chemical processing", "mekanism:configurator");

    private final String id;
    private final String englishName;
    private final String englishDescription;
    private final String icon;

    GuideChapter(String id, String englishName, String englishDescription, String icon) {
        this.id = id;
        this.englishName = englishName;
        this.englishDescription = englishDescription;
        this.icon = icon;
    }

    public String id() { return id; }
    public String englishName() { return englishName; }
    public String englishDescription() { return englishDescription; }
    public String icon() { return icon; }
    public Component label() { return Component.translatable("mekanismponder.ponder.tag." + id); }

    public static GuideChapter fromId(String id) {
        return Arrays.stream(values()).filter(chapter -> chapter.id.equals(id)).findFirst().orElse(null);
    }

    public static List<GuideChapter> active(List<GuideBook.Lesson> lessons) {
        return Arrays.stream(values()).filter(chapter -> lessons.stream().anyMatch(lesson -> lesson.chapter().equals(chapter.id))).toList();
    }
}
