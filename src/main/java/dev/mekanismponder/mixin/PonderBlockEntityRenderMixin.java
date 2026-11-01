package dev.mekanismponder.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mekanismponder.client.PonderRenderGuard;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Respects Mekanism's renderer eligibility only for model-only blocks in our Ponder scenes. */
@Mixin(value = WorldSectionElementImpl.class, remap = false)
public abstract class PonderBlockEntityRenderMixin {
    @WrapOperation(method = "renderBlockEntities", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", remap = true))
    private void mekanismponder$renderOnlyWhenEligible(BlockEntityRenderer<?> renderer, BlockEntity blockEntity, float partialTick,
          PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, Operation<Void> original) {
        if (PonderRenderGuard.shouldRender(renderer, blockEntity)) {
            original.call(renderer, blockEntity, partialTick, poseStack, buffers, light, overlay);
        }
    }
}
