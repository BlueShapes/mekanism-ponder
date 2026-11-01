package dev.mekanismponder.validation;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.lwjgl.glfw.GLFW;

/** Version-specific platform hooks for the validation-only source set. */
final class ValidationPlatform {
    private ValidationPlatform() {}

    static void hideWindow(Minecraft client) {
        GLFW.glfwHideWindow(client.getWindow().getWindow());
    }

    static void recoverInitialScreen(Minecraft client) {
        var overlay = client.getOverlay();
        if (overlay != null && !overlay.getClass().getSimpleName().equals("LoadingOverlay")) {
            try {
                var setter = java.util.Arrays.stream(Minecraft.class.getMethods())
                        .filter(method -> method.getName().equals("setOverlay") && method.getParameterCount() == 1)
                        .findFirst().orElseThrow(() -> new IllegalStateException("Minecraft has no overlay recovery method"));
                setter.invoke(client, new Object[]{null});
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not dismiss the startup overlay", exception);
            }
        }
        if (client.level == null && client.screen != null && !(client.screen instanceof TitleScreen))
            client.setScreen(new TitleScreen());
    }

    static Screen createOptionsScreen(Minecraft client) {
        return new OptionsScreen(null, client.options);
    }

    static boolean isOptionsScreen(Screen screen) {
        return screen instanceof OptionsScreen;
    }

    static void createFreshLevel(Minecraft client) {
        var rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
        client.createWorldOpenFlows().createFreshLevel("mekanismponder-validation-" + System.currentTimeMillis(),
                new LevelSettings("Mekanism Ponder Validation", GameType.CREATIVE, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT),
                new WorldOptions(1L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
    }

    static CompoundTag saveWithoutMetadata(BlockEntity blockEntity, RegistryAccess registryAccess) {
        return blockEntity.saveWithoutMetadata(registryAccess);
    }

    static ModelData getModelData(PonderLevel level, BlockPos position) {
        return level.getModelData(position);
    }
}
