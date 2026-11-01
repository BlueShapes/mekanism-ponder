package dev.mekanismponder.validation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mekanismponder.GuideResources;
import dev.mekanismponder.client.GuideBook;
import dev.mekanismponder.client.GuideItemDescriptions;
import dev.mekanismponder.client.GuidePonderScreen;
import dev.mekanismponder.client.GuidePresentation;
import dev.mekanismponder.client.GuideTextLayout;
import dev.mekanismponder.mixin.PonderUIAccessor;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Runtime item/catalogue evidence checks for the opt-in hidden-client coverage run. */
final class ItemCoverageValidation {
    private static final String ITEM_NAMESPACE = "mekanism";
    private static final String OWN_NAMESPACE = "mekanismponder";
    private static final String CUSTOM_DESCRIPTION_PREFIX = "coverage_";
    private static final Set<String> REQUIRED_FONT_SELECTION = Set.of(
            "coverage_ore_blocks", "coverage_lasers", "coverage_fissile_materials");
    private static final int EXPECTED_EXISTING_DESCRIPTION_COUNT = 12;
    private static final List<Integer> GUI_WIDTHS = List.of(320, 480, 960);
    private static final int MAX_TOOLTIP_UNIT_LINES = 3;

    private static final List<Map<String, Object>> ISSUES = new ArrayList<>();
    private static final List<Map<String, Object>> MEASUREMENTS = new ArrayList<>();
    private static final List<String> REGISTERED_ITEMS = new ArrayList<>();
    private static final List<String> COVERAGE_DESCRIPTIONS = new ArrayList<>();
    private static final List<String> AUTO_INCLUDED_EXISTING_DESCRIPTIONS = new ArrayList<>();
    private static final List<Map<String, Object>> VALIDATED_VARIANT_GROUPS = new ArrayList<>();
    private static final Map<String, ItemExpectation> EXPECTATIONS = new LinkedHashMap<>();
    private static final Map<String, GuideItemDescriptions.Entry> DESCRIPTIONS = new LinkedHashMap<>();
    private static final Map<String, String> RESOURCE_HASHES = new LinkedHashMap<>();
    private static final Map<String, String> CLASS_HASHES = new LinkedHashMap<>();

    private static JsonObject manifest;
    private static List<String> locales = List.of();
    private static List<String> requestedFontDescriptionIds = List.of();
    private static String mekanismJarSha256;
    private static String mekanismJarPath;
    private static String expectationsSha256;
    private static int descriptionsChecked;
    private static int tooltipCopiesChecked;
    private static int ponderSubjectsChecked;
    private static int nativeTooltipKeysChecked;
    private static int nativeTooltipHooksChecked;
    private static int componentCopiesChecked;
    private static int guideAdapterComponentStacksChecked;
    private static int componentfulTooltipAppendCasesChecked;
    private static int tooltipColorLinesChecked;
    private static int factoryItemsChecked;
    private static int creativeItemVariantsChecked;
    private static int transporterItemsChecked;
    private static boolean initialized;

    private ItemCoverageValidation() {}

    static void initialize(String manifestPath) throws Exception {
        if (initialized) throw new IllegalStateException("Coverage validator was initialized more than once");
        requestedFontDescriptionIds = parseRequestedFontDescriptionIds(
                System.getProperty("mekanismponder.validationDescriptions", ""));
        Path path = Path.of(manifestPath);
        if (!Files.isRegularFile(path))
            throw new IllegalStateException("Missing item coverage expectations: " + path.toAbsolutePath());
        expectationsSha256 = hashPath(path);
        manifest = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        enumerateRegisteredItems();
        readExpectations(manifest);
        validateRegistryItemSet();
        validateDescriptions();
        validateItemExpectations();
        validateFactoryVariants();
        validateCreativeVariants();
        validateVariantGroups();
        validateTransporterRegistryItems();
        validateCraftingFormulaCopy();
        validateDefaultComponentCopies();
        validateNativeTooltipHooks();
        validateMekaSuitTooltipAppendPreservesComponents();
        collectEvidenceHashes();
        initialized = true;
    }

    static List<String> locales() {
        return locales;
    }

    static void validateLocale(Minecraft client, String locale) throws Exception {
        if (!initialized) throw new IllegalStateException("Coverage validator has not been initialized");
        if (!locales.contains(locale)) throw new IllegalArgumentException("Locale is not in the guide roster: " + locale);
        if (!client.getLanguageManager().getSelected().equals(locale))
            throw new IllegalStateException("Coverage locale mismatch: expected=" + locale + ", actual="
                    + client.getLanguageManager().getSelected());

        JsonObject language = readJsonResource("/assets/mekanismponder/lang/" + locale + ".json", "language " + locale);
        JsonObject localePresentation = readPresentationLocale(locale);
        var fontSet = fontSet(client.font);
        Set<Integer> seenGlyphs = new HashSet<>();
        int guiWidth = client.getWindow().getGuiScaledWidth();
        int actualWindowWidth = Math.max(1, Math.min(240, guiWidth - 32));

        for (var description : fontSelectedDescriptions()) {
            String key = description.translationKey();
            String expectedText = language.has(key) ? language.get(key).getAsString() : "";
            String actualText = Component.translatable(key).getString();
            if (expectedText.isBlank() || expectedText.equals(key) || actualText.equals(key) || actualText.isBlank()) {
                issue("missing-translation", locale, description.id(), -1, -1, 0, expectedText,
                        "New guide description is absent or blank in the active locale resource");
                continue;
            }
            if (!actualText.equals(expectedText))
                issue("translation-mismatch", locale, description.id(), -1, -1, 0, expectedText,
                        "Runtime translation differs from the loaded guide language resource");
            if (!localePresentation.has(key) || !localePresentation.get(key).isJsonArray()) {
                issue("missing-presentation", locale, description.id(), -1, -1, 0, expectedText,
                        "New description has no explicit intent units in presentation.json");
                continue;
            }

            List<GuidePresentation.Unit> units;
            try {
                units = GuidePresentation.units(key, actualText);
            } catch (RuntimeException exception) {
                issue("invalid-presentation", locale, description.id(), -1, -1, 0, expectedText,
                        exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
                continue;
            }
            if (units.size() != localePresentation.getAsJsonArray(key).size())
                issue("presentation-unit-count", locale, description.id(), -1, -1, 0, actualText,
                        "Runtime presentation unit count differs from the authored locale data");

            List<Component> actualTooltipComponents = appendExpandedDescription(description, actualText);
            List<String> actualTooltipLines = actualTooltipComponents.stream().map(Component::getString).toList();
            List<String> expectedTooltipLinesAtRuntimeWidth = new ArrayList<>();
            List<Integer> expectedTooltipColorsAtRuntimeWidth = new ArrayList<>();
            List<Integer> expectedTooltipUnitsAtRuntimeWidth = new ArrayList<>();
            for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
                GuidePresentation.Unit unit = units.get(unitIndex);
                checkGlyphs(fontSet, unit.text(), seenGlyphs, locale, description.id(), unitIndex);
                List<String> expectedLines = GuideTextLayout.wrap(client.font, unit.text(), actualWindowWidth);
                expectedTooltipLinesAtRuntimeWidth.addAll(expectedLines);
                expectedTooltipColorsAtRuntimeWidth.addAll(java.util.Collections.nCopies(
                        expectedLines.size(), unit.intent().textColor()));
                expectedTooltipUnitsAtRuntimeWidth.addAll(java.util.Collections.nCopies(expectedLines.size(), unitIndex));

                for (int widthIndex = 0; widthIndex < GUI_WIDTHS.size(); widthIndex++) {
                    int virtualGuiWidth = GUI_WIDTHS.get(widthIndex);
                    int width = Math.max(1, Math.min(240, virtualGuiWidth - 32));
                    List<String> lines = GuideTextLayout.wrap(client.font, unit.text(), width);
                    List<Integer> visualWidths = new ArrayList<>();
                    for (String line : lines) {
                        int lineWidth = GuideTextLayout.measuredWidth(client.font, line);
                        visualWidths.add(lineWidth);
                        if (lineWidth > width)
                            issue("glyph-overflow", locale, description.id(), unitIndex, virtualGuiWidth, lines.size(),
                                    unit.text(), "A visually shaped tooltip line exceeds the native width");
                    }
                    if (lines.size() > MAX_TOOLTIP_UNIT_LINES)
                        issue("line-count", locale, description.id(), unitIndex, virtualGuiWidth, lines.size(),
                                unit.text(), "A complete authored description unit exceeds three real-font lines");
                    var measurement = new HashMap<String, Object>();
                    measurement.put("locale", locale);
                    measurement.put("description", description.id());
                    measurement.put("unit", unitIndex);
                    measurement.put("guiWidth", virtualGuiWidth);
                    measurement.put("tooltipWidth", width);
                    measurement.put("lineCount", lines.size());
                    measurement.put("visualGlyphWidths", List.copyOf(visualWidths));
                    measurement.put("text", unit.text());
                    MEASUREMENTS.add(measurement);
                }
                descriptionsChecked++;
            }
            if (actualTooltipLines.size() != expectedTooltipLinesAtRuntimeWidth.size()
                    || !actualTooltipLines.equals(expectedTooltipLinesAtRuntimeWidth))
                issue("native-tooltip-wrap", locale, description.id(), -1, guiWidth, actualTooltipLines.size(), actualText,
                        "GuideItemDescriptions.append did not preserve the expected wrapped line sequence");
            if (!requestedFontDescriptionIds.isEmpty()) {
                for (int line = 0; line < Math.min(actualTooltipComponents.size(), expectedTooltipColorsAtRuntimeWidth.size()); line++) {
                    var color = actualTooltipComponents.get(line).getStyle().getColor();
                    tooltipColorLinesChecked++;
                    if (color == null || color.getValue() != expectedTooltipColorsAtRuntimeWidth.get(line))
                        issue("tooltip-intent-color", locale, description.id(), expectedTooltipUnitsAtRuntimeWidth.get(line),
                                guiWidth, actualTooltipLines.size(),
                                actualTooltipLines.get(line), "Expanded tooltip line color differs from its authored unit intent");
                }
            }
        }

        if (locale.equals("ja_jp") || locale.equals("en_us")) {
            for (ItemExpectation expected : EXPECTATIONS.values()) {
                NativeTooltip nativeTooltip = expected.nativeTooltip();
                if (nativeTooltip == null) continue;
                String value = Component.translatable(nativeTooltip.translationKey()).getString();
                if (value.isBlank() || value.equals(nativeTooltip.translationKey()))
                    issue("missing-native-tooltip-translation", locale, expected.id(), -1, -1, 0, value,
                            "Mekanism native tooltip translation key is unresolved in this checked locale");
            }
        }
    }

