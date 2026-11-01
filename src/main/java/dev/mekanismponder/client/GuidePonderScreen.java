package dev.mekanismponder.client;

import dev.mekanismponder.GuideResources;
import dev.mekanismponder.mixin.PonderUIAccessor;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

/** Uses Ponder's standard controls and history for the selected course entry. */
public final class GuidePonderScreen extends PonderUI {
    private ResourceLocation synchronizedScene;
    private final String language;

    public GuidePonderScreen(GuideBook.Lesson lesson, ItemStack subject) {
        super(PonderIndex.getSceneAccess().compile(GuidePlugin.course()));
        language = Minecraft.getInstance().getLanguageManager().getSelected();
        int index = GuideBook.lessons().indexOf(lesson);
        if (index < 0) throw new IllegalArgumentException("Unknown guide lesson: " + lesson.id());
        var access = (PonderUIAccessor) (Object) this;
        access.mekanismponder$setStack(subject.copy());
        access.mekanismponder$setIndex(index);
        access.mekanismponder$getLazyIndex().startWithValue(index);
        synchronizeActiveLesson(true);
    }

    @Override
    protected void init() {
        synchronizeActiveLesson(true);
        super.init();
    }

    @Override
    protected boolean scroll(boolean forward) {
        boolean moved = super.scroll(forward);
        if (moved && synchronizeActiveLesson(false)) rebuildWidgets();
        return moved;
    }

    @Override
    public void tick() {
        super.tick();
        // Native tag context can select an index directly without calling scroll.
        if (synchronizeActiveLesson(false)) rebuildWidgets();
    }

    private boolean synchronizeActiveLesson(boolean refreshTags) {
        var sceneId = getActiveScene().getId();
        if (!sceneId.getNamespace().equals("mekanismponder")) return false;
        if (!refreshTags && sceneId.equals(synchronizedScene)) return false;
        var lesson = GuideBook.lessons().stream().filter(candidate -> candidate.id().equals(sceneId.getPath()))
                .findFirst().orElse(null);
        if (lesson == null) return false;

        var access = (PonderUIAccessor) (Object) this;
        boolean subjectChanged = synchronizedScene != null && !sceneId.equals(synchronizedScene);
        if (subjectChanged) {
            var item = BuiltInRegistries.ITEM.get(GuideResources.parse(lesson.icon()));
            access.mekanismponder$setStack(new ItemStack(item));
        }
        synchronizedScene = sceneId;

        var tagAccess = PonderIndex.getTagAccess();
        var chapter = tagAccess.getRegisteredTag(GuideResources.of("mekanismponder", lesson.chapter()));
        if (chapter == null) throw new IllegalStateException("Missing guide chapter tag: " + lesson.chapter());
        var tags = new ArrayList<PonderTag>();
        tags.add(chapter);
        // Keep tags owned by other integrations for the actual subject.
        tagAccess.getTags(BuiltInRegistries.ITEM.getKey(getSubject().getItem())).stream()
                .filter(tag -> !tag.getId().getNamespace().equals("mekanismponder"))
                .forEach(tags::add);
        boolean tagsChanged = !access.mekanismponder$getTags().equals(tags);
        if (tagsChanged) {
            access.mekanismponder$getTags().clear();
            access.mekanismponder$getTags().addAll(tags);
        }
        return subjectChanged || tagsChanged;
    }

    public static void open(GuideBook.Lesson lesson) {
        var item = BuiltInRegistries.ITEM.get(GuideResources.parse(lesson.icon()));
        open(lesson, new ItemStack(item));
    }

    public static void open(GuideBook.Lesson lesson, ItemStack subject) {
        var screen = new GuidePonderScreen(lesson, subject);
        if (Minecraft.getInstance().screen instanceof NavigatableSimiScreen)
            ScreenOpener.transitionTo(screen);
        else
            ScreenOpener.open(screen);
    }

    @Override
    public boolean isEquivalentTo(NavigatableSimiScreen other) {
        return other instanceof GuidePonderScreen guide && language.equals(guide.language)
                && super.isEquivalentTo(other)
                && getActiveScene().getId().equals(guide.getActiveScene().getId());
    }
}
