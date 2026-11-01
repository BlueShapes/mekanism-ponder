package dev.mekanismponder.client;

import dev.mekanismponder.GuideResources;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuidePlugin implements PonderPlugin {
    private static final Map<String, StoryBoardEntry> ENTRIES = new LinkedHashMap<>();
    private static final List<StoryBoardEntry> COURSE = new ArrayList<>();

    public static List<StoryBoardEntry> course() { return List.copyOf(COURSE); }

    public static StoryBoardEntry entry(String lessonId) {
        var entry = ENTRIES.get(lessonId);
        if (entry == null) throw new IllegalStateException("Ponder lesson has not been registered: " + lessonId);
        return entry;
    }

    @Override public String getModId() { return "mekanismponder"; }

    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ENTRIES.clear();
        COURSE.clear();
        for (var lesson : GuideBook.lessons()) {
            var tag = GuideResources.of(getModId(), lesson.chapter());
            var entry = helper.addStoryBoard(GuideResources.parse(lesson.icon()), "stage",
                    (scene, util) -> GuideScenes.build(lesson, scene, util), tag);
            ENTRIES.put(lesson.id(), entry);
            // PonderUI resolves the first compiled scene to an ItemLike for its header icon.
            // Reusing each lesson's real icon entry gives the ordered course a real item without
            // registering a fake component that other Ponder integrations could enumerate.
            COURSE.add(entry);
            for (var component : lesson.components()) {
                if (!component.equals(lesson.icon())) helper.addStoryBoard(GuideResources.parse(component), "stage",
                        (scene, util) -> GuideScenes.build(lesson, scene, util), tag);
            }
        }
    }

    @Override public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        for (var chapter : GuideChapter.active(GuideBook.lessons())) {
            var icon = BuiltInRegistries.ITEM.getOptional(GuideResources.parse(chapter.icon()))
                    .orElseThrow(() -> new IllegalStateException("Missing chapter icon item: " + chapter.icon()));
            helper.registerTag(GuideResources.of(getModId(), chapter.id()))
                    .title(chapter.englishName()).description(chapter.englishDescription())
                    .item(icon).addToIndex().register();
        }
        for (var lesson : GuideBook.lessons()) {
            var tag = GuideResources.of(getModId(), lesson.chapter());
            helper.addTagToComponent(GuideResources.parse(lesson.icon()), tag);
            for (var component : lesson.components()) helper.addTagToComponent(GuideResources.parse(component), tag);
        }
    }

    @Override public void registerSharedText(SharedTextRegistrationHelper helper) {
        for (var lesson : GuideBook.lessons()) for (int index = 0; index < lesson.steps().size(); index++)
            helper.registerSharedText(lesson.id() + "_" + index, lesson.steps().get(index).text().en());
    }
}