    static void writeReport(Path outputPath, boolean passed) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        report.put("passed", passed);
        report.put("baselineCommit", string(manifest, "baselineCommit", ""));
        report.put("targetJarSha256Expected", string(manifest, "targetJarSha256", ""));
        report.put("targetJarSha256Actual", mekanismJarSha256);
        report.put("targetJarPath", mekanismJarPath);
        report.put("expectationsSha256", expectationsSha256);
        report.put("sourceEvidence", manifest != null && manifest.has("sourceEvidence")
                ? manifest.get("sourceEvidence") : new JsonArray());
        report.put("expectationsManifest", manifest == null ? new JsonObject() : manifest);
        report.put("registeredMekanismItems", List.copyOf(REGISTERED_ITEMS));
        report.put("expectedMekanismItems", List.copyOf(EXPECTATIONS.keySet()));
        report.put("items", expectationSummary());
        report.put("variantGroups", List.copyOf(VALIDATED_VARIANT_GROUPS));
        report.put("coverageDescriptions", List.copyOf(COVERAGE_DESCRIPTIONS));
        report.put("fontSelection", requestedFontDescriptionIds.isEmpty()
                ? "legacy unfiltered mode: all 48 coverage descriptions, no captions"
                : "requested three coverage descriptions plus the 12 existing descriptions, all 22 locales");
        report.put("requestedIDs", List.copyOf(requestedFontDescriptionIds));
        report.put("autoIncludedExistingIDs", List.copyOf(AUTO_INCLUDED_EXISTING_DESCRIPTIONS));
        report.put("measuredDescriptionIDs", MEASUREMENTS.stream().map(measurement -> (String) measurement.get("description"))
                .distinct().toList());
        report.put("measuredLocales", MEASUREMENTS.stream().map(measurement -> (String) measurement.get("locale"))
                .distinct().toList());
        report.put("locales", locales);
        report.put("descriptionsChecked", descriptionsChecked);
        report.put("tooltipCopiesChecked", tooltipCopiesChecked);
        report.put("ponderSubjectsChecked", ponderSubjectsChecked);
        report.put("nativeTooltipKeysChecked", nativeTooltipKeysChecked);
        report.put("nativeTooltipHooksChecked", nativeTooltipHooksChecked);
        report.put("componentCopiesChecked", componentCopiesChecked);
        report.put("guideAdapterComponentStacksChecked", guideAdapterComponentStacksChecked);
        report.put("componentfulTooltipAppendCasesChecked", componentfulTooltipAppendCasesChecked);
        report.put("tooltipColorLinesChecked", tooltipColorLinesChecked);
        report.put("factoryItemsChecked", factoryItemsChecked);
        report.put("creativeItemVariantsChecked", creativeItemVariantsChecked);
        report.put("transporterItemsChecked", transporterItemsChecked);
        report.put("transporterColorIsBlockEntityState", true);
        report.put("guiWidths", GUI_WIDTHS);
        report.put("maxTooltipUnitLines", MAX_TOOLTIP_UNIT_LINES);
        report.put("measurements", List.copyOf(MEASUREMENTS));
        report.put("issues", List.copyOf(ISSUES));
        report.put("classSha256", Map.copyOf(CLASS_HASHES));
        report.put("resourceSha256", Map.copyOf(RESOURCE_HASHES));
        report.put("compiledMainFiles", compiledMainFiles());
        Files.writeString(outputPath, new com.google.gson.Gson().toJson(report), StandardCharsets.UTF_8);
    }

    static Map<String, Object> successSummary() {
        var summary = new LinkedHashMap<String, Object>();
        summary.put("passed", ISSUES.isEmpty());
        summary.put("registeredItems", REGISTERED_ITEMS.size());
        summary.put("expectedItems", EXPECTATIONS.size());
        summary.put("items", expectationSummary());
        summary.put("descriptionIds", List.copyOf(COVERAGE_DESCRIPTIONS));
        summary.put("fontSelection", requestedFontDescriptionIds.isEmpty()
                ? "legacy unfiltered mode: all 48 coverage descriptions, no captions"
                : "requested three coverage descriptions plus the 12 existing descriptions, all 22 locales");
        summary.put("requestedIDs", List.copyOf(requestedFontDescriptionIds));
        summary.put("autoIncludedExistingIDs", List.copyOf(AUTO_INCLUDED_EXISTING_DESCRIPTIONS));
        summary.put("measuredDescriptionIDs", MEASUREMENTS.stream().map(measurement -> (String) measurement.get("description"))
                .distinct().toList());
        summary.put("measuredLocales", MEASUREMENTS.stream().map(measurement -> (String) measurement.get("locale"))
                .distinct().toList());
        summary.put("locales", locales);
        summary.put("descriptionsChecked", descriptionsChecked);
        summary.put("tooltipCopiesChecked", tooltipCopiesChecked);
        summary.put("ponderSubjectsChecked", ponderSubjectsChecked);
        summary.put("nativeTooltipKeysChecked", nativeTooltipKeysChecked);
        summary.put("nativeTooltipHooksChecked", nativeTooltipHooksChecked);
        summary.put("componentCopiesChecked", componentCopiesChecked);
        summary.put("guideAdapterComponentStacksChecked", guideAdapterComponentStacksChecked);
        summary.put("componentfulTooltipAppendCasesChecked", componentfulTooltipAppendCasesChecked);
        summary.put("tooltipColorLinesChecked", tooltipColorLinesChecked);
        summary.put("factoryItemsChecked", factoryItemsChecked);
        summary.put("creativeItemVariantsChecked", creativeItemVariantsChecked);
        summary.put("transporterItemsChecked", transporterItemsChecked);
        summary.put("transporterColorIsBlockEntityState", true);
        summary.put("targetJarSha256", mekanismJarSha256);
        summary.put("expectationsSha256", expectationsSha256);
        summary.put("classSha256", Map.copyOf(CLASS_HASHES));
        summary.put("resourceSha256", Map.copyOf(RESOURCE_HASHES));
        summary.put("compiledMainFiles", compiledMainFiles());
        summary.put("issues", List.copyOf(ISSUES));
        return summary;
    }

    private static void readExpectations(JsonObject root) throws Exception {
        JsonArray items = requireArray(root, "items");
        Map<String, Integer> nativeKinds = new HashMap<>();
        Set<String> shiftUpgradeIds = new TreeSet<>();
        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            String id = requiredString(item, "id");
            String group = requiredString(item, "group");
            List<String> ponder = strings(requireArray(item, "ponder"), "ponder for " + id);
            String description = nullableString(item, "description");
            String exclusion = nullableString(item, "exclusion");
            NativeTooltip nativeTooltip = null;
            if (!item.has("nativeTooltip"))
                throw new IllegalStateException("Missing nativeTooltip classification for " + id);
            if (item.has("nativeTooltip") && !item.get("nativeTooltip").isJsonNull()) {
                JsonObject nativeObject = item.getAsJsonObject("nativeTooltip");
                String kind = requiredString(nativeObject, "kind");
                if (!kind.equals("shift+n") && !kind.equals("shift") && !kind.equals("always"))
                    throw new IllegalStateException("Unknown native tooltip kind for " + id + ": " + kind);
                nativeTooltip = new NativeTooltip(kind, requiredString(nativeObject, "translationKey"));
                nativeKinds.merge(kind, 1, Integer::sum);
                if (kind.equals("shift")) shiftUpgradeIds.add(id);
            }
            if (!id.startsWith(ITEM_NAMESPACE + ":"))
                throw new IllegalStateException("Expected item is outside Mekanism namespace: " + id);
            boolean excluded = exclusion != null;
            if (id.equals("mekanism:bounding_block")) {
                if (!excluded || !ponder.isEmpty() || description != null || nativeTooltip != null)
                    throw new IllegalStateException("The internal bounding block must be the sole excluded item: " + id);
            } else if (excluded || (ponder.isEmpty() && description == null && nativeTooltip == null)) {
                throw new IllegalStateException("Usable Mekanism item lacks a valid guide or native tooltip path: " + id);
            }
            if (BuiltInRegistries.ITEM.getOptional(GuideResources.parse(id)).isEmpty()) {
                issue("unregistered-expectation", null, id, -1, -1, 0, "",
                        "Item coverage expectations include an ID absent from the runtime registry");
                continue;
            }
            if (EXPECTATIONS.putIfAbsent(id, new ItemExpectation(id, group, ponder, description,
                    nativeTooltip, exclusion)) != null)
                throw new IllegalStateException("Duplicate item in coverage expectations: " + id);
            if (exclusion != null && exclusion.isBlank())
                throw new IllegalStateException("Empty exclusion reason for " + id);
        }
        if (EXPECTATIONS.isEmpty()) throw new IllegalStateException("Item coverage expectations contain no items");
        if (!nativeKinds.equals(Map.of("shift+n", 158, "shift", 4, "always", 31)))
            throw new IllegalStateException("Native tooltip classifications differ from Mekanism 10.7.19.85: " + nativeKinds);
        if (!shiftUpgradeIds.equals(Set.of("mekanism:upgrade_chemical", "mekanism:upgrade_filter",
                "mekanism:upgrade_muffling", "mekanism:upgrade_stone_generator")))
            throw new IllegalStateException("Native Shift details must cover exactly Mekanism's four upgrade items: " + shiftUpgradeIds);

        try (InputStream stream = ItemCoverageValidation.class.getResourceAsStream(
                "/assets/mekanismponder/guide/languages.json")) {
            if (stream == null) throw new IllegalStateException("Missing locale roster resource");
            locales = List.copyOf(JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet());
        }
        if (locales.size() != 22) throw new IllegalStateException("Expected 22 supported guide locales, found " + locales.size());
    }

    private static void enumerateRegisteredItems() {
        var actual = BuiltInRegistries.ITEM.keySet().stream()
                .filter(id -> id.getNamespace().equals(ITEM_NAMESPACE) && !id.getPath().equals("air"))
                .map(ResourceLocation::toString).sorted().toList();
        REGISTERED_ITEMS.addAll(actual);
    }

    private static void validateRegistryItemSet() {
        Set<String> actualSet = new TreeSet<>(REGISTERED_ITEMS);
        Set<String> expectedSet = new TreeSet<>(EXPECTATIONS.keySet());
        if (!actualSet.equals(expectedSet)) {
            Set<String> missing = new TreeSet<>(actualSet);
            missing.removeAll(expectedSet);
            Set<String> stale = new TreeSet<>(expectedSet);
            stale.removeAll(actualSet);
            issue("registry-expectation-mismatch", null, null, -1, -1, 0, "",
                    "Unclassified registry items=" + missing + "; expected but unregistered=" + stale);
        }
    }

    private static void validateItemExpectations() {
        for (ItemExpectation expected : EXPECTATIONS.values()) {
            ResourceLocation itemId = GuideResources.parse(expected.id());
            Set<String> actualScenes = PonderIndex.getSceneAccess().compile(itemId).stream()
                    .filter(scene -> scene.getId().getNamespace().equals(OWN_NAMESPACE))
                    .map(scene -> scene.getId().toString())
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            Set<String> expectedScenes = expected.ponder().stream().map(id -> OWN_NAMESPACE + ":" + id)
                    .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            if (!actualScenes.equals(expectedScenes))
                throw new IllegalStateException("Ponder item mapping differs for " + expected.id()
                        + "; expected=" + expectedScenes + ", actual=" + actualScenes);
            if (!expected.ponder().isEmpty()) {
                GuideBook.Lesson first = GuideBook.firstLessonForItem(itemId, null);
                if (first == null || !first.id().equals(expected.ponder().get(0)))
                    throw new IllegalStateException("Wrong first lesson for item " + expected.id() + "; expected="
                            + expected.ponder().get(0) + ", actual=" + (first == null ? "<none>" : first.id()));
                for (String lessonId : expected.ponder()) {
                    GuideBook.Lesson lesson = lesson(lessonId);
                    ResourceLocation tagId = GuideResources.of(OWN_NAMESPACE, lesson.chapter());
                    boolean tagged = PonderIndex.getTagAccess().getTags(itemId).stream()
                            .anyMatch(tag -> tag.getId().equals(tagId));
                    if (!tagged) throw new IllegalStateException("Ponder item is missing native chapter tag: "
                            + expected.id() + " -> " + lesson.chapter());
                }
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(itemId));
                PonderUI ui = PonderUI.of(stack);
                var uiScenes = ((PonderUIAccessor) (Object) ui).mekanismponder$getScenes();
                boolean entirelyOwned = !uiScenes.isEmpty()
                        && uiScenes.stream().allMatch(scene -> scene.getId().getNamespace().equals(OWN_NAMESPACE));
                if (entirelyOwned && (!(ui instanceof GuidePonderScreen guide)
                        || !guide.getActiveScene().getId().getPath().equals(expected.ponder().get(0))))
                    throw new IllegalStateException("Native W item factory did not open its expected first lesson: " + expected.id());
                if (ui instanceof GuidePonderScreen guide && !sameStackComponents(stack, guide.getSubject()))
                    throw new IllegalStateException("Native W item factory did not preserve ItemStack components: " + expected.id());
                ponderSubjectsChecked++;
            }
            validateItemDescriptionMapping(expected);
            validateCollapsedAndExpandedTooltip(expected);
        }
    }

    private static void validateDescriptions() {
        for (GuideItemDescriptions.Entry entry : GuideItemDescriptions.descriptions()) {
            if (DESCRIPTIONS.putIfAbsent(entry.id(), entry) != null)
                throw new IllegalStateException("Duplicate runtime guide description ID: " + entry.id());
        }
        for (ItemExpectation expected : EXPECTATIONS.values()) {
            if (expected.description() == null) continue;
            GuideItemDescriptions.Entry description = DESCRIPTIONS.get(expected.description());
            if (description == null)
                throw new IllegalStateException("Coverage expectation references unknown guide description: "
                        + expected.id() + " -> " + expected.description());
            if (!description.items().contains(expected.id()))
                throw new IllegalStateException("Guide description does not list its expected item: "
                        + expected.id() + " -> " + expected.description());
        }
        Set<String> expectedCoverageDescriptions = EXPECTATIONS.values().stream()
                .map(ItemExpectation::description).filter(id -> id != null && id.startsWith(CUSTOM_DESCRIPTION_PREFIX))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        Set<String> actualCoverageDescriptions = DESCRIPTIONS.keySet().stream()
                .filter(id -> id.startsWith(CUSTOM_DESCRIPTION_PREFIX))
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if (!actualCoverageDescriptions.equals(expectedCoverageDescriptions))
            throw new IllegalStateException("New self-authored descriptions differ from item coverage expectations; expected="
                    + expectedCoverageDescriptions + ", actual=" + actualCoverageDescriptions);
        COVERAGE_DESCRIPTIONS.addAll(actualCoverageDescriptions);
        if (COVERAGE_DESCRIPTIONS.isEmpty())
            throw new IllegalStateException("No self-authored coverage descriptions were added");
        if (!COVERAGE_DESCRIPTIONS.containsAll(requestedFontDescriptionIds))
            throw new IllegalStateException("Requested font descriptions are not registered coverage IDs: "
                    + requestedFontDescriptionIds);
        List<String> existingDescriptions = DESCRIPTIONS.keySet().stream()
                .filter(id -> !id.startsWith(CUSTOM_DESCRIPTION_PREFIX)).toList();
        if (existingDescriptions.size() != EXPECTED_EXISTING_DESCRIPTION_COUNT)
            throw new IllegalStateException("Expected exactly " + EXPECTED_EXISTING_DESCRIPTION_COUNT
                    + " existing descriptions, found " + existingDescriptions.size());
        if (!requestedFontDescriptionIds.isEmpty())
            AUTO_INCLUDED_EXISTING_DESCRIPTIONS.addAll(existingDescriptions);
        for (GuideItemDescriptions.Entry entry : coverageDescriptions()) {
            for (String item : entry.items()) {
                ItemExpectation expected = EXPECTATIONS.get(item);
                if (expected == null || !entry.id().equals(expected.description()))
                    throw new IllegalStateException("Description item mapping is not bidirectional: " + entry.id() + "/" + item);
            }
        }
    }

    private static void validateItemDescriptionMapping(ItemExpectation expected) {
        GuideItemDescriptions.Entry actual = null;
        for (GuideItemDescriptions.Entry candidate : GuideItemDescriptions.descriptions())
            if (candidate.items().contains(expected.id())) actual = candidate;
        String actualId = actual == null ? null : actual.id();
        if (!java.util.Objects.equals(actualId, expected.description()))
            throw new IllegalStateException("Guide item description mapping differs for " + expected.id()
                    + "; expected=" + expected.description() + ", actual=" + actualId);
    }

    private static void validateCollapsedAndExpandedTooltip(ItemExpectation expected) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(GuideResources.parse(expected.id())));
        Component name = Component.literal("Original item name");
        Component tail = Component.literal("Existing tooltip line");
        var collapsed = new ArrayList<Component>(List.of(name, tail));
        GuideItemDescriptions.append(stack, collapsed, false);
        if (collapsed.get(0) != name || collapsed.get(collapsed.size() - 1) != tail)
            throw new IllegalStateException("Collapsed guide tooltip changed existing lines: " + expected.id());
        if (expected.description() == null) {
            if (collapsed.size() != 2) throw new IllegalStateException("Undescribed item received guide tooltip text: " + expected.id());
            tooltipCopiesChecked++;
            return;
        }
        if (collapsed.size() != 3 || collapsed.get(1).getStyle().getColor() == null
                || collapsed.get(1).getStyle().getColor().getValue() != GuidePresentation.Intent.HINT.textColor())
            throw new IllegalStateException("Collapsed Shift hint is missing or has the wrong color: " + expected.id());
        var expanded = new ArrayList<Component>(List.of(name, tail));
        GuideItemDescriptions.append(stack, expanded, true);
        if (expanded.get(0) != name || expanded.get(expanded.size() - 1) != tail)
            throw new IllegalStateException("Expanded guide tooltip changed existing lines: " + expected.id());
        tooltipCopiesChecked++;
    }

    private static void validateVariantGroups() {
        if (!manifest.has("variantGroups") || !manifest.get("variantGroups").isJsonArray())
            throw new IllegalStateException("Item coverage expectations need a variantGroups array");
        for (JsonElement element : manifest.getAsJsonArray("variantGroups")) {
            JsonObject group = element.getAsJsonObject();
            String kind = requiredString(group, "kind");
            List<String> ids = group.has("ids") && group.get("ids").isJsonArray()
                    ? strings(group.getAsJsonArray("ids"), "variant group " + kind) : List.of();
            Set<String> unique = new HashSet<>(ids);
            if (unique.size() != ids.size()) throw new IllegalStateException("Duplicate IDs in item variant group: " + kind);
            for (String id : ids) {
                if (!EXPECTATIONS.containsKey(id)) {
                    issue("unclassified-variant-item", null, id, -1, -1, 0, "",
                            "Variant group " + kind + " names an item missing from the coverage manifest");
                }
            }
            var summary = new LinkedHashMap<String, Object>();
            for (var entry : group.entrySet()) summary.put(entry.getKey(), entry.getValue());
            if (!ids.isEmpty()) summary.put("validatedRegistryIds", ids.stream().sorted().toList());
            Integer actualCount = switch (kind) {
                case "factory_tiers" -> factoryItemsChecked;
                case "energy_cubes" -> countInstances("mekanism.common.item.block.ItemBlockEnergyCube");
                case "fluid_tanks" -> countInstances("mekanism.common.item.block.machine.ItemBlockFluidTank");
                case "chemical_tanks" -> countInstances("mekanism.common.item.block.ItemBlockChemicalTank");
                case "transmitter_colors" -> countInstances("mekanism.common.item.block.transmitter.ItemBlockTransporter");
                case "primary_processing_intermediates" -> validateProcessingIntermediates();
                default -> null;
            };
            if (actualCount != null) {
                summary.put("actualItemCount", actualCount);
                if (group.has("expectedItems") && group.get("expectedItems").isJsonPrimitive()
                        && group.getAsJsonPrimitive("expectedItems").isNumber()) {
                    int expectedCount = group.get("expectedItems").getAsInt();
                    if (expectedCount != actualCount)
                        issue("variant-group-count", null, kind, -1, -1, 0, "",
                                "Expected " + expectedCount + " registry items, found " + actualCount);
                }
            } else if (group.has("expectedItems")) {
                summary.put("countValidation", "item IDs are covered by the exhaustive registry comparison");
            }
            VALIDATED_VARIANT_GROUPS.add(summary);
        }
        if (VALIDATED_VARIANT_GROUPS.isEmpty()) throw new IllegalStateException("No item variant groups were supplied");
    }

    private static int countInstances(String className) {
        try {
            Class<?> itemType = Class.forName(className);
            int count = 0;
            for (String id : REGISTERED_ITEMS)
                if (itemType.isInstance(BuiltInRegistries.ITEM.get(GuideResources.parse(id)))) count++;
            return count;
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Mekanism item class is missing from the pinned runtime: " + className, exception);
        }
    }

    private static int validateProcessingIntermediates() {
        JsonArray recipes = requireArray(manifest, "processingRecipes");
        Set<String> outputIds = new TreeSet<>();
        Set<String> registered = new HashSet<>(REGISTERED_ITEMS);
        for (JsonElement element : recipes) {
            JsonObject recipe = element.getAsJsonObject().getAsJsonObject("recipe");
            if (recipe == null || !recipe.has("output") || !recipe.get("output").isJsonObject()) {
                issue("invalid-processing-evidence", null, null, -1, -1, 0, "",
                        "A processing evidence entry has no item output object");
                continue;
            }
            JsonObject output = recipe.getAsJsonObject("output");
            if (!output.has("id") || !output.get("id").isJsonPrimitive()
                    || !output.getAsJsonPrimitive("id").isString()) {
                issue("invalid-processing-evidence", null, null, -1, -1, 0, "",
                        "A processing evidence output has no explicit item ID");
                continue;
            }
            String id = output.get("id").getAsString();
            if (!registered.contains(id))
                issue("unregistered-processing-output", null, id, -1, -1, 0, "",
                        "A cited processing intermediate is absent from the runtime Mekanism registry");
            if (!outputIds.add(id))
                issue("duplicate-processing-output", null, id, -1, -1, 0, "",
                        "Processing evidence does not describe a unique intermediate output");
        }
        return outputIds.size();
    }

    private static void validateFactoryVariants() throws Exception {
        Class<?> factoryItemType = Class.forName("mekanism.common.item.block.machine.ItemBlockFactory");
        Map<String, Integer> counts = new HashMap<>();
        Set<String> tierNames = new TreeSet<>();
        Set<String> factoryNames = new TreeSet<>();
        for (String id : REGISTERED_ITEMS) {
            Item item = BuiltInRegistries.ITEM.get(GuideResources.parse(id));
            if (!factoryItemType.isInstance(item)) continue;
            Object tier = factoryItemType.getMethod("getTier").invoke(item);
            String tierName = ((Enum<?>) tier).name();
            Object block = item.getClass().getMethod("getBlock").invoke(item);
            Object type = block.getClass().getMethod("getType").invoke(block);
            Object factoryType = type.getClass().getMethod("getFactoryType").invoke(type);
            String factoryName = ((Enum<?>) factoryType).name();
            tierNames.add(tierName);
            factoryNames.add(factoryName);
            counts.merge(factoryName + "/" + tierName, 1, Integer::sum);
            assertComponentsPreserved("factory:" + id, new ItemStack(item));
            factoryItemsChecked++;
        }
        if (!tierNames.equals(Set.of("BASIC", "ADVANCED", "ELITE", "ULTIMATE")))
            throw new IllegalStateException("Factory tier coverage differs from Mekanism 10.7.19.85: " + tierNames);
        if (factoryNames.size() != 9 || factoryItemsChecked != 36)
            throw new IllegalStateException("Expected nine factory types by four tiers (36 items), found " + factoryItemsChecked
                    + " across " + factoryNames);
        for (String factory : factoryNames) for (String tier : tierNames)
            if (counts.getOrDefault(factory + "/" + tier, 0) != 1)
                throw new IllegalStateException("Expected exactly one factory item for " + factory + "/" + tier);
    }

    private static void validateCreativeVariants() throws Exception {
        Class<?> energyCubeType = Class.forName("mekanism.common.item.block.ItemBlockEnergyCube");
        Class<?> chemicalTankType = Class.forName("mekanism.common.item.block.ItemBlockChemicalTank");
        Class<?> fluidTankType = Class.forName("mekanism.common.item.block.machine.ItemBlockFluidTank");
        Map<String, Class<?>> creativeTypes = Map.of(
                "mekanism:creative_energy_cube", energyCubeType,
                "mekanism:creative_chemical_tank", chemicalTankType,
                "mekanism:creative_fluid_tank", fluidTankType);
        for (var entry : creativeTypes.entrySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(GuideResources.parse(entry.getKey()))
                    .orElseThrow(() -> new IllegalStateException("Pinned creative machine item is missing: " + entry.getKey()));
            if (!entry.getValue().isInstance(item))
                throw new IllegalStateException("Creative item has unexpected Mekanism item type: " + entry.getKey()
                        + " -> " + item.getClass().getName());
            Object tier = item.getClass().getMethod("getTier").invoke(item);
            if (!((Enum<?>) tier).name().equals("CREATIVE"))
                throw new IllegalStateException("Creative item reports non-creative tier: " + entry.getKey());
            assertComponentsPreserved("creative:" + entry.getKey(), new ItemStack(item));
            if (entry.getKey().equals("mekanism:creative_energy_cube"))
                validateCreativeEnergyCubeStacks(item, GuideResources.parse(entry.getKey()));
        }
    }

    private static void validateCreativeEnergyCubeStacks(Item item, ResourceLocation itemId) throws Exception {
        Class<?> customContents = Class.forName(
                "mekanism.common.registration.impl.CreativeTabDeferredRegister$ICustomCreativeTabContents");
        if (!customContents.isInstance(item))
            throw new IllegalStateException("Creative energy cube no longer exposes its real creative variants");
        Holder<Item> holder = BuiltInRegistries.ITEM.getHolderOrThrow(ResourceKey.create(Registries.ITEM, itemId));
        List<ItemStack> variants = new ArrayList<>();
        Method addItems = customContents.getMethod("addItems", Holder.class, java.util.function.Consumer.class);
        addItems.invoke(item, holder, (java.util.function.Consumer<ItemStack>) variants::add);
        if (variants.size() != 2)
            throw new IllegalStateException("Expected Mekanism's two creative cube stacks, found " + variants.size());

        Class<?> energyCubeType = Class.forName("mekanism.common.item.block.ItemBlockEnergyCube");
        Object sideConfigComponent = componentType("mekanism.common.registries.MekanismDataComponents", "SIDE_CONFIG");
        List<Object> expectedConfigs = List.of(energyCubeType.getField("ALL_INPUT").get(null),
                energyCubeType.getField("ALL_OUTPUT").get(null));
        for (int index = 0; index < variants.size(); index++) {
            ItemStack variant = variants.get(index);
            if (variant.getItem() != item)
                throw new IllegalStateException("Creative cube variant changed registry identity at index " + index);
            if (!java.util.Objects.equals(getComponent(variant, sideConfigComponent), expectedConfigs.get(index)))
                throw new IllegalStateException("Creative cube variant lost its expected side config at index " + index);
            assertComponentsPreserved("creative-energy-cube-variant:" + index, variant);
            assertGuideAdapterPreservesComponents("creative-energy-cube-variant:" + index, variant);
            creativeItemVariantsChecked++;
        }
    }

    private static void validateTransporterRegistryItems() {
        transporterItemsChecked = countInstances("mekanism.common.item.block.transmitter.ItemBlockTransporter");
        if (transporterItemsChecked == 0) throw new IllegalStateException("No registered transporter items were found");
    }

    private static void validateCraftingFormulaCopy() throws Exception {
        String formulaId = "mekanism:crafting_formula";
        Item item = BuiltInRegistries.ITEM.getOptional(GuideResources.parse(formulaId))
                .orElseThrow(() -> new IllegalStateException("Crafting formula item is missing from the registry"));
        Class<?> formulaType = Class.forName("mekanism.common.attachments.FormulaAttachment");
        List<ItemStack> ingredients = new ArrayList<>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        ingredients.set(4, new ItemStack(BuiltInRegistries.ITEM.get(GuideResources.parse("minecraft:iron_ingot"))));
        Object formula = formulaType.getConstructor(List.class, boolean.class).newInstance(ingredients, false);
        Object component = componentType("mekanism.common.registries.MekanismDataComponents", "FORMULA_HOLDER");
        ItemStack stack = new ItemStack(item);
        setComponent(stack, component, formula);
        if (!java.util.Objects.equals(getComponent(stack, component), formula))
            throw new IllegalStateException("Crafting formula did not retain its sample recipe attachment");
        assertComponentsPreserved("crafting-formula", stack);
        assertGuideAdapterPreservesComponents("crafting-formula", stack);
    }

    private static void validateDefaultComponentCopies() {
        for (String id : REGISTERED_ITEMS) {
            Item item = BuiltInRegistries.ITEM.get(GuideResources.parse(id));
            ItemStack stack = item.getDefaultInstance();
            assertComponentsPreserved("default:" + id, stack);
        }
    }

    private static void validateNativeTooltipHooks() throws Exception {
        int keyCount = 0;
        for (ItemExpectation expected : EXPECTATIONS.values()) {
            NativeTooltip specification = expected.nativeTooltip();
            if (specification == null) continue;
            String localized = Component.translatable(specification.translationKey()).getString();
            if (localized.isBlank() || localized.equals(specification.translationKey()))
                throw new IllegalStateException("Unresolved Mekanism native tooltip key " + specification.translationKey()
                        + " for " + expected.id());
            keyCount++;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(GuideResources.parse(expected.id())));
            List<Component> tooltip = invokeNativeTooltip(stack, specification.kind().equals("shift+n"),
                    specification.kind().equals("shift"));
            String normalizedTooltip = tooltip.stream().map(Component::getString).map(ItemCoverageValidation::withoutWhitespace)
                    .reduce("", String::concat);
            if (!normalizedTooltip.contains(withoutWhitespace(localized)))
                throw new IllegalStateException("Mekanism native tooltip hook did not append its expected text: "
                        + expected.id() + " / " + specification.translationKey());
            nativeTooltipHooksChecked++;
        }
        nativeTooltipKeysChecked = keyCount;
    }

    private static List<Component> invokeNativeTooltip(ItemStack stack, boolean descriptionKeyDown,
                                                        boolean detailsKeyDown) throws Exception {
        var client = Minecraft.getInstance();
        Class<?> handlerClass = Class.forName("mekanism.client.key.MekanismKeyHandler");
        KeyMapping descriptionKey = (KeyMapping) handlerClass.getField("descriptionKey").get(null);
        KeyMapping detailsKey = (KeyMapping) handlerClass.getField("detailsKey").get(null);
        boolean oldDescriptionDown = descriptionKey.isDown();
        boolean oldDetailsDown = detailsKey.isDown();
        var descriptionContext = descriptionKey.getKeyConflictContext();
        var detailsContext = detailsKey.getKeyConflictContext();
        var descriptionModifier = descriptionKey.getKeyModifier();
        var detailsModifier = detailsKey.getKeyModifier();
        var descriptionCode = descriptionKey.getKey();
        var detailsCode = detailsKey.getKey();
        List<Component> tooltip = new ArrayList<>();
        try {
            // This is a logical key-branch test in a hidden client, not physical
            // keyboard input. GUI-only context and the physical Shift modifier
            // would otherwise suppress KeyMapping.isDown before the first UI.
            descriptionKey.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
            detailsKey.setKeyConflictContext(KeyConflictContext.UNIVERSAL);
            descriptionKey.setKeyModifierAndCode(KeyModifier.NONE, descriptionCode);
            detailsKey.setKeyModifierAndCode(KeyModifier.NONE, detailsCode);
            descriptionKey.setDown(descriptionKeyDown);
            detailsKey.setDown(detailsKeyDown);
            if (descriptionKey.isDown() != descriptionKeyDown || detailsKey.isDown() != detailsKeyDown)
                throw new IllegalStateException("Logical native tooltip key precondition was not established");
            Class<?> contextClass = Class.forName("net.minecraft.world.item.Item$TooltipContext");
            Object context = createTooltipContext(contextClass, client);
            Class<?> flagClass = Class.forName("net.minecraft.world.item.TooltipFlag");
            Object normalFlag = staticField(flagClass, "NORMAL");
            Method append = stack.getItem().getClass().getMethod("appendHoverText", ItemStack.class,
                    contextClass, List.class, flagClass);
            append.invoke(stack.getItem(), stack, context, tooltip, normalFlag);
        } finally {
            descriptionKey.setKeyModifierAndCode(descriptionModifier, descriptionCode);
            detailsKey.setKeyModifierAndCode(detailsModifier, detailsCode);
            descriptionKey.setKeyConflictContext(descriptionContext);
            detailsKey.setKeyConflictContext(detailsContext);
            descriptionKey.setDown(oldDescriptionDown);
            detailsKey.setDown(oldDetailsDown);
        }
        return tooltip;
    }

    private static Object createTooltipContext(Class<?> contextClass, Minecraft client) throws Exception {
        for (Method method : contextClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) || !method.getName().equals("of") || method.getParameterCount() != 1)
                continue;
            Class<?> parameter = method.getParameterTypes()[0];
            Object value = null;
            if (parameter.isInstance(client.level)) value = client.level;
            else if (parameter.isInstance(client.level.registryAccess())) value = client.level.registryAccess();
            if (value != null) return method.invoke(null, value);
        }
        try {
            return contextClass.getField("EMPTY").get(null);
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Could not create the pinned Item.TooltipContext", exception);
        }
    }

    private static void assertComponentsPreserved(String context, ItemStack source) {
        if (source.isEmpty()) throw new IllegalStateException("Cannot test components on an empty stack: " + context);
        ItemStack copy = source.copy();
        if (source.getItem() != copy.getItem() || !source.getComponents().equals(copy.getComponents()))
            throw new IllegalStateException("ItemStack.copy changed registry identity or components: " + context);
        componentCopiesChecked++;
    }

    private static void assertGuideAdapterPreservesComponents(String context, ItemStack source) {
        PonderUI ui = PonderUI.of(source);
        if (!(ui instanceof GuidePonderScreen guide))
            throw new IllegalStateException("Expected the native W adapter to open this guide subject: " + context);
        if (!sameStackComponents(source, guide.getSubject()))
            throw new IllegalStateException("Native W adapter changed ItemStack registry identity or components: " + context);
        guideAdapterComponentStacksChecked++;
    }

    private static void validateMekaSuitTooltipAppendPreservesComponents() throws Exception {
        String itemId = "mekanism:mekasuit_helmet";
        Item item = BuiltInRegistries.ITEM.getOptional(GuideResources.parse(itemId))
                .orElseThrow(() -> new IllegalStateException("Meka Suit helmet is missing from the registry"));
        ItemExpectation expected = EXPECTATIONS.get(itemId);
        if (expected == null || !expected.ponder().isEmpty() || expected.description() == null)
            throw new IllegalStateException("Expected the Meka Suit helmet to be a described non-W item: " + itemId);

        Class<?> customContents = Class.forName(
                "mekanism.common.registration.impl.CreativeTabDeferredRegister$ICustomCreativeTabContents");
        if (!customContents.isInstance(item))
            throw new IllegalStateException("Meka Suit helmet no longer exposes its real creative stack");
        Holder<Item> holder = BuiltInRegistries.ITEM.getHolderOrThrow(
                ResourceKey.create(Registries.ITEM, GuideResources.parse(itemId)));
        List<ItemStack> variants = new ArrayList<>();
        customContents.getMethod("addItems", Holder.class, java.util.function.Consumer.class)
                .invoke(item, holder, (java.util.function.Consumer<ItemStack>) variants::add);
        if (variants.size() != 1 || variants.get(0).getItem() != item)
            throw new IllegalStateException("Expected one real Meka Suit helmet creative stack, found " + variants.size());

        ItemStack source = variants.get(0);
        Object moduleContainer = componentType("mekanism.common.registries.MekanismDataComponents", "MODULE_CONTAINER");
        if (getComponent(source, moduleContainer) == null)
            throw new IllegalStateException("Real Meka Suit creative stack has no module-container data");
        Item originalItem = source.getItem();
        int originalCount = source.getCount();
        var originalComponents = source.getComponents();

        List<Component> collapsed = new ArrayList<>(List.of(Component.literal("existing")));
        GuideItemDescriptions.append(source, collapsed, false);
        if (collapsed.size() <= 1)
            throw new IllegalStateException("Collapsed guide Shift append changed or failed to use the Meka Suit stack data");
        assertStackUnchanged("guide collapsed Meka Suit", source, originalItem, originalCount, originalComponents);
        componentfulTooltipAppendCasesChecked++;

        List<Component> expanded = new ArrayList<>(List.of(Component.literal("existing")));
        GuideItemDescriptions.append(source, expanded, true);
        if (expanded.size() <= 1)
            throw new IllegalStateException("Expanded guide Shift append changed or failed to use the Meka Suit stack data");
        assertStackUnchanged("guide expanded Meka Suit", source, originalItem, originalCount, originalComponents);
        componentfulTooltipAppendCasesChecked++;

        invokeNativeTooltip(source, false, false);
        assertStackUnchanged("native collapsed Meka Suit", source, originalItem, originalCount, originalComponents);
        componentfulTooltipAppendCasesChecked++;

        invokeNativeTooltip(source, false, true);
        assertStackUnchanged("native expanded Meka Suit", source, originalItem, originalCount, originalComponents);
        componentfulTooltipAppendCasesChecked++;
    }

    private static boolean sameStackComponents(ItemStack first, ItemStack second) {
        return !second.isEmpty() && first.getItem() == second.getItem() && first.getCount() == second.getCount()
                && first.getComponents().equals(second.getComponents());
    }

    private static void assertStackUnchanged(String context, ItemStack stack, Item item, int count, Object components) {
        if (stack.getItem() != item || stack.getCount() != count || !components.equals(stack.getComponents()))
            throw new IllegalStateException("Tooltip append changed the source ItemStack: " + context);
    }

    private static void setComponent(ItemStack stack, Object componentType, Object value) throws Exception {
        Method set = ItemStack.class.getMethod("set", net.minecraft.core.component.DataComponentType.class, Object.class);
        set.invoke(stack, componentType, value);
    }

    private static Object getComponent(ItemStack stack, Object componentType) throws Exception {
        Method get = ItemStack.class.getMethod("get", net.minecraft.core.component.DataComponentType.class);
        return get.invoke(stack, componentType);
    }

    private static Object componentType(String ownerClassName, String fieldName) throws Exception {
        Object holder = Class.forName(ownerClassName).getField(fieldName).get(null);
        try {
            return holder.getClass().getMethod("get").invoke(holder);
        } catch (NoSuchMethodException ignored) {
            if (holder instanceof java.util.function.Supplier<?> supplier) return supplier.get();
            throw new IllegalStateException("Data component holder does not expose get(): " + ownerClassName + "." + fieldName);
        }
    }

    private static Object staticField(Class<?> type, String name) throws Exception {
        Field field = type.getField(name);
        return field.get(null);
    }

    private static List<Component> appendExpandedDescription(GuideItemDescriptions.Entry description, String expectedText) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(GuideResources.parse(description.items().get(0))));
        var tooltip = new ArrayList<Component>(List.of(Component.literal("Existing item name"), Component.literal("Existing tail")));
        GuideItemDescriptions.append(stack, tooltip, true);
        if (!tooltip.get(0).getString().equals("Existing item name")
                || !tooltip.get(tooltip.size() - 1).getString().equals("Existing tail"))
            issue("tooltip-preservation", Minecraft.getInstance().getLanguageManager().getSelected(), description.id(),
                    -1, -1, 0, expectedText, "Guide tooltip did not preserve pre-existing vanilla lines");
        return List.copyOf(tooltip.subList(1, tooltip.size() - 1));
    }

    private static void checkGlyphs(FontSet fontSet, String text, Set<Integer> seen, String locale,
                                    String descriptionId, int unitIndex) {
        Component.literal(text).getVisualOrderText().accept((index, style, codepoint) -> {
            if (seen.add(codepoint) && !Character.isWhitespace(codepoint)
                    && Character.getType(codepoint) != Character.FORMAT
                    && fontSet.getGlyphInfo(codepoint, false) == SpecialGlyphs.MISSING)
                issue("missing-glyph", locale, descriptionId, unitIndex, -1, 0, text,
                        "Description uses a missing visual glyph U+" + Integer.toHexString(codepoint));
            return true;
        });
    }

    private static FontSet fontSet(Font font) throws Exception {
        Method getFontSet = Font.class.getDeclaredMethod("getFontSet", ResourceLocation.class);
        getFontSet.setAccessible(true);
        return (FontSet) getFontSet.invoke(font, Style.DEFAULT_FONT);
    }

    private static JsonObject readPresentationLocale(String locale) throws Exception {
        JsonObject root = readJsonResource("/assets/mekanismponder/guide/presentation.json", "presentation data");
        if (!root.has("locales") || !root.getAsJsonObject("locales").has(locale))
            return new JsonObject();
        return root.getAsJsonObject("locales").getAsJsonObject(locale);
    }

    private static JsonObject readJsonResource(String resource, String label) throws Exception {
        try (InputStream stream = ItemCoverageValidation.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing " + label + " resource: " + resource);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<GuideItemDescriptions.Entry> coverageDescriptions() {
        return COVERAGE_DESCRIPTIONS.stream().map(DESCRIPTIONS::get).filter(java.util.Objects::nonNull).toList();
    }

    private static List<GuideItemDescriptions.Entry> fontSelectedDescriptions() {
        if (requestedFontDescriptionIds.isEmpty()) return coverageDescriptions();
        var selected = new ArrayList<GuideItemDescriptions.Entry>();
        for (String id : requestedFontDescriptionIds) {
            GuideItemDescriptions.Entry description = DESCRIPTIONS.get(id);
            if (description == null)
                throw new IllegalStateException("Requested font description is not registered: " + id);
            selected.add(description);
        }
        for (String id : AUTO_INCLUDED_EXISTING_DESCRIPTIONS) {
            GuideItemDescriptions.Entry description = DESCRIPTIONS.get(id);
            if (description == null)
                throw new IllegalStateException("Existing font description is not registered: " + id);
            selected.add(description);
        }
        return List.copyOf(selected);
    }

    private static List<String> parseRequestedFontDescriptionIds(String value) {
        String configured = value == null ? "" : value.trim();
        if (configured.isEmpty()) return List.of();
        List<String> ids = java.util.Arrays.stream(configured.split(",", -1)).map(String::trim).toList();
        if (ids.size() != REQUIRED_FONT_SELECTION.size() || ids.stream().anyMatch(String::isEmpty)
                || new HashSet<>(ids).size() != ids.size() || !new HashSet<>(ids).equals(REQUIRED_FONT_SELECTION))
            throw new IllegalArgumentException("Coverage font validation accepts exactly " + REQUIRED_FONT_SELECTION
                    + "; received=" + ids);
        return List.copyOf(ids);
    }

    private static GuideBook.Lesson lesson(String id) {
        return GuideBook.lessons().stream().filter(candidate -> candidate.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Expected item maps to an unknown lesson: " + id));
    }

    private static JsonArray requireArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray())
            throw new IllegalStateException("Expected JSON array property: " + key);
        return object.getAsJsonArray(key);
    }

    private static List<String> strings(JsonArray array, String label) {
        var result = new ArrayList<String>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new IllegalStateException("Expected a string in " + label);
            result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString()
                || object.get(key).getAsString().isBlank())
            throw new IllegalStateException("Expected nonblank JSON string property: " + key);
        return object.get(key).getAsString();
    }

    private static String nullableString(JsonObject object, String key) {
        if (!object.has(key)) throw new IllegalStateException("Missing nullable JSON property: " + key);
        if (object.get(key).isJsonNull()) return null;
        if (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isString())
            throw new IllegalStateException("Expected nullable string property: " + key);
        String value = object.get(key).getAsString();
        if (value.isBlank()) throw new IllegalStateException("Expected null or a nonblank JSON string property: " + key);
        return value;
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
    }

    private static String withoutWhitespace(String text) {
        var result = new StringBuilder();
        text.codePoints().filter(codepoint -> !Character.isWhitespace(codepoint) && !Character.isSpaceChar(codepoint))
                .forEach(result::appendCodePoint);
        return result.toString();
    }

    private static List<Map<String, Object>> expectationSummary() {
        return EXPECTATIONS.values().stream().map(expected -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", expected.id());
            item.put("group", expected.group());
            item.put("ponder", expected.ponder());
            item.put("description", expected.description());
            if (expected.nativeTooltip() == null) item.put("nativeTooltip", null);
            else item.put("nativeTooltip", Map.of("kind", expected.nativeTooltip().kind(),
                    "translationKey", expected.nativeTooltip().translationKey()));
            item.put("exclusion", expected.exclusion());
            return item;
        }).toList();
    }

    private static void issue(String type, String locale, String item, int unit, int guiWidth, int lineCount,
                              String text, String message) {
        var issue = new LinkedHashMap<String, Object>();
        issue.put("type", type);
        if (locale != null && !locale.isBlank()) issue.put("locale", locale);
        if (item != null && !item.isBlank()) issue.put("itemOrDescription", item);
        if (unit >= 0) issue.put("unit", unit);
        if (guiWidth >= 0) issue.put("guiWidth", guiWidth);
        issue.put("lineCount", lineCount);
        issue.put("text", text);
        issue.put("message", message);
        ISSUES.add(issue);
    }

    private static void collectEvidenceHashes() throws Exception {
        for (String name : List.of(
                "dev/mekanismponder/validation/GuideValidation.class",
                "dev/mekanismponder/validation/ItemCoverageValidation.class",
                "dev/mekanismponder/MekanismPonder.class",
                "dev/mekanismponder/client/GuideBook.class",
                "dev/mekanismponder/client/GuideCommands.class",
                "dev/mekanismponder/client/GuideItemDescriptions.class",
                "dev/mekanismponder/client/GuidePlugin.class",
                "dev/mekanismponder/client/GuidePonderScreen.class",
                "dev/mekanismponder/client/GuidePresentation.class",
                "dev/mekanismponder/client/GuideScenes.class",
                "dev/mekanismponder/client/GuideTextLayout.class",
                "dev/mekanismponder/mixin/PonderUIFactoryMixin.class",
                "dev/mekanismponder/mixin/PonderUIAccessor.class")) {
            String hash = hashResource("/" + name);
            if (hash != null) CLASS_HASHES.put(name, hash);
        }
        for (String name : List.of(
                "/assets/mekanismponder/guide/lessons.json",
                "/assets/mekanismponder/guide/item_descriptions.json",
                "/assets/mekanismponder/guide/presentation.json",
                "/assets/mekanismponder/guide/languages.json")) {
            String hash = hashResource(name);
            if (hash != null) RESOURCE_HASHES.put(name, hash);
        }
        for (String locale : locales) {
            String name = "/assets/mekanismponder/lang/" + locale + ".json";
            String hash = hashResource(name);
            if (hash != null) RESOURCE_HASHES.put(name, hash);
        }
        mekanismJarSha256 = hashMekanismJar();
        String expectedHash = string(manifest, "targetJarSha256", "").trim();
        if (!expectedHash.isEmpty() && mekanismJarSha256 != null && !expectedHash.equalsIgnoreCase(mekanismJarSha256))
            throw new IllegalStateException("Loaded Mekanism jar hash differs from the pinned item coverage evidence: expected="
                    + expectedHash + ", actual=" + mekanismJarSha256);
    }

    private static Map<String, String> compiledMainFiles() {
        var files = new LinkedHashMap<String, String>();
        files.putAll(CLASS_HASHES);
        RESOURCE_HASHES.forEach((path, hash) -> files.put(path.startsWith("/") ? path.substring(1) : path, hash));
        return Map.copyOf(files);
    }

    private static String hashMekanismJar() throws Exception {
        var fileInfo = net.neoforged.fml.ModList.get().getModFileById(ITEM_NAMESPACE);
        if (fileInfo == null || fileInfo.getFile() == null)
            throw new IllegalStateException("NeoForge did not expose the loaded Mekanism mod file");
        Path path = fileInfo.getFile().getFilePath();
        if (path == null || !Files.isRegularFile(path))
            throw new IllegalStateException("Loaded Mekanism mod file is not a regular jar path: " + path);
        mekanismJarPath = path.toAbsolutePath().normalize().toString();
        return hashPath(path);
    }

    private static String hashResource(String resource) throws Exception {
        try (InputStream stream = ItemCoverageValidation.class.getResourceAsStream(resource)) {
            return stream == null ? null : sha256(stream);
        }
    }

    private static String hashPath(Path path) throws Exception {
        try (InputStream stream = Files.newInputStream(path)) {
            return sha256(stream);
        }
    }

    private static String sha256(InputStream stream) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private record ItemExpectation(String id, String group, List<String> ponder, String description,
                                   NativeTooltip nativeTooltip, String exclusion) {}
    private record NativeTooltip(String kind, String translationKey) {}
}
