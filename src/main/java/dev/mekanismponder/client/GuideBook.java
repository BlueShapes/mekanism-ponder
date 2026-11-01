package dev.mekanismponder.client;

import com.google.gson.Gson;
import dev.mekanismponder.GuideResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validated lesson data shared by the native Ponder course, subtitles and scenes. */
public final class GuideBook {
    private static final String LESSONS_RESOURCE = "/assets/mekanismponder/guide/lessons.json";

    private GuideBook() {}

    private static final class Holder {
        private static final List<Lesson> LESSONS = load();
    }

    public static List<Lesson> allLessons() { return Holder.LESSONS; }

    public static List<Lesson> lessons() {
        Set<String> loadedMods = net.neoforged.fml.ModList.get().getMods().stream()
                .map(mod -> mod.getModId()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        return lessonsForMods(loadedMods);
    }

    /** Returns the first course lesson associated with an item, optionally restricted to one chapter. */
    public static Lesson firstLessonForItem(ResourceLocation item, String chapter) {
        String itemId = Objects.requireNonNull(item).toString();
        return lessons().stream()
                .filter(lesson -> chapter == null || lesson.chapter().equals(chapter))
                .filter(lesson -> lesson.icon().equals(itemId) || lesson.components().contains(itemId))
                .findFirst().orElse(null);
    }

    public static List<Lesson> lessonsForMods(Set<String> loadedMods) {
        Objects.requireNonNull(loadedMods);
        return allLessons().stream().filter(lesson -> loadedMods.containsAll(lesson.requiresMods())).toList();
    }

    private static List<Lesson> load() {
        try {
            var lessons = readLessons(LESSONS_RESOURCE);
            if (lessons.isEmpty()) throw new IllegalStateException("Mekanism lesson catalogue is empty");
            var ids = new HashSet<String>();
            for (var lesson : lessons) {
                if (!ids.add(lesson.id())) throw new IllegalStateException("Duplicate lesson ID: " + lesson.id());
            }
            return lessons;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load Mekanism Ponder lessons", exception);
        }
    }

    private static List<Lesson> readLessons(String path) throws Exception {
        var stream = Objects.requireNonNull(GuideBook.class.getResourceAsStream(path), "Missing lesson catalogue: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Lesson[] lessons = new Gson().fromJson(reader, Lesson[].class);
            if (lessons == null) throw new IllegalStateException("Lesson catalogue is not a JSON array: " + path);
            return List.copyOf(Arrays.asList(lessons));
        }
    }

    public record Localized(String ja, String en) {
        public Localized {
            if (ja == null || ja.isBlank() || en == null || en.isBlank())
                throw new IllegalArgumentException("Both Japanese and English text are required");
        }
    }

    public record Node(String id, String block, List<Integer> pos, Map<String, String> state, String nbt) {
        public Node {
            if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid node ID: " + id);
            GuideResources.parse(block);
            pos = List.copyOf(Objects.requireNonNull(pos, "Node position"));
            if (pos.size() != 3 || pos.get(0) < 0 || pos.get(0) > 8 || pos.get(1) < 1
                    || pos.get(1) > 4 || pos.get(2) < 0 || pos.get(2) > 8)
                throw new IllegalArgumentException("Node outside the 9 x 5 x 9 stage: " + id);
            state = state == null ? Map.of() : Map.copyOf(state);
            nbt = nbt == null ? "{}" : nbt;
            GuideScenes.parseNbt(nbt);
        }

        public BlockPos position() { return new BlockPos(pos.get(0), pos.get(1), pos.get(2)); }
    }

    public record Flow(String from, String to, String item) {
        public Flow {
            Objects.requireNonNull(from, "Flow source");
            Objects.requireNonNull(to, "Flow destination");
            if (from.equals(to)) throw new IllegalArgumentException("Flow endpoints must differ");
            GuideResources.parse(item);
        }
    }

    public record Callout(String item, String anchor, String action, String pointing, List<Double> offset, boolean sneak) {
        public Callout {
            GuideResources.parse(item);
            if (anchor == null || !anchor.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid callout anchor: " + anchor);
            action = action == null ? "none" : action;
            if (!List.of("none", "right_click", "left_click", "scroll").contains(action))
                throw new IllegalArgumentException("Invalid callout action: " + action);
            pointing = pointing == null ? "down" : pointing.toLowerCase(Locale.ROOT);
            if (!List.of("up", "down", "left", "right").contains(pointing))
                throw new IllegalArgumentException("Invalid callout direction: " + pointing);
            offset = offset == null ? List.of(0.0, 0.25, 0.0) : List.copyOf(offset);
            if (offset.size() != 3 || offset.stream().anyMatch(value -> !Double.isFinite(value) || Math.abs(value) > 2))
                throw new IllegalArgumentException("Invalid callout offset: " + offset);
            if (sneak && action.equals("none")) throw new IllegalArgumentException("Sneak requires a callout action");
        }
    }

    public record Step(Localized text, List<String> show, List<String> focus, List<String> hide,
                       Flow flow, boolean warning, Map<String, String> nbt, List<Callout> callouts) {
        public Step {
            Objects.requireNonNull(text);
            show = show == null ? List.of() : List.copyOf(show);
            focus = focus == null ? List.of() : List.copyOf(focus);
            hide = hide == null ? List.of() : List.copyOf(hide);
            nbt = nbt == null ? Map.of() : Map.copyOf(nbt);
            nbt.values().forEach(GuideScenes::parseNbt);
            callouts = callouts == null ? List.of() : List.copyOf(callouts);
            if (callouts.size() > 3) throw new IllegalArgumentException("Too many callouts in one step");
            validateUnique(show, "show");
            validateUnique(focus, "focus");
            validateUnique(hide, "hide");
            if (show.stream().anyMatch(hide::contains)) throw new IllegalArgumentException("A step cannot show and hide the same node");
        }
    }

    public record Lesson(String id, String chapter, Localized title, Localized summary, String icon,
                         List<String> requiresMods, List<String> components, List<Node> nodes, List<Step> steps) {
        public Lesson {
            if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid lesson ID: " + id);
            if (chapter == null || GuideChapter.fromId(chapter) == null)
                throw new IllegalArgumentException("Unknown chapter for lesson " + id + ": " + chapter);
            Objects.requireNonNull(title);
            Objects.requireNonNull(summary);
            GuideResources.parse(icon);
            requiresMods = requiresMods == null ? List.of() : List.copyOf(requiresMods);
            if (requiresMods.stream().anyMatch(mod -> mod == null || !mod.matches("[a-z0-9_.-]+")))
                throw new IllegalArgumentException("Invalid required mod ID for lesson: " + id);
            components = components == null ? List.of() : List.copyOf(components);
            components.forEach(GuideResources::parse);
            nodes = List.copyOf(Objects.requireNonNull(nodes, "Lesson nodes"));
            steps = List.copyOf(Objects.requireNonNull(steps, "Lesson steps"));
            if (nodes.isEmpty() || steps.isEmpty()) throw new IllegalArgumentException("Empty lesson: " + id);

            var nodeIds = new HashSet<String>();
            var positions = new HashSet<BlockPos>();
            for (var node : nodes) {
                if (!nodeIds.add(node.id())) throw new IllegalArgumentException("Duplicate node ID in " + id + ": " + node.id());
                if (!positions.add(node.position())) throw new IllegalArgumentException("Duplicate node position in " + id + ": " + node.position());
            }
            var componentIds = new HashSet<String>();
            for (var component : components) {
                if (!componentIds.add(component)) throw new IllegalArgumentException("Duplicate component in " + id + ": " + component);
            }

            var visible = new HashSet<String>();
            for (var step : steps) {
                for (var nodeId : step.show()) requireNode(nodes, nodeId);
                for (var nodeId : step.focus()) requireNode(nodes, nodeId);
                for (var nodeId : step.hide()) requireNode(nodes, nodeId);
                for (var nodeId : step.nbt().keySet()) requireNode(nodes, nodeId);
                if (step.flow() != null) {
                    requireNode(nodes, step.flow().from());
                    requireNode(nodes, step.flow().to());
                }
                visible.removeAll(step.hide());
                visible.addAll(step.show());
                for (var node : step.focus()) {
                    if (!visible.contains(node)) throw new IllegalArgumentException("Step focuses a hidden node: " + id + "/" + node);
                }
                for (var callout : step.callouts()) {
                    requireNode(nodes, callout.anchor());
                    if (!visible.contains(callout.anchor()))
                        throw new IllegalArgumentException("Callout points to a hidden node: " + id + "/" + callout.anchor());
                }
            }
        }

        public Node node(String nodeId) { return requireNode(nodes, nodeId); }

        private static Node requireNode(List<Node> nodes, String id) {
            return nodes.stream().filter(node -> node.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown scene node: " + id));
        }
    }

    private static void validateUnique(List<String> values, String name) {
        if (values.stream().anyMatch(value -> value == null || !value.matches("[a-z0-9_]+"))
                || new HashSet<>(values).size() != values.size())
            throw new IllegalArgumentException("Invalid or duplicate node in " + name + ": " + values);
    }
}
