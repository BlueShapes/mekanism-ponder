package dev.mekanismponder.mixin;

import dev.mekanismponder.client.GuideBook;
import dev.mekanismponder.client.GuidePonderScreen;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adapts only Mekanism Ponder entries to the ordered guide course. */
@Mixin(value = PonderUI.class, remap = false)
public abstract class PonderUIFactoryMixin {
    @Inject(method = "of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/ponder/foundation/ui/PonderUI;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void mekanismponder$adaptItem(ItemStack stack, CallbackInfoReturnable<PonderUI> cir) {
        PonderUI original = cir.getReturnValue();
        PonderUI adapted = mekanismponder$adapt(original, stack, null);
        if (adapted != original) cir.setReturnValue(adapted);
    }

    @Inject(method = "of(Lnet/minecraft/world/item/ItemStack;Lnet/createmod/ponder/foundation/PonderTag;)Lnet/createmod/ponder/foundation/ui/PonderUI;",
            at = @At("RETURN"), cancellable = true, remap = false)
    private static void mekanismponder$adaptTaggedItem(ItemStack stack, PonderTag tag,
                                                        CallbackInfoReturnable<PonderUI> cir) {
        PonderUI original = cir.getReturnValue();
        PonderUI adapted = mekanismponder$adapt(original, stack, tag);
        if (adapted != original) cir.setReturnValue(adapted);
    }

    @Unique
    private static PonderUI mekanismponder$adapt(PonderUI original, ItemStack stack, PonderTag tag) {
        if (original == null || original.getClass() != PonderUI.class || stack == null || stack.isEmpty()) return original;

        var scenes = ((PonderUIAccessor) original).mekanismponder$getScenes();
        if (scenes.isEmpty() || scenes.stream().anyMatch(scene -> !mekanismponder$isGuideScene(scene))) return original;

        String chapter = null;
        if (tag != null) {
            if (!tag.getId().getNamespace().equals("mekanismponder")) return original;
            chapter = tag.getId().getPath();
        }

        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        GuideBook.Lesson lesson = GuideBook.firstLessonForItem(itemId, chapter);
        if (lesson == null) return original;
        return new GuidePonderScreen(lesson, stack);
    }

    @Unique
    private static boolean mekanismponder$isGuideScene(PonderScene scene) {
        return scene.getId().getNamespace().equals("mekanismponder");
    }
}
