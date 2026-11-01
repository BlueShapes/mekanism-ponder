package dev.mekanismponder.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.mekanismponder.GuideResources;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.ui.PonderTagIndexScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuideCommands {
    private GuideCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher, "ponder-mekanism", false);
        registerRoot(dispatcher, "mekanismponder", true);
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name, boolean legacy) {
        var root = Commands.literal(name)
                .executes(context -> openIndex())
                .then(Commands.literal("search")
                        .then(Commands.argument("query", StringArgumentType.greedyString())
                                .executes(context -> openSearch(StringArgumentType.getString(context, "query")))))
                .then(Commands.literal("item")
                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        itemSuggestions(), builder))
                                .executes(context -> openItem(StringArgumentType.getString(context, "item")))));
        if (legacy) {
            root.then(Commands.literal("lesson")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                    GuideBook.lessons().stream().map(GuideBook.Lesson::id), builder))
                            .executes(context -> openLesson(StringArgumentType.getString(context, "id")))));
        }
        root.then(Commands.argument("chapter", StringArgumentType.greedyString())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(chapterSuggestions(), builder))
                .executes(context -> openChapter(StringArgumentType.getString(context, "chapter"))));
        dispatcher.register(root);
    }

    private static int openIndex() throws CommandSyntaxException {
        requireWorld();
        ScreenOpener.open(new PonderTagIndexScreen());
        return 1;
    }

    private static int openChapter(String query) throws CommandSyntaxException {
        requireWorld();
        List<GuideChapter> chapters = GuideChapter.active(GuideBook.lessons());
        GuideChapter selected = parseChapter(query, chapters);
        if (selected == null) throw new DynamicCommandExceptionType(value ->
                Component.translatable("mekanismponder.command.unknown_chapter", value)).create(query);
        GuideBook.Lesson lesson = GuideBook.lessons().stream()
                .filter(value -> value.chapter().equals(selected.id())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Active chapter has no lessons: " + selected.id()));
        GuidePonderScreen.open(lesson);
        return 1;
    }

    private static int openSearch(String query) throws CommandSyntaxException {
        requireWorld();
        GuideBook.Lesson lesson = findLesson(query);
        if (lesson == null) throw new DynamicCommandExceptionType(value ->
                Component.translatable("mekanismponder.command.unknown", value)).create(query);
        GuidePonderScreen.open(lesson);
        return 1;
    }

    /** Searches logical translated text, never Arabic presentation forms. */
    public static GuideBook.Lesson findLesson(String query) {
        if (query.isBlank()) return null;
        for (var lesson : GuideBook.lessons()) {
            if (containsIgnoreCase(Component.translatable("mekanismponder.ponder." + lesson.id() + ".header").getString(), query)
                    || containsIgnoreCase(Component.translatable("mekanismponder.lesson." + lesson.id() + ".summary").getString(), query))
                return lesson;
            for (int index = 0; index < lesson.steps().size(); index++)
                if (containsIgnoreCase(Component.translatable("mekanismponder.ponder.shared." + lesson.id() + "_" + index).getString(), query))
                    return lesson;
        }
        return null;
    }

    private static GuideChapter parseChapter(String query, List<GuideChapter> chapters) {
        String value = query.trim();
        try {
            int index = Integer.parseInt(value);
            return index > 0 && index <= chapters.size() ? chapters.get(index - 1) : null;
        } catch (NumberFormatException ignored) {
            // Chapter IDs and both localized and source titles are also accepted.
        }
        return chapters.stream().filter(chapter -> chapter.id().equalsIgnoreCase(value)
                || chapter.englishName().equalsIgnoreCase(value)
                || chapter.label().getString().equalsIgnoreCase(value)).findFirst().orElse(null);
    }

    private static int openItem(String query) throws CommandSyntaxException {
        requireWorld();
        ResolvedLesson resolved = resolveItem(query);
        if (resolved == null) throw new DynamicCommandExceptionType(value ->
                Component.translatable("mekanismponder.command.unknown_item", value)).create(query);
        GuidePonderScreen.open(resolved.lesson(), resolved.stack());
        return 1;
    }

    private static ResolvedLesson resolveItem(String query) {
        String value = query.trim();
        ResourceLocation exactId = ResourceLocation.tryParse(value);
        if (exactId != null && BuiltInRegistries.ITEM.containsKey(exactId)) {
            var lesson = GuideBook.firstLessonForItem(exactId, null);
            if (lesson != null) return new ResolvedLesson(lesson, new ItemStack(BuiltInRegistries.ITEM.get(exactId)));
        }

        String normalized = value.toLowerCase(Locale.ROOT).replace(' ', '_');
        for (var lesson : GuideBook.lessons()) {
            for (String itemId : lessonItems(lesson)) {
                ResourceLocation id = GuideResources.parse(itemId);
                var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                if (item == null) continue;
                String displayName = item.getName(new ItemStack(item)).getString();
                if (id.getPath().contains(normalized) || containsIgnoreCase(displayName, value))
                    return new ResolvedLesson(lesson, new ItemStack(item));
            }
        }
        return null;
    }

    private static List<String> lessonItems(GuideBook.Lesson lesson) {
        var items = new ArrayList<String>(lesson.components().size() + 1);
        items.add(lesson.icon());
        items.addAll(lesson.components());
        return items;
    }

    private static List<String> itemSuggestions() {
        return GuideBook.lessons().stream().flatMap(lesson -> lessonItems(lesson).stream()).distinct().toList();
    }

    private static boolean containsIgnoreCase(String text, String query) {
        Locale locale = Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().replace('_', '-'));
        return GuideSearchText.contains(GuideTextLayout.searchableCaption(text),
                GuideTextLayout.searchableCaption(query), locale);
    }

    private static List<String> chapterSuggestions() {
        var suggestions = new ArrayList<String>();
        List<GuideChapter> chapters = GuideChapter.active(GuideBook.lessons());
        for (int index = 0; index < chapters.size(); index++) {
            GuideChapter chapter = chapters.get(index);
            suggestions.add(Integer.toString(index + 1));
            suggestions.add(chapter.id());
            suggestions.add(chapter.englishName());
            suggestions.add(chapter.label().getString());
        }
        return suggestions;
    }

    private static int openLesson(String id) throws CommandSyntaxException {
        requireWorld();
        var lesson = GuideBook.lessons().stream().filter(value -> value.id().equals(id)).findFirst()
                .orElseThrow(() -> new DynamicCommandExceptionType(value ->
                        Component.translatable("mekanismponder.command.unknown", value)).create(id));
        GuidePonderScreen.open(lesson);
        return 1;
    }

    private static void requireWorld() throws CommandSyntaxException {
        if (Minecraft.getInstance().level == null)
            throw new SimpleCommandExceptionType(Component.translatable("mekanismponder.command.requires_world")).create();
    }

    private record ResolvedLesson(GuideBook.Lesson lesson, ItemStack stack) {}
}
