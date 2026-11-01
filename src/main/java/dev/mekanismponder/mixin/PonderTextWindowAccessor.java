package dev.mekanismponder.mixin;

import net.createmod.ponder.foundation.element.TextWindowElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

/** Used only on text elements created by this guide, without changing native rendering. */
@Mixin(value = TextWindowElement.class, remap = false)
public interface PonderTextWindowAccessor {
    @Accessor("textGetter")
    void mekanismponder$setTextGetter(Supplier<String> getter);
}
