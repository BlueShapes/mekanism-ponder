package dev.mekanismponder.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.mekanismponder.client.GuideTextLayout;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The native box must fit shaped Arabic glyphs as well as its logical wrapping. */
@Mixin(value = TextWindowElement.class, remap = false)
public abstract class PonderTextWidthMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Ljava/lang/Math;min(FF)F", ordinal = 0), remap = false)
    private float mekanismponder$keepNativeCaptionWidth(float first, float second, Operation<Float> original,
                                                        PonderScene scene, PonderUI ui, GuiGraphics graphics,
                                                        float partialTick, float fade) {
        float x = original.call(first, second);
        if (!scene.getId().getNamespace().equals("mekanismponder")) return x;
        return GuideTextLayout.constrainNearSceneX(x, ui.width);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I"), remap = false)
    private int mekanismponder$visualWidth(Font font, FormattedText text, Operation<Integer> original,
                                           PonderScene scene, PonderUI ui, GuiGraphics graphics, float partialTick, float fade) {
        int width = original.call(font, text);
        if (!scene.getId().getNamespace().equals("mekanismponder")) return width;
        return Math.max(width, font.width(Language.getInstance().getVisualOrder(text)));
    }
}
