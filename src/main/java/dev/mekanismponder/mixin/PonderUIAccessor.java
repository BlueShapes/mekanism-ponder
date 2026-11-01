package dev.mekanismponder.mixin;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Minimal access to Ponder's course, active scene position and selected subject. */
@Mixin(value = PonderUI.class, remap = false)
public interface PonderUIAccessor {
    @Accessor("index") void mekanismponder$setIndex(int index);
    @Accessor("lazyIndex") LerpedFloat mekanismponder$getLazyIndex();
    @Accessor("scenes") List<PonderScene> mekanismponder$getScenes();
    @Accessor("tags") List<PonderTag> mekanismponder$getTags();
    @Accessor("tagButtons") List<PonderButton> mekanismponder$getTagButtons();
    @Accessor("stack") void mekanismponder$setStack(ItemStack stack);
}
