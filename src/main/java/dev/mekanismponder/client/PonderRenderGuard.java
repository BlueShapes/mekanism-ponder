package dev.mekanismponder.client;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Tracks only this guide's Ponder levels that contain a model-only Mekanism block entity. */
public final class PonderRenderGuard {
    private static final Set<ResourceLocation> MODEL_ONLY_TYPES = Set.of(
            ResourceLocation.fromNamespaceAndPath("mekanism", "thermal_evaporation_controller"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "teleporter")
    );
    private static final Set<PonderLevel> GUIDE_LEVELS = Collections.newSetFromMap(new WeakHashMap<>());

    private PonderRenderGuard() {}

    public static boolean requiresModelOnlyRendering(String blockId) {
        return MODEL_ONLY_TYPES.contains(ResourceLocation.parse(blockId));
    }

    public static synchronized void markGuideScene(PonderLevel level) {
        GUIDE_LEVELS.add(level);
    }

    public static synchronized boolean shouldRender(BlockEntityRenderer<?> renderer, BlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof PonderLevel level) || !GUIDE_LEVELS.contains(level)) return true;
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType());
        if (!MODEL_ONLY_TYPES.contains(type)) return true;
        Vec3 camera = Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getPosition();
        return shouldRenderUnchecked(renderer, blockEntity, camera);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean shouldRenderUnchecked(BlockEntityRenderer renderer, BlockEntity blockEntity, Vec3 camera) {
        return renderer.shouldRender(blockEntity, camera);
    }
}
