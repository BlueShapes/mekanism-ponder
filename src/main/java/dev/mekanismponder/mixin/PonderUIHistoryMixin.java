package dev.mekanismponder.mixin;

import dev.mekanismponder.client.GuidePonderScreen;
import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.ponder.foundation.ui.PonderUI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep this guide's course distinct when native history compares only the subject item. */
@Mixin(value = PonderUI.class, remap = false)
public abstract class PonderUIHistoryMixin {
    @Inject(method = "isEquivalentTo", at = @At("HEAD"), cancellable = true, remap = false)
    private void mekanismponder$separateGuideCourse(NavigatableSimiScreen other, CallbackInfoReturnable<Boolean> cir) {
        if (((Object) this instanceof GuidePonderScreen) != (other instanceof GuidePonderScreen))
            cir.setReturnValue(false);
    }
}
