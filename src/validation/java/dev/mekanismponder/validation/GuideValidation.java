package dev.mekanismponder.validation;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import dev.mekanismponder.GuideResources;
import dev.mekanismponder.client.GuideBook;
import dev.mekanismponder.client.GuideChapter;
import dev.mekanismponder.client.GuideCommands;
import dev.mekanismponder.client.GuideItemDescriptions;
import dev.mekanismponder.client.GuidePlugin;
import dev.mekanismponder.client.GuidePresentation;
import dev.mekanismponder.client.GuidePonderScreen;
import dev.mekanismponder.client.GuideScenes;
import dev.mekanismponder.client.GuideTextLayout;
import dev.mekanismponder.mixin.PonderUIAccessor;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.foundation.ui.PonderTagIndexScreen;
import net.createmod.ponder.foundation.ui.PonderTagScreen;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Opt-in, client-only integration validation; this class is excluded from the released JAR. */
public final class GuideValidation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long TIMEOUT_NANOS = 600_000_000_000L;
    private static final int STARTUP_RECOVERY_TICKS = 20 * 12;
    private static final List<Integer> CAPTION_GUI_WIDTHS = List.of(320, 480, 960);
    private static final Set<String> REQUIRED_COVERAGE_CAPTIONS = Set.of("power_2", "diagnose_processing_3");
    private static final List<CompiledLesson> COMPILED = new ArrayList<>();
    private static final List<String> SCREENSHOTS = new ArrayList<>();
    private static final List<Map<String, Object>> CAPTION_LAYOUT_ISSUES = new ArrayList<>();
    private static final List<Map<String, Object>> CAPTION_MEASUREMENTS = new ArrayList<>();

    private static long startedAt;
    private static boolean navigationOnly;
    private static boolean coverageMode;
    private static boolean representativeGui;
    private static boolean openingWorld;
    private static boolean worldReady;
    private static boolean waitingForRenderedFrame;
    private static CompletableFuture<Void> languageReload;
    private static String reloadTarget;
    private static int startupTicks;
    private static List<GuideBook.Lesson> lessons;
    private static int validateIndex;
    private static int uiStage;
    private static int ponderWaitFrames;
    private static int renderLessonIndex;
    private static int renderPhase;
    private static int renderWaitFrames;
    private static boolean englishIndexRendered;
    private static int verifiedCallouts;
    private static boolean replayVerified;
    private static boolean transmitterModelDataVerified;
    private static boolean nativeIndexVerified;
    private static boolean uiNavigationVerified;
    private static boolean descriptionsVerified;
    private static CommandDispatcher<CommandSourceStack> commandDispatcher;
    private static List<String> validationLocales;
    private static final List<String> VERIFIED_LOCALES = new ArrayList<>();
    private static int localePhase;
    private static int localeFrames;
    private static boolean englishFallbackVerified;
    private static boolean presentationFallbackVerified;
    private static int captionPagesChecked;
    private static int captionUnitsChecked;
    private static int captionGuiWidthChecks;
    private static int maximumCaptionLines;
    private static boolean captionWindowWidthsVerified;
    private static int nativeScreenFrames;
    private static int glyphsChecked;
    private static boolean tagPaginationVerified;
    private static boolean factoryBoundariesVerified;
    private static boolean ordinaryGuideButtonsAbsent;
    private static boolean componentFactoryVerified;
    private static boolean playbackTagVerified;
    private static boolean chapterBoundaryVerified;
    private static GuideBook.Lesson componentLesson;
    private static ResourceLocation componentSubject;
    private static final List<PaletteSample> PALETTE_SAMPLES = List.of(
            new PaletteSample(2, 0, GuidePresentation.Intent.NORMAL),
            new PaletteSample(0, 2, GuidePresentation.Intent.HINT),
            new PaletteSample(3, 0, GuidePresentation.Intent.WARNING));
    private static PonderScene paletteScene;
    private static int paletteSampleIndex;
    private static int palettePhase;
    private static boolean captionPalettesVerified;
    private static boolean tooltipPalettesVerified;
    private static GuidePonderScreen japaneseHistoryGuide;
    private static boolean historyTypeGuardVerified;
    private static boolean historyLocaleGuardVerified;
    private static int representativeArabicPhase;
    private static boolean arabicFontRechecked;
    private static int coverageLocaleIndex;
    private static Set<String> validationCaptionIds = Set.of();

    private GuideValidation() {}

    static boolean initialize() {
        coverageMode = Boolean.getBoolean("mekanismponder.validateCoverage");
        String descriptionSelection = System.getProperty("mekanismponder.validationDescriptions", "").trim();
        String captionSelection = System.getProperty("mekanismponder.validationCaptions", "").trim();
        if (coverageMode && (descriptionSelection.isEmpty() != captionSelection.isEmpty()))
            throw new IllegalArgumentException("Coverage description and caption filters must be supplied together");
        validationCaptionIds = coverageMode ? parseCoverageCaptionIds(captionSelection) : Set.of();
        representativeGui = Boolean.getBoolean("mekanismponder.validateRepresentativeGui");
        navigationOnly = coverageMode || representativeGui || Boolean.getBoolean("mekanismponder.validateNavigation");
        if (!navigationOnly && !Boolean.getBoolean("mekanismponder.validate")) return false;
        var client = Minecraft.getInstance();
        startedAt = System.nanoTime();
        ValidationPlatform.hideWindow(client);
        beginLanguageReload("ja_jp");
        try {
            Files.deleteIfExists(client.gameDirectory.toPath().resolve("validation-result.json"));
            Files.deleteIfExists(client.gameDirectory.toPath().resolve("validation-failure.txt"));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not clear previous validation output", exception);
        }
        LOGGER.info("MEKANISM_PONDER_VALIDATION_INITIALIZED: navigationOnly={}, representativeGui={}, screen={}, language={}",
                navigationOnly, representativeGui, screenName(client), client.getLanguageManager().getSelected());
        return true;
    }

    private static void beginLanguageReload(String locale) {
        var client = Minecraft.getInstance();
        // LoadingOverlay renders the current screen after the old font atlas has closed.
        if (worldReady) client.setScreen(new ReloadBlankScreen());
        LanguageManager manager = client.getLanguageManager();
        manager.setSelected(locale);
        client.options.languageCode = locale;
        reloadTarget = locale;
        languageReload = client.reloadResourcePacks();
    }

    static void rendered() {
        waitingForRenderedFrame = true;
    }

    static void tick() {
        var client = Minecraft.getInstance();
        if (System.nanoTime() - startedAt > TIMEOUT_NANOS) {
            fail(client, new IllegalStateException("Validation exceeded its 600 second deadline at stage " + uiStage));
            return;
        }
        try {
            if (!awaitLanguageReload()) return;
            if (!openingWorld) {
                if (client.getOverlay() == null && client.screen instanceof TitleScreen) {
                    verifyNoOrdinaryGuideButtons(client.screen);
                    openingWorld = true;
                    ValidationPlatform.createFreshLevel(client);
                    return;
                }
                if (++startupTicks > STARTUP_RECOVERY_TICKS && client.level == null) {
                    ValidationPlatform.recoverInitialScreen(client);
                    startupTicks = STARTUP_RECOVERY_TICKS - 80;
                }
                return;
            }
            if (client.getOverlay() != null || client.level == null || client.player == null) return;
            if (!waitingForRenderedFrame) return;
            waitingForRenderedFrame = false;

            if (!worldReady) {
                if (client.screen != null) {
                    if (System.nanoTime() - startedAt > 30_000_000_000L) client.setScreen(null);
                    return;
                }
                worldReady = true;
                LOGGER.info("MEKANISM_PONDER_VALIDATION_WORLD_READY");
                lessons = navigationOnly ? GuideBook.lessons() : selectLessons();
                if (coverageMode) {
                    ItemCoverageValidation.initialize(System.getProperty("mekanismponder.validationCoverageExpectations", ""));
                    if (!validationCaptionIds.isEmpty()) {
                        // Scoped font checks reuse the unchanged native UI evidence and avoid
                        // keeping an Options screen's baked glyphs across resource reloads.
                        coverageLocaleIndex = 0;
                        uiStage = 42;
                        beginLanguageReload(ItemCoverageValidation.locales().get(coverageLocaleIndex));
                    }
                }
                return;
            }

            if (navigationOnly) {
                if (uiStage == 0) {
                    verifyDescriptionResources();
                    validateUiLanguage("ja_jp");
                    client.setScreen(ValidationPlatform.createOptionsScreen(client));
                    uiStage = 1;
                } else {
                    runUiAndRendering(client);
                }
                return;
            }

            if (validateIndex < lessons.size()) {
                validateLesson(lessons.get(validateIndex++));
                return;
            }
            if (!descriptionsVerified) {
                verifyDescriptionResources();
                descriptionsVerified = true;
                validateUiLanguage("ja_jp");
                client.setScreen(ValidationPlatform.createOptionsScreen(client));
                uiStage = 1;
                return;
            }
            runUiAndRendering(client);
        } catch (Throwable exception) {
            fail(client, exception);
        }
    }

    private static boolean awaitLanguageReload() {
        if (languageReload == null) return true;
        if (!languageReload.isDone()) return false;
        try {
            languageReload.join();
            LOGGER.info("MEKANISM_PONDER_VALIDATION_LANGUAGE_READY: {}", reloadTarget);
            languageReload = null;
            return true;
        } catch (CompletionException exception) {
            throw new IllegalStateException("Could not reload validation locale " + reloadTarget, exception.getCause());
        }
    }

    private static List<GuideBook.Lesson> selectLessons() {
        String selection = System.getProperty("mekanismponder.validationLessons", "").strip();
        if (selection.isEmpty()) return GuideBook.lessons();
        var ids = Arrays.stream(selection.split(",")).map(String::strip).filter(value -> !value.isEmpty()).toList();
        if (new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Duplicate validation lesson ID: " + selection);
        var selected = GuideBook.lessons().stream().filter(lesson -> ids.contains(lesson.id())).toList();
        if (selected.size() != ids.size()) throw new IllegalArgumentException("Unknown validation lesson selection: " + selection);
        return selected;
    }

    private static void validateLesson(GuideBook.Lesson lesson) throws ReflectiveOperationException {
        requireItem(lesson.icon());
        requireSceneItem(lesson.icon(), lesson.id());
        for (String component : lesson.components()) {
            requireItem(component);
            if (!PonderIndex.getSceneAccess().doScenesExistForId(GuideResources.parse(component)))
                throw new IllegalStateException("Lesson component has no registered Ponder scene: " + lesson.id() + "/" + component);
        }
        for (var node : lesson.nodes()) {
            ResourceLocation id = GuideResources.parse(node.block());
            var block = BuiltInRegistries.BLOCK.getOptional(id)
                    .orElseThrow(() -> new IllegalStateException("Unknown lesson block: " + lesson.id() + "/" + node.id() + "/" + id));
            var state = GuideScenes.state(node);
            if (state.getBlock() != block) throw new IllegalStateException("State resolves to a different block: " + lesson.id() + "/" + node.id());
        }
        for (var step : lesson.steps()) {
            if (step.flow() != null) requireItem(step.flow().item());
            for (var callout : step.callouts()) requireItem(callout.item());
        }

        PonderScene scene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(lesson.id()))).get(0);
        if (scene.getKeyframeCount() != lesson.steps().size())
            throw new IllegalStateException("Ponder keyframe count differs from lesson steps: " + lesson.id());
        if (scene.getBounds().maxX() != 8 || scene.getBounds().maxY() != 4 || scene.getBounds().maxZ() != 8)
            throw new IllegalStateException("Ponder scene does not use the 9 x 5 x 9 validation stage: " + lesson.id()
                    + " bounds=" + scene.getBounds());

        scene.seekToTime(scene.getTotalTime());
        checkBlocks(lesson, scene);
        var firstPass = snapshot(lesson, scene);
        scene.begin();
        var expectedNbt = initialNbt(lesson);
        Map<String, String> cableModelStart = Map.of();
        Map<String, String> cableModelDisconnected = Map.of();
        Map<String, String> cableModelRestored = Map.of();
        for (int stepIndex = 0; stepIndex < lesson.steps().size(); stepIndex++) {
            var step = lesson.steps().get(stepIndex);
            scene.seekToTime(scene.getKeyframeTime(stepIndex) + 20);
            for (var update : step.nbt().entrySet()) {
                expectedNbt.computeIfAbsent(update.getKey(), ignored -> new CompoundTag())
                        .merge(GuideScenes.parseNbt(update.getValue()));
            }
            checkBlocks(lesson, scene);
            for (var update : expectedNbt.entrySet()) {
                var entity = scene.getWorld().getBlockEntity(lesson.node(update.getKey()).position());
                if (entity == null) throw new IllegalStateException("NBT update node has no block entity: " + lesson.id() + "/" + update.getKey());
                var actual = ValidationPlatform.saveWithoutMetadata(entity, scene.getWorld().registryAccess());
                if (!containsExpected(actual, update.getValue()))
                    throw new IllegalStateException("Visual NBT did not apply at keyframe: " + lesson.id() + "/" + stepIndex + "/" + update.getKey()
                            + " expected=" + update.getValue() + " actual=" + actual);
            }
            if (lesson.id().equals("cables") && List.of(0, 2, 3).contains(stepIndex)) {
                Map<String, String> current = new HashMap<>();
                for (String nodeId : List.of("a", "b")) current.put(nodeId, modelSignature(lesson, scene, lesson.node(nodeId)));
                if (stepIndex == 0) cableModelStart = Map.copyOf(current);
                if (stepIndex == 2) cableModelDisconnected = Map.copyOf(current);
                if (stepIndex == 3) cableModelRestored = Map.copyOf(current);
            }
            verifiedCallouts += checkCallouts(lesson, stepIndex, scene);
        }

        scene.seekToTime(scene.getTotalTime());
        checkBlocks(lesson, scene);
        var replayExpected = snapshot(lesson, scene);
        scene.begin();
        scene.seekToTime(scene.getTotalTime());
        checkBlocks(lesson, scene);
        if (!replayExpected.equals(snapshot(lesson, scene)) || !firstPass.equals(replayExpected))
            throw new IllegalStateException("Scene final model changed after begin/replay: " + lesson.id());
        replayVerified = true;
        if (lesson.id().equals("cables")) {
            for (String nodeId : List.of("a", "b")) {
                String initial = cableModelStart.get(nodeId);
                String disconnected = cableModelDisconnected.get(nodeId);
                String restored = cableModelRestored.get(nodeId);
                if (initial == null || disconnected == null || restored == null || initial.equals(disconnected)
                        || !initial.equals(restored))
                    throw new IllegalStateException("Transmitter NBT did not refresh its rendered connection model for " + nodeId
                            + ": initial=" + initial + ", disconnected=" + disconnected + ", restored=" + restored);
            }
            transmitterModelDataVerified = true;
        }
        COMPILED.add(new CompiledLesson(lesson, scene));
        LOGGER.info("MEKANISM_PONDER_SCENE_PASS {} ({} nodes, {} steps, {} callouts)", lesson.id(),
                lesson.nodes().size(), lesson.steps().size(), lesson.steps().stream().mapToInt(step -> step.callouts().size()).sum());
    }

    private static Map<String, CompoundTag> initialNbt(GuideBook.Lesson lesson) {
        var values = new HashMap<String, CompoundTag>();
        for (var node : lesson.nodes()) {
            if (!node.nbt().equals("{}")) values.put(node.id(), GuideScenes.parseNbt(node.nbt()));
        }
        return values;
    }

    private static boolean containsExpected(CompoundTag actual, CompoundTag expected) {
        for (String key : expected.getAllKeys()) {
            if (!actual.contains(key)) return false;
            Tag actualValue = actual.get(key);
            Tag expectedValue = expected.get(key);
            if (expectedValue instanceof CompoundTag expectedCompound) {
                if (!(actualValue instanceof CompoundTag actualCompound) || !containsExpected(actualCompound, expectedCompound)) return false;
            } else if (!expectedValue.equals(actualValue)) return false;
        }
        return true;
    }

    private static void checkBlocks(GuideBook.Lesson lesson, PonderScene scene) {
        for (var node : lesson.nodes()) {
            BlockState actual = scene.getWorld().getBlockState(node.position());
            if (!actual.is(BuiltInRegistries.BLOCK.get(GuideResources.parse(node.block()))))
                throw new IllegalStateException("Lesson block is missing at its declared position: " + lesson.id() + "/" + node.id());
            boolean expectsBlockEntity = !node.nbt().equals("{}") || lesson.steps().stream().anyMatch(step -> step.nbt().containsKey(node.id()));
            if (expectsBlockEntity && scene.getWorld().getBlockEntity(node.position()) == null)
                throw new IllegalStateException("Lesson NBT target has no block entity: " + lesson.id() + "/" + node.id());
        }
    }

    private static Map<String, SceneSnapshot> snapshot(GuideBook.Lesson lesson, PonderScene scene) {
        var states = new HashMap<String, SceneSnapshot>();
        for (var node : lesson.nodes()) {
            var state = scene.getWorld().getBlockState(node.position());
            var entity = scene.getWorld().getBlockEntity(node.position());
            CompoundTag nbt = entity == null ? null : ValidationPlatform.saveWithoutMetadata(entity, scene.getWorld().registryAccess());
            states.put(node.id(), new SceneSnapshot(state, nbt == null ? null : nbt.copy()));
        }
        return Map.copyOf(states);
    }

    private static int checkCallouts(GuideBook.Lesson lesson, int stepIndex, PonderScene scene) throws ReflectiveOperationException {
        var callouts = lesson.steps().get(stepIndex).callouts();
        if (callouts.isEmpty()) return 0;
        Field itemField = InputWindowElement.class.getDeclaredField("item");
        itemField.setAccessible(true);
        int slotDuration = GuideScenes.captionDuration(lesson, stepIndex) / callouts.size();
        for (int slot = 0; slot < callouts.size(); slot++) {
            int time = scene.getKeyframeTime(stepIndex) + 12 + slot * slotDuration + 15;
            scene.seekToTime(time);
            var visible = scene.getElements().stream().filter(InputWindowElement.class::isInstance)
                    .map(InputWindowElement.class::cast).filter(InputWindowElement::isVisible).toList();
            if (visible.size() != 1) throw new IllegalStateException("Expected one visible item callout: " + lesson.id() + "/" + stepIndex + "/" + slot);
            var element = visible.get(0);
            var item = (ItemStack) itemField.get(element);
            String actualItem = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
            if (element.getFade(1) < .99f || !actualItem.equals(callouts.get(slot).item()))
                throw new IllegalStateException("Callout is faded or has the wrong item: " + lesson.id() + "/" + stepIndex + "/" + slot
                        + " expected=" + callouts.get(slot).item() + " actual=" + actualItem);
        }
        return callouts.size();
    }

    private static String modelSignature(GuideBook.Lesson lesson, PonderScene scene, GuideBook.Node node) {
        BlockState state = scene.getWorld().getBlockState(node.position());
        var renderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = renderer.getBlockModel(state);
        ModelData data = ValidationPlatform.getModelData(scene.getWorld(), node.position());
        int count = 0;
        int hash = 1;
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            var random = RandomSource.create(1L);
            var general = model.getQuads(state, null, random, data, layer);
            count += general.size();
            for (BakedQuad quad : general) hash = 31 * hash + quadHash(quad);
            for (Direction direction : Direction.values()) {
                random = RandomSource.create(1L);
                var quads = model.getQuads(state, direction, random, data, layer);
                count += quads.size();
                for (BakedQuad quad : quads) hash = 31 * hash + quadHash(quad);
            }
        }
        if (count == 0) throw new IllegalStateException("Transmitter rendered no quads: " + lesson.id() + "/" + node.id());
        return count + ":" + hash;
    }

    private static int quadHash(BakedQuad quad) {
        int result = Arrays.hashCode(quad.getVertices());
        result = 31 * result + quad.getTintIndex();
        result = 31 * result + (quad.getDirection() == null ? 0 : quad.getDirection().ordinal() + 1);
        return result;
    }

    private static void requireSceneItem(String id, String lesson) {
        if (!PonderIndex.getSceneAccess().doScenesExistForId(GuideResources.parse(id)))
            throw new IllegalStateException("Lesson icon has no registered Ponder scene: " + lesson + "/" + id);
    }

    private static void requireItem(String id) {
        ResourceLocation key = GuideResources.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalStateException("Unknown lesson item: " + id);
    }

    private static void verifyDescriptionResources() {
        for (var description : GuideItemDescriptions.descriptions()) {
            for (String id : description.items()) requireItem(id);
            if (description.text().ja().isBlank() || description.text().en().isBlank())
                throw new IllegalStateException("Description is missing Japanese or English: " + description.id());
        }
    }

    private static void validateUiLanguage(String locale) {
        var client = Minecraft.getInstance();
        String expectedLanguage = locale.equals("ja_jp") ? "ja" : "en";
        if (!client.getLanguageManager().getSelected().equals(locale))
            throw new IllegalStateException("UI test ran under the wrong locale: " + client.getLanguageManager().getSelected());
        if (GuideBook.lessons().isEmpty()) throw new IllegalStateException("No runtime Mekanism lessons are available");

        GuideBook.Lesson sample = GuideBook.lessons().get(0);
        String expectedTitle = locale.equals("ja_jp") ? sample.title().ja() : sample.title().en();
        String expectedSummary = locale.equals("ja_jp") ? sample.summary().ja() : sample.summary().en();
        if (!Component.translatable("mekanismponder.ponder." + sample.id() + ".header").getString().equals(expectedTitle))
            throw new IllegalStateException("Localized lesson title does not match the " + expectedLanguage + " data: " + sample.id());
        if (!Component.translatable("mekanismponder.lesson." + sample.id() + ".summary").getString().equals(expectedSummary))
            throw new IllegalStateException("Localized lesson summary does not match the " + expectedLanguage + " data: " + sample.id());
        String chapterTitle = GuideChapter.fromId(sample.chapter()).label().getString();
        if (chapterTitle.isBlank() || chapterTitle.equals("mekanismponder.ponder.tag." + sample.chapter()))
            throw new IllegalStateException("Chapter title is not localized for " + expectedLanguage + ": " + sample.chapter());
        if (!coverageMode) {
            var localizedDescription = GuideItemDescriptions.descriptions().get(0);
            String expectedDescription = locale.equals("ja_jp") ? localizedDescription.text().ja() : localizedDescription.text().en();
            if (!Component.translatable(localizedDescription.translationKey()).getString().equals(expectedDescription))
                throw new IllegalStateException("Localized item description does not match the " + expectedLanguage + " data: " + localizedDescription.id());
            var describedItem = BuiltInRegistries.ITEM.get(GuideResources.parse(localizedDescription.items().get(0)));
            var originalName = Component.literal("Existing item name");
            var existingLine = Component.literal("Existing tooltip line");
            var tooltip = new ArrayList<Component>(List.of(originalName, existingLine));
            GuideItemDescriptions.append(new ItemStack(describedItem), tooltip, true);
            if (tooltip.get(0) != originalName || tooltip.get(tooltip.size() - 1) != existingLine)
                throw new IllegalStateException("Expanded item description removed existing tooltip content");
            int width = Math.max(1, Math.min(240, client.getWindow().getGuiScaledWidth() - 32));
            var descriptionLines = tooltip.subList(1, tooltip.size() - 1);
            String joined = descriptionLines.stream().map(Component::getString).reduce("", String::concat).replaceAll("\\s", "");
            if (!joined.equals(expectedDescription.replaceAll("\\s", ""))
                    || descriptionLines.stream().anyMatch(line -> client.font.width(line.getVisualOrderText()) > width))
                throw new IllegalStateException("Expanded item description lost or overflowed localized text: " + localizedDescription.id());
            var collapsed = new ArrayList<Component>(List.of(originalName, existingLine));
            GuideItemDescriptions.append(new ItemStack(describedItem), collapsed, false);
            if (collapsed.size() != 3 || collapsed.get(0) != originalName || collapsed.get(2) != existingLine)
                throw new IllegalStateException("Collapsed item description did not preserve existing tooltip");
            if (collapsed.get(1).getStyle().getColor() == null
                    || collapsed.get(1).getStyle().getColor().getValue() != GuidePresentation.Intent.HINT.textColor())
                throw new IllegalStateException("Collapsed description hint has the wrong native text color");
            verifyTooltipPalettes(width);
        }
    }

    private static void verifyTooltipPalettes(int width) {
        var client = Minecraft.getInstance();
        for (var description : GuideItemDescriptions.descriptions()) {
            String text = Component.translatable(description.translationKey()).getString();
            var expectedColors = new ArrayList<Integer>();
            for (var unit : GuidePresentation.units(description.translationKey(), text))
                for (String ignored : GuideTextLayout.wrap(client.font, unit.text(), width))
                    expectedColors.add(unit.intent().textColor());
            var tooltip = new ArrayList<Component>(List.of(Component.literal("Existing item name")));
            var item = BuiltInRegistries.ITEM.get(GuideResources.parse(description.items().get(0)));
            GuideItemDescriptions.append(new ItemStack(item), tooltip, true);
            var added = tooltip.subList(1, tooltip.size());
            if (added.size() != expectedColors.size())
                throw new IllegalStateException("Tooltip intention line count differs: " + description.id());
            for (int index = 0; index < added.size(); index++) {
                var color = added.get(index).getStyle().getColor();
                if (color == null || color.getValue() != expectedColors.get(index))
                    throw new IllegalStateException("Tooltip native text color differs: " + description.id() + "/" + index);
            }
        }
        tooltipPalettesVerified = true;
    }

    private static void runUiAndRendering(Minecraft client) throws Exception {
        if (uiStage == 1) {
            if (!ValidationPlatform.isOptionsScreen(client.screen)) throw new IllegalStateException("Options screen was not opened");
            verifyNoOrdinaryGuideButtons(client.screen);
            ordinaryGuideButtonsAbsent = true;
            screenshot("options-no-guide-button.png");
            dispatchCommand("ponder-mekanism");
            uiStage = 2;
            return;
        }
        if (uiStage == 2) {
            if (!(client.screen instanceof PonderTagIndexScreen index))
                throw new IllegalStateException("Root command did not open native PonderTagIndexScreen: " + screenName(client));
            nativeIndexVerified = true;
            screenshot("tag-index-ja.png");
            clickTag(index, GuideResources.of("mekanismponder", "start"));
            uiStage = 3;
            return;
        }
        if (uiStage == 3) {
            if (!(client.screen instanceof PonderTagScreen tagScreen)
                    || !tagScreen.getTag().getId().equals(GuideResources.of("mekanismponder", "start")))
                throw new IllegalStateException("Native tag click did not open the start chapter: " + screenName(client));
            if (++nativeScreenFrames < 24) return;
            nativeScreenFrames = 0;
            screenshot("tag-start-ja.png");
            GuideBook.Lesson selected = laterStartLesson();
            clickItem(tagScreen, GuideResources.parse(selected.icon()));
            uiStage = 4;
            ponderWaitFrames = 0;
            return;
        }
        if (uiStage == 4) {
            GuideBook.Lesson selected = laterStartLesson();
            var ponder = requirePonderLesson(selected);
            if (ponderWaitFrames == 0) {
                verifyCourseOrder(ponder);
                ResourceLocation iconId = GuideResources.parse(selected.icon());
                if (!BuiltInRegistries.ITEM.getKey(ponder.getSubject().getItem()).equals(iconId))
                    throw new IllegalStateException("Native item click did not preserve the selected subject icon");
                PonderUI fromWKey = PonderUI.of(new ItemStack(BuiltInRegistries.ITEM.get(iconId)));
                boolean allGuideScenes = !scenes(fromWKey).isEmpty()
                        && scenes(fromWKey).stream().allMatch(scene -> scene.getId().getNamespace().equals("mekanismponder"));
                if (allGuideScenes && (!(fromWKey instanceof GuidePonderScreen wKeyGuide)
                        || !wKeyGuide.getActiveScene().getId().getPath().equals(selected.id())))
                    throw new IllegalStateException("Ponder's one-argument item factory did not enter the matching guide lesson");
                if (!allGuideScenes && fromWKey instanceof GuidePonderScreen)
                    throw new IllegalStateException("The guide adapter claimed a mixed Mekanism/foreign scene list");
                verifyForeignItemFactoryBoundary();
                var sameSubjectOtherScene = new GuidePonderScreen(GuideBook.lessons().get(0), ponder.getSubject());
                if (ponder.isEquivalentTo(sameSubjectOtherScene) || !ponder.isEquivalentTo(ponder))
                    throw new IllegalStateException("Guide course equivalence ignored the active scene when sharing an item");
            }
            ponder.seekToTime(Math.min(70, Math.max(0, ponder.getActiveScene().getTotalTime() - 1)));
            if (++ponderWaitFrames == 24) screenshot("course-selected-lesson.png");
            if (ponderWaitFrames >= 24) {
                clickArrow(ponder, true);
                uiStage = 5;
            }
            return;
        }
        if (uiStage == 5) {
            requirePonderLesson(GuideBook.lessons().get(GuideBook.lessons().indexOf(laterStartLesson()) + 1));
            var ponder = (PonderUI) client.screen;
            if (++ponderWaitFrames >= 26) {
                clickArrow(ponder, false);
                uiStage = 6;
            }
            return;
        }
        if (uiStage == 6) {
            var ponder = requirePonderLesson(laterStartLesson());
            clickBack(ponder);
            uiStage = 7;
            return;
        }
        if (uiStage == 7) {
            if (!(client.screen instanceof PonderTagScreen tagScreen)
                    || !tagScreen.getTag().getId().equals(GuideResources.of("mekanismponder", "start")))
                throw new IllegalStateException("Native Ponder back did not restore the selected tag screen: " + screenName(client));
            clickBack(tagScreen);
            uiStage = 8;
            return;
        }
        if (uiStage == 8) {
            if (!(client.screen instanceof PonderTagIndexScreen index))
                throw new IllegalStateException("Native tag back did not restore PonderTagIndexScreen: " + screenName(client));
            clickTag(index, GuideResources.of("mekanismponder", "transport"));
            uiStage = 9;
            return;
        }
        if (uiStage == 9) {
            if (!(client.screen instanceof PonderTagScreen tagScreen)
                    || !tagScreen.getTag().getId().equals(GuideResources.of("mekanismponder", "transport")))
                throw new IllegalStateException("Native transport tag click did not open its tag screen: " + screenName(client));
            if (++nativeScreenFrames < 24) return;
            nativeScreenFrames = 0;
            screenshot("tag-transport-ja.png");
            verifyTagPagination(tagScreen);
            clickItem(tagScreen, GuideResources.parse(firstLessonIn("transport").icon()));
            uiStage = 10;
            return;
        }
        if (uiStage == 10) {
            var transportLesson = firstLessonIn("transport");
            var ponder = requirePonderLesson(transportLesson);
            verifyCourseOrder(ponder);
            clickBack(ponder);
            uiStage = 11;
            return;
        }
        if (uiStage == 11) {
            if (!(client.screen instanceof PonderTagScreen tagScreen)
                    || !tagScreen.getTag().getId().equals(GuideResources.of("mekanismponder", "transport")))
                throw new IllegalStateException("Ponder back did not restore the transport tag screen: " + screenName(client));
            clickBack(tagScreen);
            uiStage = 12;
            return;
        }
        if (uiStage == 12) {
            if (!(client.screen instanceof PonderTagIndexScreen))
                throw new IllegalStateException("Native tag back did not restore PonderTagIndexScreen: " + screenName(client));
            clickBack(client.screen);
            uiStage = 13;
            return;
        }
        if (uiStage == 13) {
            if (!ValidationPlatform.isOptionsScreen(client.screen))
                throw new IllegalStateException("Native tag index back did not restore Options: " + screenName(client));
            uiNavigationVerified = true;
            dispatchCommand("mekanismponder");
            uiStage = 14;
            return;
        }
        if (uiStage == 14) {
            requireScreen(PonderTagIndexScreen.class, "Legacy root command");
            openOptions(client);
            dispatchCommand("ponder-mekanism");
            uiStage = 15;
            return;
        }
        if (uiStage == 15) {
            requireScreen(PonderTagIndexScreen.class, "New root command");
            openOptions(client);
            dispatchCommand("ponder-mekanism 1");
            uiStage = 16;
            return;
        }
        if (uiStage == 16) {
            requirePonderLesson(GuideBook.lessons().get(0));
            openOptions(client);
            dispatchCommand("ponder-mekanism advanced");
            uiStage = 17;
            return;
        }
        if (uiStage == 17) {
            requirePonderLesson(firstLessonIn("advanced"));
            openOptions(client);
            dispatchCommand("ponder-mekanism " + GuideChapter.START.label().getString());
            uiStage = 18;
            return;
        }
        if (uiStage == 18) {
            requirePonderLesson(GuideBook.lessons().get(0));
            openOptions(client);
            dispatchCommand("ponder-mekanism item energy_cube");
            uiStage = 19;
            return;
        }
        if (uiStage == 19) {
            requirePonderLesson(GuideBook.lessons().get(0));
            openOptions(client);
            dispatchCommand("mekanismponder lesson steel");
            uiStage = 20;
            return;
        }
        if (uiStage == 20) {
            requirePonderLesson(lessonById("steel"));
            openOptions(client);
            openComponentFactory();
            ponderWaitFrames = 0;
            uiStage = 27;
            return;
        }
        if (uiStage == 21) {
            validateUiLanguage("en_us");
            verifyCrossLocaleHistory();
            dispatchCommand("ponder-mekanism " + GuideChapter.START.label().getString());
            uiStage = 22;
            return;
        }
        if (uiStage == 22) {
            requirePonderLesson(GuideBook.lessons().get(0));
            openOptions(client);
            dispatchCommand("ponder-mekanism");
            uiStage = 23;
            return;
        }
        if (uiStage == 23) {
            if (!(client.screen instanceof PonderTagIndexScreen))
                throw new IllegalStateException("English command did not open native Ponder index: " + screenName(client));
            if (!englishIndexRendered) {
                screenshot("tag-index-en.png");
                englishIndexRendered = true;
                if (coverageMode) {
                    var locales = ItemCoverageValidation.locales();
                    if (locales.isEmpty()) throw new IllegalStateException("Coverage locale roster is empty");
                    coverageLocaleIndex = 0;
                    beginLanguageReload(locales.get(coverageLocaleIndex));
                    uiStage = 42;
                    return;
                }
                if (representativeGui) {
                    beginLanguageReload("ar_sa");
                    uiStage = 40;
                    return;
                }
                if (navigationOnly) compileNavigationSamples();
                renderLessonIndex = 0;
                renderPhase = 0;
                uiStage = 24;
                return;
            }
        }
        if (uiStage == 24) {
            renderEveryScene(client);
            return;
        }
        if (uiStage == 25) validateAllLocales(client);
        if (uiStage == 26) writeSuccessAndStop(client);
        if (uiStage == 42) validateCoverageLocales(client);
        if (uiStage >= 27) runActiveCategoryNavigation(client);
    }

    private static void runActiveCategoryNavigation(Minecraft client) throws Exception {
        if (uiStage == 27) {
            var ponder = requirePonderLesson(componentLesson, componentSubject);
            if (++ponderWaitFrames < 24) return;
            componentFactoryVerified = true;
            screenshot("component-w-subject.png");
            clickPlaybackTag(ponder, componentLesson.chapter());
            uiStage = 28;
            return;
        }
        if (uiStage == 28) {
            var tag = requireCategory(componentLesson.chapter());
            verifyCategoryItems(tag);
            screenshot("component-native-category.png");
            clickItem(tag, GuideResources.parse(componentPeerLesson().icon()));
            ponderWaitFrames = 0;
            uiStage = 37;
            return;
        }
        if (uiStage == 29) {
            var ponder = requirePonderLesson(componentLesson, componentSubject);
            playbackTagVerified = true;
            if (++ponderWaitFrames < 26) return;
            clickArrow(ponder, true);
            ponderWaitFrames = 0;
            uiStage = 30;
            return;
        }
        if (uiStage == 30) {
            var next = GuideBook.lessons().get(GuideBook.lessons().indexOf(componentLesson) + 1);
            var ponder = requirePonderLesson(next);
            if (++ponderWaitFrames < 26) return;
            clickArrow(ponder, false);
            uiStage = 31;
            return;
        }
        if (uiStage == 31) {
            // Returning after a lesson change uses that scene's icon, not the original W subject.
            requirePonderLesson(componentLesson);
            openOptions(client);
            GuidePonderScreen.open(chapterBoundaryBefore());
            ponderWaitFrames = 0;
            uiStage = 32;
            return;
        }
        if (uiStage == 32) {
            var ponder = requirePonderLesson(chapterBoundaryBefore());
            if (++ponderWaitFrames < 24) return;
            clickArrow(ponder, true);
            ponderWaitFrames = 0;
            uiStage = 33;
            return;
        }
        if (uiStage == 33) {
            var ponder = requirePonderLesson(chapterBoundaryAfter());
            if (++ponderWaitFrames < 26) return;
            screenshot("course-next-chapter.png");
            clickPlaybackTag(ponder, chapterBoundaryAfter().chapter());
            uiStage = 34;
            return;
        }
        if (uiStage == 34) {
            var tag = requireCategory(chapterBoundaryAfter().chapter());
            verifyCategoryItems(tag);
            screenshot("course-next-chapter-category.png");
            clickBack(tag);
            uiStage = 35;
            return;
        }
        if (uiStage == 35) {
            var ponder = requirePonderLesson(chapterBoundaryAfter());
            clickArrow(ponder, false);
            uiStage = 36;
            return;
        }
        if (uiStage == 36) {
            requirePonderLesson(chapterBoundaryBefore());
            chapterBoundaryVerified = true;
            uiStage = 39;
            return;
        }
        if (uiStage == 37) {
            var ponder = requirePonderLesson(componentPeerLesson());
            if (++ponderWaitFrames < 24) return;
            screenshot("component-category-other-lesson.png");
            clickBack(ponder);
            uiStage = 38;
            return;
        }
        if (uiStage == 38) {
            var tag = requireCategory(componentLesson.chapter());
            clickBack(tag);
            ponderWaitFrames = 0;
            uiStage = 29;
            return;
        }
        if (uiStage == 39) {
            if (coverageMode) {
                beginLanguageReload("en_us");
                uiStage = 21;
            } else {
                runPaletteSamples(client);
            }
        }
        if (uiStage == 40) {
            runRepresentativeArabic(client);
            return;
        }
        if (uiStage == 41) {
            verifyEnglishFallback();
            uiStage = 26;
        }
    }

    private static void validateCoverageLocales(Minecraft client) throws Exception {
        if (!validationCaptionIds.isEmpty() && !captionWindowWidthsVerified) verifyCaptionWindowWidths();
        List<String> locales = ItemCoverageValidation.locales();
        if (coverageLocaleIndex >= locales.size()) {
            writeCaptionLayoutReport(client);
            var reportPath = client.gameDirectory.toPath().resolve("item-coverage-report.json");
            boolean passed = (boolean) ItemCoverageValidation.successSummary().get("passed")
                    && (validationCaptionIds.isEmpty()
                    || CAPTION_LAYOUT_ISSUES.isEmpty() && coverageCaptionMeasurementsComplete());
            ItemCoverageValidation.writeReport(reportPath, passed);
            if (!passed)
                throw new IllegalStateException("Item coverage recorded "
                        + ((List<?>) ItemCoverageValidation.successSummary().get("issues")).size()
                        + " item issue(s) and " + CAPTION_LAYOUT_ISSUES.size()
                        + " caption issue(s); see item-coverage-report.json and caption-overflow.json");
            uiStage = 26;
            return;
        }
        String locale = locales.get(coverageLocaleIndex);
        ItemCoverageValidation.validateLocale(client, locale);
        if (!validationCaptionIds.isEmpty()) checkLocaleText(locale, false);
        VERIFIED_LOCALES.add(locale);
        LOGGER.info("MEKANISM_PONDER_ITEM_COVERAGE_LOCALE_PASS {}", locale);
        coverageLocaleIndex++;
        if (coverageLocaleIndex < locales.size()) beginLanguageReload(locales.get(coverageLocaleIndex));
    }

    private static void runRepresentativeArabic(Minecraft client) throws Exception {
        if (representativeArabicPhase == 0) {
            verifyCaptionWindowWidths();
            checkLocaleText("ar_sa");
            writeCaptionLayoutReport(client);
            if (!CAPTION_LAYOUT_ISSUES.isEmpty())
                throw new IllegalStateException("Arabic font recheck found " + CAPTION_LAYOUT_ISSUES.size()
                        + " caption issue(s); see caption-overflow.json");
            representativeArabicPhase = 1;
            return;
        }
        var sample = GuideBook.lessons().get(0);
        if (representativeArabicPhase == 1) {
            requirePonderLesson(sample);
            var scene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(sample.id()))).get(0);
            client.setScreen(new ValidationPonderScreen(scene));
            localeFrames = 0;
            representativeArabicPhase = 2;
            return;
        }
        var screen = (PonderUI) client.screen;
        String text = Component.translatable("mekanismponder.ponder.shared." + sample.id() + "_0").getString();
        int units = GuideTextLayout.captionPages(text).size();
        screen.seekToTime(27 + GuideScenes.captionDuration(sample, 0) / units * Math.min(1, units - 1) + 30);
        if (++localeFrames < 4) return;
        screenshot("caption-ar_sa-representative.png");
        VERIFIED_LOCALES.add("ar_sa");
        arabicFontRechecked = true;
        writeCaptionLayoutReport(client);
        beginLanguageReload("xx_zz");
        uiStage = 41;
    }

    private static void runPaletteSamples(Minecraft client) throws Exception {
        if (paletteSampleIndex >= PALETTE_SAMPLES.size()) {
            captionPalettesVerified = true;
            beginLanguageReload("en_us");
            uiStage = 21;
            return;
        }
        var lesson = lessonById("power");
        if (paletteScene == null)
            paletteScene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(lesson.id()))).get(0);
        if (!(client.screen instanceof ValidationPonderScreen screen) || screen.getActiveScene() != paletteScene) {
            client.setScreen(new ValidationPonderScreen(paletteScene));
            return;
        }
        var sample = PALETTE_SAMPLES.get(paletteSampleIndex);
        String key = "mekanismponder.ponder.shared." + lesson.id() + "_" + sample.step();
        var units = GuidePresentation.units(key, Component.translatable(key).getString());
        if (units.size() <= sample.unit() || units.get(sample.unit()).intent() != sample.intent())
            throw new IllegalStateException("Representative caption intention differs: " + key + "/" + sample.unit());
        if (palettePhase == 0) {
            paletteScene.begin();
            int pageDuration = GuideScenes.captionDuration(lesson, sample.step()) / units.size();
            screen.seekToTime(paletteScene.getKeyframeTime(sample.step()) + 12 + sample.unit() * pageDuration + 20);
            verifyVisiblePalette(paletteScene, units.get(sample.unit()));
            palettePhase = 1;
            ponderWaitFrames = 0;
            return;
        }
        if (++ponderWaitFrames < 4) return;
        verifyVisiblePalette(paletteScene, units.get(sample.unit()));
        screenshot("caption-palette-" + sample.intent().name().toLowerCase(java.util.Locale.ROOT) + ".png");
        paletteSampleIndex++;
        palettePhase = 0;
    }

    private static void verifyVisiblePalette(PonderScene scene, GuidePresentation.Unit expected) throws Exception {
        var windows = new ArrayList<TextWindowElement>();
        scene.forEachVisible(TextWindowElement.class, windows::add);
        if (windows.size() != 1 || windows.get(0).getPalette() != expected.intent().palette())
            throw new IllegalStateException("Native caption element has the wrong palette: expected=" + expected.intent()
                    + ", actual=" + windows.stream().map(TextWindowElement::getPalette).toList());
        var getter = TextWindowElement.class.getDeclaredField("textGetter");
        getter.setAccessible(true);
        var actualText = ((java.util.function.Supplier<?>) getter.get(windows.get(0))).get();
        String display = GuideTextLayout.captionDisplayText(Minecraft.getInstance().font, expected.text());
        if (!display.equals(actualText))
            throw new IllegalStateException("Native colored caption differs from the authored display unit");
    }

    private static void validateAllLocales(Minecraft client) throws Exception {
        if (validationLocales == null) {
            try (var stream = GuideValidation.class.getResourceAsStream("/assets/mekanismponder/guide/languages.json")) {
                if (stream == null) throw new IllegalStateException("Missing locale roster");
                validationLocales = List.copyOf(JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                        .getAsJsonObject().keySet());
            }
            String requested = System.getProperty("mekanismponder.validationLocales", "").trim();
            if (!requested.isEmpty()) {
                var selected = new java.util.LinkedHashSet<>(List.of(requested.split(",")));
                if (!validationLocales.containsAll(selected))
                    throw new IllegalArgumentException("Unknown validation locale: " + requested);
                validationLocales = validationLocales.stream().filter(selected::contains).toList();
            }
            verifyCaptionWindowWidths();
        }
        if (VERIFIED_LOCALES.size() < validationLocales.size()) {
            String locale = validationLocales.get(VERIFIED_LOCALES.size());
            if (localePhase == 0) {
                beginLanguageReload(locale);
                localePhase = 1;
                return;
            }
            if (localePhase == 1) {
                checkLocaleText(locale);
                localePhase = 2;
                return;
            }
            if (localePhase == 2) {
                requirePonderLesson(GuideBook.lessons().get(0));
                var sample = GuideBook.lessons().get(0);
                PonderScene scene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(sample.id()))).get(0);
                client.setScreen(new ValidationPonderScreen(scene));
                localeFrames = 0;
                localePhase = 3;
                return;
            }
            var screen = (PonderUI) client.screen;
            var sample = GuideBook.lessons().get(0);
            // Render the revised connection/side-configuration narration in every locale.
            int sampleTime = screen.getActiveScene().getKeyframeTime(2) + 12 + 30;
            screen.seekToTime(sampleTime);
            if (++localeFrames < 4) return;
            if (localePhase == 3) {
                screenshot("caption-" + locale + ".png");
                screen.init(client, 320, 240);
                localeFrames = 0;
                localePhase = 4;
                return;
            }
            if (localePhase == 4) {
                screenshot("caption-" + locale + "-320.png");
                screen.init(client, 480, 320);
                localeFrames = 0;
                localePhase = 5;
                return;
            }
            screenshot("caption-" + locale + "-480.png");
            screen.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            VERIFIED_LOCALES.add(locale);
            LOGGER.info("MEKANISM_PONDER_LOCALE_PASS {}", locale);
            localePhase = 0;
            return;
        }
        if (localePhase == 0) {
            writeCaptionLayoutReport(client);
            if (!CAPTION_LAYOUT_ISSUES.isEmpty())
                throw new IllegalStateException("Caption layout has " + CAPTION_LAYOUT_ISSUES.size()
                        + " issue(s); see caption-overflow.json for every locale and GUI width");
            beginLanguageReload("xx_zz");
            localePhase = 4;
            return;
        }
        verifyEnglishFallback();
        uiStage = 26;
    }

    private static void verifyEnglishFallback() throws Exception {
        String key = "mekanismponder.ponder.shared.power_0";
        String fallback = Component.translatable(key).getString();
        if (!fallback.equals(GuideBook.lessons().get(0).steps().get(0).text().en()))
            throw new IllegalStateException("Unsupported locale did not fall back to English");
        try (var stream = GuideValidation.class.getResourceAsStream("/assets/mekanismponder/guide/presentation.json")) {
            if (stream == null) throw new IllegalStateException("Missing presentation fallback resource");
            var expected = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("locales").getAsJsonObject("en_us").getAsJsonArray(key);
            var expectedIntents = new ArrayList<String>();
            expected.forEach(value -> expectedIntents.add(value.getAsString()));
            var actual = GuidePresentation.units(key, fallback);
            if (!actual.stream().map(unit -> unit.intent().name()).toList().equals(expectedIntents)
                    || !String.join("\n", actual.stream().map(GuidePresentation.Unit::text).toList()).equals(fallback))
                throw new IllegalStateException("Unsupported locale did not fall back to English caption intentions");
        }
        englishFallbackVerified = true;
        presentationFallbackVerified = true;
    }

    private static void checkLocaleText(String locale) throws Exception {
        checkLocaleText(locale, true);
    }

    private static void checkLocaleText(String locale, boolean verifyNavigation) throws Exception {
        var client = Minecraft.getInstance();
        try (var stream = GuideValidation.class.getResourceAsStream("/assets/mekanismponder/lang/" + locale + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing language resource: " + locale);
            var expected = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> englishKeys;
            try (var englishStream = GuideValidation.class.getResourceAsStream("/assets/mekanismponder/lang/en_us.json")) {
                if (englishStream == null) throw new IllegalStateException("Missing English language resource");
                englishKeys = new HashSet<>(JsonParser.parseString(new String(englishStream.readAllBytes(), StandardCharsets.UTF_8))
                        .getAsJsonObject().keySet());
            }
            Set<String> actualKeys = new HashSet<>(expected.keySet());
            if (!actualKeys.equals(englishKeys)) {
                Set<String> missing = new HashSet<>(englishKeys);
                missing.removeAll(actualKeys);
                Set<String> extra = new HashSet<>(actualKeys);
                extra.removeAll(englishKeys);
                throw new IllegalStateException("Language resource key set differs from en_us: " + locale
                        + "; missing=" + missing + "; extra=" + extra);
            }
            for (var entry : expected.entrySet()) {
                String expectedText = entry.getValue().getAsString().replace("%s", "test");
                if (!Component.translatable(entry.getKey(), "test").getString().equals(expectedText))
                    throw new IllegalStateException("Resource stack translation differs: " + locale + "/" + entry.getKey());
            }
            var getFontSet = Font.class.getDeclaredMethod("getFontSet", ResourceLocation.class);
            getFontSet.setAccessible(true);
            FontSet fontSet = (FontSet) getFontSet.invoke(client.font, Style.DEFAULT_FONT);
            var seen = new HashSet<Integer>();
            for (var entry : expected.entrySet()) {
                Component.literal(entry.getValue().getAsString()).getVisualOrderText().accept((index, style, codepoint) -> {
                    if (seen.add(codepoint) && !Character.isWhitespace(codepoint)
                            && Character.getType(codepoint) != Character.FORMAT) {
                        if (fontSet.getGlyphInfo(codepoint, false) == SpecialGlyphs.MISSING)
                            throw new IllegalStateException("Missing visual glyph U+" + Integer.toHexString(codepoint) + ": " + locale);
                        glyphsChecked++;
                    }
                    return true;
                });
            }
        }
        for (var lesson : GuideBook.lessons()) {
            for (int index = 0; index < lesson.steps().size(); index++) {
                String captionId = lesson.id() + "_" + index;
                if (coverageMode && !validationCaptionIds.contains(captionId)) continue;
                String raw = Component.translatable("mekanismponder.ponder.shared." + lesson.id() + "_" + index).getString();
                var pages = GuideTextLayout.captionPages(raw);
                if (!pages.equals(Arrays.asList(raw.split("\n", -1))))
                    recordCaptionBoundaryIssue(locale, lesson.id(), index, raw,
                            "Runtime caption units differ from authored newline boundaries");
                for (int unitIndex = 0; unitIndex < pages.size(); unitIndex++) {
                    String page = pages.get(unitIndex);
                    String displayText = GuideTextLayout.captionDisplayText(client.font, page);
                    captionUnitsChecked++;
                    if (!String.join("", displayText.split("\n", -1)).equals(page))
                        recordCaptionBoundaryIssue(locale, lesson.id(), index, page,
                                "Natural line reflow changed caption content or order");
                    if (page.isBlank())
                        recordCaptionIssue("empty-unit", locale, lesson.id(), index, unitIndex, -1, -1, 0, page);
                    for (int guiWidth : CAPTION_GUI_WIDTHS) {
                        float x = GuideTextLayout.constrainNearSceneX(guiWidth * 0.75f, guiWidth);
                        int width = GuideTextLayout.nativeCaptionWidth(guiWidth, x);
                        captionGuiWidthChecks++;
                        var wrappedLines = client.font.getSplitter().splitLines(displayText, width, Style.EMPTY);
                        int count = wrappedLines.size();
                        maximumCaptionLines = Math.max(maximumCaptionLines, count);
                        if (count > GuideTextLayout.MAX_PAGE_LINES)
                            recordCaptionIssue("line-count", locale, lesson.id(), index, unitIndex, guiWidth, width, count, page);
                        var glyphWidths = new ArrayList<Integer>();
                        for (var line : wrappedLines) {
                            int glyphWidth = Math.max(client.font.width(line),
                                    client.font.width(Language.getInstance().getVisualOrder(line)));
                            glyphWidths.add(glyphWidth);
                            if (glyphWidth > width)
                                recordCaptionIssue("glyph-overflow", locale, lesson.id(), index, unitIndex,
                                        guiWidth, width, count, page);
                        }
                        var measurement = new HashMap<String, Object>();
                        measurement.put("locale", locale);
                        measurement.put("lesson", lesson.id());
                        measurement.put("step", index);
                        measurement.put("unit", unitIndex);
                        measurement.put("guiWidth", guiWidth);
                        measurement.put("captionWidth", width);
                        measurement.put("lineCount", count);
                        measurement.put("visualGlyphWidths", List.copyOf(glyphWidths));
                        measurement.put("text", page);
                        CAPTION_MEASUREMENTS.add(measurement);
                    }
                    captionPagesChecked++;
                }
            }
        }
        for (var description : GuideItemDescriptions.descriptions()) {
            String raw = Component.translatable(description.translationKey()).getString();
            for (int width : List.of(160, 240)) {
                var lines = GuideTextLayout.wrap(client.font, raw, width);
                if (!String.join("", lines).replaceAll("\\s", "").equals(raw.replaceAll("\\s", ""))
                        || lines.stream().anyMatch(line -> GuideTextLayout.measuredWidth(client.font, line) > width))
                    throw new IllegalStateException("Tooltip lost text or overflowed: " + locale + "/" + description.id());
            }
        }
        if (verifyNavigation) {
            for (String key : List.of("mekanismponder.ponder.power.header", "mekanismponder.lesson.power.summary", "mekanismponder.ponder.shared.power_0"))
                if (GuideCommands.findLesson(Component.translatable(key).getString()) == null)
                    throw new IllegalStateException("Localized title/summary/caption search failed: " + locale + "/" + key);
            openOptions(client);
            dispatchCommand("ponder-mekanism " + GuideChapter.START.label().getString());
        }
    }

    private static void recordCaptionBoundaryIssue(String locale, String lessonId, int stepIndex, String text, String message) {
        var issue = new HashMap<String, Object>();
        issue.put("type", "boundary");
        issue.put("locale", locale);
        issue.put("lesson", lessonId);
        issue.put("step", stepIndex);
        issue.put("message", message);
        issue.put("text", text);
        CAPTION_LAYOUT_ISSUES.add(issue);
    }

    private static void recordCaptionIssue(String type, String locale, String lessonId, int stepIndex,
                                           int unitIndex, int guiWidth, int captionWidth, int lineCount, String text) {
        var issue = new HashMap<String, Object>();
        issue.put("type", type);
        issue.put("locale", locale);
        issue.put("lesson", lessonId);
        issue.put("step", stepIndex);
        issue.put("unit", unitIndex);
        if (guiWidth >= 0) issue.put("guiWidth", guiWidth);
        if (captionWidth >= 0) issue.put("captionWidth", captionWidth);
        issue.put("lineCount", lineCount);
        issue.put("text", text);
        CAPTION_LAYOUT_ISSUES.add(issue);
    }

    private static GuideBook.Lesson laterStartLesson() {
        return GuideBook.lessons().stream().filter(lesson -> lesson.chapter().equals("start")).skip(1)
                .findFirst().orElseThrow(() -> new IllegalStateException("Start chapter needs at least two lessons for navigation validation"));
    }

    private static GuideBook.Lesson firstLessonIn(String chapter) {
        return GuideBook.lessons().stream().filter(lesson -> lesson.chapter().equals(chapter)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No lesson in chapter: " + chapter));
    }

    private static GuideBook.Lesson lessonById(String id) {
        return GuideBook.lessons().stream().filter(lesson -> lesson.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing validation lesson: " + id));
    }

    private static GuideBook.Lesson chapterBoundaryBefore() {
        var course = GuideBook.lessons();
        for (int index = 0; index < course.size() - 1; index++)
            if (!course.get(index).chapter().equals(course.get(index + 1).chapter())) return course.get(index);
        throw new IllegalStateException("No course chapter boundary is available for navigation validation");
    }

    private static GuideBook.Lesson componentPeerLesson() {
        return GuideBook.lessons().stream()
                .filter(lesson -> lesson.chapter().equals(componentLesson.chapter()) && !lesson.id().equals(componentLesson.id()))
                .filter(lesson -> lesson.equals(GuideBook.firstLessonForItem(GuideResources.parse(lesson.icon()), lesson.chapter())))
                .findFirst().orElseThrow(() -> new IllegalStateException("Component category has no second item lesson"));
    }

    private static GuideBook.Lesson chapterBoundaryAfter() {
        var course = GuideBook.lessons();
        return course.get(course.indexOf(chapterBoundaryBefore()) + 1);
    }

    private static void verifyNoOrdinaryGuideButtons(Screen screen) {
        long ownButtons = screen.children().stream().filter(Button.class::isInstance)
                .filter(child -> child.getClass().getName().startsWith("dev.mekanismponder."))
                .count();
        if (ownButtons != 0) throw new IllegalStateException("Ordinary screen contains " + ownButtons + " guide buttons");
    }

    private static void openComponentFactory() {
        for (var lesson : GuideBook.lessons()) {
            for (String component : lesson.components()) {
                if (component.equals(lesson.icon())) continue;
                var id = GuideResources.parse(component);
                var expected = GuideBook.firstLessonForItem(id, null);
                if (expected == null || GuideBook.lessons().indexOf(expected) == GuideBook.lessons().size() - 1) continue;
                var compiled = PonderIndex.getSceneAccess().compile(id);
                if (compiled.isEmpty() || compiled.stream().anyMatch(scene -> !scene.getId().getNamespace().equals("mekanismponder")))
                    continue;
                var stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                PonderUI screen = PonderUI.of(stack);
                if (!(screen instanceof GuidePonderScreen))
                    throw new IllegalStateException("Related W item did not enter the guide course: " + id);
                componentLesson = expected;
                componentSubject = id;
                var tags = ((PonderUIAccessor) (Object) screen).mekanismponder$getTags();
                var expectedTag = GuideResources.of("mekanismponder", expected.chapter());
                if (!tags.stream().map(net.createmod.ponder.foundation.PonderTag::getId).toList().contains(expectedTag))
                    throw new IllegalStateException("Related W item is missing its native category tag: " + id);
                var registeredTags = PonderIndex.getTagAccess().getTags(id);
                if (registeredTags.stream().noneMatch(tag -> tag.getId().equals(expectedTag)))
                    throw new IllegalStateException("Related W item has no registered category tag: " + id);
                ScreenOpener.open(screen);
                return;
            }
        }
        throw new IllegalStateException("No owned related W item is available for component navigation validation");
    }

    private static PonderTagScreen requireCategory(String chapter) {
        Screen screen = Minecraft.getInstance().screen;
        var expected = GuideResources.of("mekanismponder", chapter);
        if (!(screen instanceof PonderTagScreen category) || !category.getTag().getId().equals(expected))
            throw new IllegalStateException("Playback tag did not open its active category: " + expected);
        return category;
    }

    private static void verifyCategoryItems(PonderTagScreen screen) {
        String chapter = screen.getTag().getId().getPath();
        Set<ResourceLocation> expected = new HashSet<>();
        for (var lesson : GuideBook.lessons()) {
            if (!lesson.chapter().equals(chapter)) continue;
            expected.add(GuideResources.parse(lesson.icon()));
            lesson.components().stream().map(GuideResources::parse).forEach(expected::add);
        }
        var actual = PonderIndex.getTagAccess().getItems(screen.getTag());
        if (!actual.equals(expected))
            throw new IllegalStateException("Native category contains missing, unrelated or unavailable items: " + chapter
                    + ", expected=" + expected + ", actual=" + actual);
    }

    private static void clickPlaybackTag(PonderUI screen, String chapter) {
        var access = (PonderUIAccessor) (Object) screen;
        var expected = GuideResources.of("mekanismponder", chapter);
        for (int index = 0; index < access.mekanismponder$getTags().size(); index++) {
            if (!access.mekanismponder$getTags().get(index).getId().equals(expected)) continue;
            var button = access.mekanismponder$getTagButtons().get(index);
            screen.mouseClicked(button.getX() + 8, button.getY() + 8, 0);
            return;
        }
        throw new IllegalStateException("Playback is missing its native category button: " + expected);
    }

    private static void clickTag(PonderTagIndexScreen screen, ResourceLocation id) {
        for (var child : screen.children()) {
            if (child instanceof PonderButton button && button.getTag() != null && button.getTag().getId().equals(id)) {
                screen.mouseClicked(button.getX() + 8, button.getY() + 8, 0);
                return;
            }
        }
        throw new IllegalStateException("Native Ponder tag index is missing tag: " + id);
    }

    private static void clickItem(PonderTagScreen screen, ResourceLocation id) {
        for (var child : screen.children()) {
            if (child instanceof PonderButton button && button.visible && !button.getItem().isEmpty()
                    && BuiltInRegistries.ITEM.getKey(button.getItem().getItem()).equals(id)) {
                screen.mouseClicked(button.getX() + 8, button.getY() + 8, 0);
                return;
            }
        }
        throw new IllegalStateException("Native Ponder tag screen is missing item: " + id);
    }

    private static void verifyTagPagination(PonderTagScreen screen) throws Exception {
        var client = Minecraft.getInstance();
        int originalWidth = screen.width;
        int originalHeight = screen.height;
        for (int[] bounds : List.of(new int[]{320, 240}, new int[]{480, 320}, new int[]{960, 640})) {
            screen.init(client, bounds[0], bounds[1]);
            var areaField = PonderTagScreen.class.getDeclaredField("itemArea");
            areaField.setAccessible(true);
            Set<ResourceLocation> all = new HashSet<>();
            screen.children().stream().filter(PonderButton.class::isInstance).map(PonderButton.class::cast)
                    .filter(button -> !button.getItem().isEmpty())
                    .forEach(button -> all.add(BuiltInRegistries.ITEM.getKey(button.getItem().getItem())));
            var seen = new HashSet<ResourceLocation>();
            for (int page = 0; page <= all.size(); page++) {
                for (var child : screen.children()) {
                    if (!(child instanceof PonderButton button) || !button.visible || button.getItem().isEmpty()) continue;
                    if (button.getX() < 8 || button.getX() + button.getWidth() > screen.width - 8
                            || button.getY() < 0 || button.getY() + button.getHeight() > screen.height - 66)
                        throw new IllegalStateException("Native tag item escapes the available area at " + bounds[0] + "x" + bounds[1]);
                    if (!seen.add(BuiltInRegistries.ITEM.getKey(button.getItem().getItem())))
                        throw new IllegalStateException("Related item repeats across native tag pages");
                    var area = (net.minecraft.client.renderer.Rect2i) areaField.get(screen);
                    int descriptionY = screen.getItemsY() - 10 + Math.max(area.getHeight(), 48);
                    if (button.getY() + button.getHeight() > descriptionY - 6)
                        throw new IllegalStateException("Native tag description overlaps the last item row");
                }
                Button next = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.getMessage().getString().equals("›") && button.active).findFirst().orElse(null);
                if (next == null) break;
                // Native description occupies the central 45% below the item grid.
                double descriptionLeft = screen.width * .275;
                double descriptionRight = screen.width * .725;
                if (next.getX() < descriptionRight && next.getX() + next.getWidth() > descriptionLeft)
                    throw new IllegalStateException("Tag page control overlaps the native description");
                next.onPress();
            }
            if (!seen.equals(all)) throw new IllegalStateException("A related item cannot be reached through native tag pages");
        }
        screen.init(client, originalWidth, originalHeight);
        tagPaginationVerified = true;
    }

    private static void requireScreen(Class<? extends Screen> expected, String action) {
        if (!expected.isInstance(Minecraft.getInstance().screen))
            throw new IllegalStateException(action + " opened " + screenName(Minecraft.getInstance())
                    + " instead of " + expected.getSimpleName());
    }

    private static void dispatchCommand(String command) throws Exception {
        if (commandDispatcher == null) {
            commandDispatcher = new CommandDispatcher<>();
            GuideCommands.register(commandDispatcher);
        }
        var client = Minecraft.getInstance();
        commandDispatcher.execute(command, client.player.createCommandSourceStack());
    }

    private static void openOptions(Minecraft client) {
        client.setScreen(ValidationPlatform.createOptionsScreen(client));
    }

    private static void compileNavigationSamples() {
        for (String chapter : List.of("start", "transport", "processing")) {
            GuideBook.Lesson lesson = firstLessonIn(chapter);
            PonderScene scene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(lesson.id()))).get(0);
            COMPILED.add(new CompiledLesson(lesson, scene));
        }
    }

    private static void verifyCaptionWindowWidths() {
        for (int uiWidth : CAPTION_GUI_WIDTHS) {
            float proposedX = uiWidth * 0.75f;
            float constrainedX = GuideTextLayout.constrainNearSceneX(proposedX, uiWidth);
            int actualWidth = GuideTextLayout.nativeCaptionWidth(uiWidth, constrainedX);
            if (actualWidth != GuideTextLayout.CAPTION_WIDTH) {
                var issue = new HashMap<String, Object>();
                issue.put("type", "native-window-width");
                issue.put("guiWidth", uiWidth);
                issue.put("expectedWidth", GuideTextLayout.CAPTION_WIDTH);
                issue.put("actualWidth", actualWidth);
                CAPTION_LAYOUT_ISSUES.add(issue);
            }
        }
        captionWindowWidthsVerified = true;
    }

    private static void writeCaptionLayoutReport(Minecraft client) throws Exception {
        var report = new HashMap<String, Object>();
        List<String> measuredCaptionIds = CAPTION_MEASUREMENTS.stream()
                .map(measurement -> measurement.get("lesson") + "_" + measurement.get("step"))
                .distinct().sorted().toList();
        List<String> measuredLocales = CAPTION_MEASUREMENTS.stream().map(measurement -> (String) measurement.get("locale"))
                .distinct().sorted().toList();
        report.put("passed", CAPTION_LAYOUT_ISSUES.isEmpty() && coverageCaptionMeasurementsComplete());
        report.put("fontSelection", !coverageMode
                ? "all authored caption IDs in the selected locale run"
                : validationCaptionIds.isEmpty() ? "none (legacy coverage mode)"
                : "requested two existing caption IDs only, all 22 locales");
        report.put("requestedIDs", validationCaptionIds.stream().sorted().toList());
        report.put("measuredCaptionIDs", measuredCaptionIds);
        report.put("nativeCaptionWidth", GuideTextLayout.CAPTION_WIDTH);
        report.put("guiWidths", CAPTION_GUI_WIDTHS);
        report.put("windowWidthsVerified", captionWindowWidthsVerified);
        report.put("unitsChecked", captionUnitsChecked);
        report.put("guiWidthChecks", captionGuiWidthChecks);
        report.put("maximumLines", maximumCaptionLines);
        report.put("measuredLocales", measuredLocales);
        report.put("measurements", List.copyOf(CAPTION_MEASUREMENTS));
        report.put("issues", List.copyOf(CAPTION_LAYOUT_ISSUES));
        Files.writeString(client.gameDirectory.toPath().resolve("caption-overflow.json"),
                new Gson().toJson(report), StandardCharsets.UTF_8);
    }

    private static boolean coverageCaptionMeasurementsComplete() {
        if (!coverageMode || validationCaptionIds.isEmpty()) return true;
        Set<String> measuredCaptionIds = CAPTION_MEASUREMENTS.stream()
                .map(measurement -> measurement.get("lesson") + "_" + measurement.get("step"))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> measuredLocales = CAPTION_MEASUREMENTS.stream()
                .map(measurement -> (String) measurement.get("locale"))
                .collect(java.util.stream.Collectors.toSet());
        return measuredCaptionIds.equals(REQUIRED_COVERAGE_CAPTIONS)
                && measuredLocales.equals(new HashSet<>(ItemCoverageValidation.locales()));
    }

    private static Set<String> parseCoverageCaptionIds(String value) {
        String configured = value == null ? "" : value.trim();
        if (configured.isEmpty()) return Set.of();
        List<String> ids = Arrays.stream(configured.split(",", -1)).map(String::trim).toList();
        if (ids.size() != REQUIRED_COVERAGE_CAPTIONS.size() || ids.stream().anyMatch(String::isEmpty)
                || new HashSet<>(ids).size() != ids.size() || !new HashSet<>(ids).equals(REQUIRED_COVERAGE_CAPTIONS))
            throw new IllegalArgumentException("Coverage caption validation accepts exactly " + REQUIRED_COVERAGE_CAPTIONS
                    + "; received=" + ids);
        return Set.copyOf(ids);
    }

    private static void verifyForeignItemFactoryBoundary() throws Exception {
        var component = GuideResources.parse(GuideBook.lessons().get(0).icon());
        var entry = new net.createmod.ponder.api.registration.StoryBoardEntry() {
            public net.createmod.ponder.api.scene.PonderStoryBoard getBoard() {
                return (scene, util) -> { scene.title("foreign_probe", "Foreign probe"); scene.idle(1); };
            }
            public String getNamespace() { return "mekanismponder_validation_foreign"; }
            public ResourceLocation getSchematicLocation() { return GuideResources.of("mekanismponder", "stage"); }
            public ResourceLocation getComponent() { return component; }
            public List<ResourceLocation> getTags() { return List.of(); }
            public List<SceneOrderingEntry> getOrderingEntries() { return List.of(); }
            public net.createmod.ponder.api.registration.StoryBoardEntry orderBefore(String namespace, String scene) { return this; }
            public net.createmod.ponder.api.registration.StoryBoardEntry orderAfter(String namespace, String scene) { return this; }
            public net.createmod.ponder.api.registration.StoryBoardEntry highlightTag(ResourceLocation tag) { return this; }
            public net.createmod.ponder.api.registration.StoryBoardEntry highlightTags(ResourceLocation... tags) { return this; }
            public net.createmod.ponder.api.registration.StoryBoardEntry highlightAllTags() { return this; }
        };
        PonderScene foreignScene = PonderIndex.getSceneAccess().compile(List.of(entry)).get(0);
        PonderScene ownScene = PonderIndex.getSceneAccess().compile(List.of(GuidePlugin.entry(GuideBook.lessons().get(0).id()))).get(0);
        var adapt = PonderUI.class.getDeclaredMethod("mekanismponder$adapt", PonderUI.class, ItemStack.class,
                net.createmod.ponder.foundation.PonderTag.class);
        adapt.setAccessible(true);
        var constructor = PonderUI.class.getDeclaredConstructor(List.class);
        constructor.setAccessible(true);
        var nativeForeign = constructor.newInstance(List.of(foreignScene));
        var nativePeer = constructor.newInstance(List.of(foreignScene));
        var guide = new GuidePonderScreen(GuideBook.lessons().get(0), nativeForeign.getSubject());
        if (nativeForeign.isEquivalentTo(guide) || guide.isEquivalentTo(nativeForeign)
                || !nativeForeign.isEquivalentTo(nativePeer) || !nativePeer.isEquivalentTo(nativeForeign))
            throw new IllegalStateException("Guide history type guard changed native equivalence or allowed a foreign course match");
        verifyNoHistoryReuse(nativeForeign, guide);
        verifyNoHistoryReuse(guide, nativeForeign);
        japaneseHistoryGuide = guide;
        historyTypeGuardVerified = true;
        var ownTag = PonderIndex.getTagAccess().getRegisteredTag(GuideResources.of("mekanismponder", "start"));
        for (var scenes : List.of(List.of(foreignScene), List.of(ownScene, foreignScene))) {
            var original = constructor.newInstance(scenes);
            var stack = new ItemStack(BuiltInRegistries.ITEM.get(component));
            if (adapt.invoke(null, original, stack, null) != original || adapt.invoke(null, original, stack, ownTag) != original)
                throw new IllegalStateException("Guide adapter replaced a foreign or mixed scene list");
        }
        factoryBoundariesVerified = true;
        var foreignId = PonderIndex.getSceneAccess().getRegisteredEntries().stream()
                .filter(registered -> !registered.getValue().getNamespace().equals("mekanismponder"))
                .map(registered -> registered.getKey())
                .filter(BuiltInRegistries.ITEM::containsKey)
                .findFirst();
        if (foreignId.isEmpty()) return;
        PonderUI foreign = PonderUI.of(new ItemStack(BuiltInRegistries.ITEM.get(foreignId.get())));
        if (foreign instanceof GuidePonderScreen)
            throw new IllegalStateException("The guide adapter claimed a foreign Ponder registry entry: " + foreignId.get());
    }

    private static void verifyCrossLocaleHistory() throws Exception {
        if (japaneseHistoryGuide == null) throw new IllegalStateException("Missing Japanese guide history sample");
        var english = new GuidePonderScreen(GuideBook.lessons().get(0), japaneseHistoryGuide.getSubject());
        if (english.isEquivalentTo(japaneseHistoryGuide) || japaneseHistoryGuide.isEquivalentTo(english))
            throw new IllegalStateException("Guide course history reused a scene compiled for another locale");
        verifyNoHistoryReuse(english, japaneseHistoryGuide);
        verifyNoHistoryReuse(japaneseHistoryGuide, english);
        historyLocaleGuardVerified = true;
    }

    private static void verifyNoHistoryReuse(net.createmod.catnip.gui.NavigatableSimiScreen candidate,
                                            net.createmod.catnip.gui.NavigatableSimiScreen historyHead) throws Exception {
        var stackField = ScreenOpener.class.getDeclaredField("backStack");
        stackField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var stack = (Deque<Screen>) stackField.get(null);
        var saved = List.copyOf(stack);
        var tryBackTracking = ScreenOpener.class.getDeclaredMethod("tryBackTracking",
                net.createmod.catnip.gui.NavigatableSimiScreen.class);
        tryBackTracking.setAccessible(true);
        try {
            stack.clear();
            stack.addFirst(historyHead);
            if ((boolean) tryBackTracking.invoke(null, candidate) || stack.peekFirst() != historyHead)
                throw new IllegalStateException("Native backtracking reused an incompatible guide or foreign history entry");
        } finally {
            stack.clear();
            stack.addAll(saved);
        }
    }

    private static void verifyCourseOrder(PonderUI screen) {
        var course = scenes(screen);
        var expected = GuideBook.lessons().stream().map(GuideBook.Lesson::id).toList();
        var actual = course.stream().map(scene -> scene.getId().getPath()).toList();
        if (!actual.equals(expected)) throw new IllegalStateException("Virtual course differs from ordered lesson catalogue: " + actual);
    }

    private static List<PonderScene> scenes(PonderUI screen) {
        return ((dev.mekanismponder.mixin.PonderUIAccessor) (Object) screen).mekanismponder$getScenes();
    }

    private static GuidePonderScreen requirePonderLesson(GuideBook.Lesson lesson) {
        return requirePonderLesson(lesson, GuideResources.parse(lesson.icon()));
    }

    private static GuidePonderScreen requirePonderLesson(GuideBook.Lesson lesson, ResourceLocation subject) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof GuidePonderScreen ponder)
                || !ponder.getActiveScene().getId().getPath().equals(lesson.id()))
            throw new IllegalStateException("Wrong active Ponder lesson: expected=" + lesson.id() + ", actual="
                    + (screen instanceof PonderUI ui ? ui.getActiveScene().getId() : screenName(Minecraft.getInstance())));
        if (!BuiltInRegistries.ITEM.getKey(ponder.getSubject().getItem()).equals(subject))
            throw new IllegalStateException("Active lesson subject differs: " + lesson.id() + ", expected=" + subject
                    + ", actual=" + BuiltInRegistries.ITEM.getKey(ponder.getSubject().getItem()));
        var access = (PonderUIAccessor) (Object) ponder;
        var ownTags = access.mekanismponder$getTags().stream().map(net.createmod.ponder.foundation.PonderTag::getId)
                .filter(id -> id.getNamespace().equals("mekanismponder")).toList();
        if (!ownTags.equals(List.of(GuideResources.of("mekanismponder", lesson.chapter()))))
            throw new IllegalStateException("Active lesson displays unrelated guide categories: " + lesson.id() + "/" + ownTags);
        var tagButtons = access.mekanismponder$getTagButtons();
        if (tagButtons.size() != access.mekanismponder$getTags().size()
                || tagButtons.stream().anyMatch(button -> !ponder.children().contains(button) || !button.visible)
                || ponder.children().stream().filter(tagButtons::contains).count() != tagButtons.size())
            throw new IllegalStateException("Native playback tag widgets are missing or duplicated: " + lesson.id());
        return ponder;
    }

    private static void clickArrow(PonderUI screen, boolean forward) {
        screen.mouseClicked(screen.width / 2 + (forward ? 28 : -28), screen.height - 41, 0);
    }

    private static void clickBack(Screen screen) {
        screen.mouseClicked(41, screen.height - 41, 0);
    }

    private static void renderEveryScene(Minecraft client) {
        if (renderLessonIndex >= COMPILED.size()) {
            uiStage = 25;
            return;
        }
        CompiledLesson compiled = COMPILED.get(renderLessonIndex);
        if (!(client.screen instanceof ValidationPonderScreen)) {
            client.setScreen(new ValidationPonderScreen(compiled.scene()));
            renderPhase = 0;
            renderWaitFrames = 0;
            return;
        }
        var screen = (ValidationPonderScreen) client.screen;
        if (screen.getActiveScene() != compiled.scene()) throw new IllegalStateException("Renderer is showing the wrong compiled scene");
        if (renderPhase == 0) {
            if (++renderWaitFrames < (navigationOnly ? 4 : 24)) return;
            screen.seekToTime(compiled.scene().getTotalTime() / 2);
            renderPhase = 1;
            return;
        }
        if (renderPhase == 1) {
            if (navigationOnly || List.of("cables", "hydrogen_chloride", "evaporation", "induction", "teleporters")
                    .contains(compiled.lesson().id()))
                screenshot("scene-" + compiled.lesson().id() + ".png");
            screen.seekToTime(compiled.scene().getTotalTime());
            renderPhase = 2;
            return;
        }
        renderLessonIndex++;
        renderPhase = 0;
        renderWaitFrames = 0;
        if (renderLessonIndex < COMPILED.size()) client.setScreen(new ValidationPonderScreen(COMPILED.get(renderLessonIndex).scene()));
        else uiStage = 25;
    }

    private static void screenshot(String fileName) {
        var client = Minecraft.getInstance();
        SCREENSHOTS.add(fileName);
        Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), message -> LOGGER.info("{}", message.getString()));
    }

    private static void writeSuccessAndStop(Minecraft client) throws Exception {
        var result = new HashMap<String, Object>();
        result.put("passed", true);
        result.put("navigationOnly", navigationOnly);
        result.put("representativeGui", representativeGui);
        result.put("lessons", lessons.size());
        result.put("rendered", renderLessonIndex);
        result.put("replay", replayVerified);
        result.put("callouts", verifiedCallouts);
        result.put("transmitterModelData", transmitterModelDataVerified);
        result.put("nativeTagIndex", nativeIndexVerified);
        result.put("nativeNavigation", uiNavigationVerified);
        result.put("tagPagination", tagPaginationVerified);
        result.put("factoryBoundaries", factoryBoundariesVerified);
        result.put("ordinaryGuideButtonsAbsent", ordinaryGuideButtonsAbsent);
        result.put("componentFactory", componentFactoryVerified);
        result.put("playbackTag", playbackTagVerified);
        result.put("chapterBoundary", chapterBoundaryVerified);
        result.put("captionPalettes", captionPalettesVerified);
        result.put("tooltipPalettes", tooltipPalettesVerified);
        result.put("historyTypeGuard", historyTypeGuardVerified);
        result.put("historyLocaleGuard", historyLocaleGuardVerified);
        result.put("arabicFontRechecked", arabicFontRechecked);
        result.put("locales", List.copyOf(VERIFIED_LOCALES));
        result.put("englishFallback", englishFallbackVerified);
        result.put("englishPresentationFallback", presentationFallbackVerified);
        result.put("captionPagesChecked", captionPagesChecked);
        result.put("captionUnitsChecked", captionUnitsChecked);
        result.put("captionGuiWidths", CAPTION_GUI_WIDTHS);
        result.put("captionGuiWidthChecks", captionGuiWidthChecks);
        result.put("captionWindowWidthsVerified", captionWindowWidthsVerified);
        result.put("maximumCaptionLines", maximumCaptionLines);
        result.put("glyphsChecked", glyphsChecked);
        result.put("screenshots", List.copyOf(SCREENSHOTS));
        result.put("validationCoverage", coverageMode);
        if (coverageMode) result.put("itemCoverage", ItemCoverageValidation.successSummary());
        Files.writeString(client.gameDirectory.toPath().resolve("validation-result.json"), new Gson().toJson(result), StandardCharsets.UTF_8);
        LOGGER.info("MEKANISM_PONDER_VALIDATION_PASS: {} lessons selected, {} scenes rendered; {} callouts checked",
                lessons.size(), renderLessonIndex, verifiedCallouts);
        client.stop();
    }

    private static void fail(Minecraft client, Throwable exception) {
        LOGGER.error("MEKANISM_PONDER_VALIDATION_FAIL", exception);
        if (coverageMode) {
            try {
                writeCaptionLayoutReport(client);
            } catch (Exception reportFailure) {
                LOGGER.error("Could not write caption-overflow.json", reportFailure);
            }
            try {
                ItemCoverageValidation.writeReport(client.gameDirectory.toPath().resolve("item-coverage-report.json"), false);
            } catch (Exception reportFailure) {
                LOGGER.error("Could not write item-coverage-report.json", reportFailure);
            }
        }
        try {
            var trace = new StringWriter();
            exception.printStackTrace(new PrintWriter(trace));
            Files.writeString(client.gameDirectory.toPath().resolve("validation-failure.txt"), trace.toString(), StandardCharsets.UTF_8);
        } catch (Exception writeFailure) {
            LOGGER.error("Could not write validation-failure.txt", writeFailure);
        }
        client.stop();
    }

    private static String screenName(Minecraft client) {
        return client.screen == null ? "<none>" : client.screen.getClass().getSimpleName();
    }

    private record CompiledLesson(GuideBook.Lesson lesson, PonderScene scene) {}
    private record PaletteSample(int step, int unit, GuidePresentation.Intent intent) {}
    private record SceneSnapshot(BlockState state, CompoundTag nbt) {}

    private static final class ReloadBlankScreen extends Screen {
        private ReloadBlankScreen() { super(Component.empty()); }

        @Override
        public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    }

    private static final class ValidationPonderScreen extends PonderUI {
        private ValidationPonderScreen(PonderScene scene) {
            super(List.of(scene));
        }
    }
}
