package dev.mekanismponder.client;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

final class GuideProgress {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("mekanismponder-progress.json");
    private static final Set<String> COMPLETED = load();

    private GuideProgress() {}

    private static Set<String> load() {
        if (!Files.isRegularFile(FILE)) return new HashSet<>();
        try (var reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            var ids = new Gson().fromJson(reader, String[].class);
            if (ids == null) return new HashSet<>();
            var known = GuideBook.allLessons().stream().map(GuideBook.Lesson::id).collect(java.util.stream.Collectors.toSet());
            var completed = new HashSet<>(Arrays.asList(ids));
            completed.retainAll(known);
            return completed;
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Could not read Mekanism Ponder progress: {}", FILE, exception);
            return new HashSet<>();
        }
    }

    static boolean completed(String id) { return COMPLETED.contains(id); }

    static void complete(String id) {
        if (!COMPLETED.add(id)) return;
        try {
            Files.createDirectories(FILE.getParent());
            var pending = FILE.resolveSibling("mekanismponder-progress.json.tmp");
            Files.writeString(pending, new Gson().toJson(COMPLETED.stream().sorted().toList()), StandardCharsets.UTF_8);
            try {
                Files.move(pending, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(pending, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            LogUtils.getLogger().warn("Could not save Mekanism Ponder progress: {}", FILE, exception);
        }
    }
}
