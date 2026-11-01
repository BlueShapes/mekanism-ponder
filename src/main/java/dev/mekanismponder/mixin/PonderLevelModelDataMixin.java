package dev.mekanismponder.mixin;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Supplies the model data of virtual block entities to NeoForge's schematic renderer. */
@Mixin(value = PonderLevel.class, remap = false)
public abstract class PonderLevelModelDataMixin {
    @Unique
    public ModelData getModelData(BlockPos position) {
        return mekanismponder$modelDataAt((PonderLevel) (Object) this, position);
    }

    @Unique
    private static ModelData mekanismponder$modelDataAt(PonderLevel level, BlockPos position) {
        var blockEntity = level.getBlockEntity(position);
        if (blockEntity == null) return ModelData.EMPTY;
        var modelData = blockEntity.getModelData();
        return modelData == null ? ModelData.EMPTY : modelData;
    }
}
