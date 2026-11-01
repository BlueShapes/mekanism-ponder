package dev.mekanismponder.mixin;

import net.createmod.ponder.foundation.ui.PonderButton;
import net.createmod.ponder.foundation.ui.PonderTagScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Fits only this guide's related items inside the existing native tag screen. */
@Mixin(value = PonderTagScreen.class, remap = false)
public abstract class PonderTagLayoutMixin extends Screen {
    @Shadow protected Rect2i itemArea;
    @Unique private List<PonderButton> mekanismponder$items = List.of();
    @Unique private int mekanismponder$page;
    @Unique private int mekanismponder$capacity;
    @Unique private int mekanismponder$columns;
    @Unique private int mekanismponder$rows;
    @Unique private Button mekanismponder$previous;
    @Unique private Button mekanismponder$next;

    protected PonderTagLayoutMixin(Component title) { super(title); }

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void mekanismponder$fitItems(CallbackInfo ci) {
        var screen = (PonderTagScreen) (Object) this;
        if (!screen.getTag().getId().getNamespace().equals("mekanismponder")) return;
        mekanismponder$items = children().stream().filter(PonderButton.class::isInstance)
                .map(PonderButton.class::cast).filter(button -> !button.getItem().isEmpty()).toList();
        mekanismponder$columns = Math.max(1, Math.min(11, (width - 32) / 36));
        mekanismponder$rows = Math.max(1, Math.min(3, (height - screen.getItemsY() - 66) / 36));
        mekanismponder$capacity = mekanismponder$columns * mekanismponder$rows;
        int pages = Math.max(1, (mekanismponder$items.size() + mekanismponder$capacity - 1) / mekanismponder$capacity);
        mekanismponder$page = Math.min(mekanismponder$page, pages - 1);
        if (pages > 1) {
            int y = screen.getItemsY() + mekanismponder$rows * 36 + 2;
            mekanismponder$previous = addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
                mekanismponder$page--;
                mekanismponder$layoutPage();
            }).bounds(16, y, 28, 20).build());
            mekanismponder$next = addRenderableWidget(Button.builder(Component.literal("›"), button -> {
                mekanismponder$page++;
                mekanismponder$layoutPage();
            }).bounds(width - 44, y, 28, 20).build());
        } else {
            mekanismponder$previous = null;
            mekanismponder$next = null;
        }
        mekanismponder$layoutPage();
    }

    @Unique
    private void mekanismponder$layoutPage() {
        int start = mekanismponder$page * mekanismponder$capacity;
        int count = Math.min(mekanismponder$capacity, mekanismponder$items.size() - start);
        int columns = Math.max(1, Math.min(mekanismponder$columns, (count + mekanismponder$rows - 1) / mekanismponder$rows));
        int rows = Math.max(1, (count + columns - 1) / columns);
        int gridWidth = columns * 36 - 8;
        int gridHeight = rows * 36 - 8;
        int y = ((PonderTagScreen) (Object) this).getItemsY();
        // Native description starts at itemsY - 10 + area.height; keep it below the last row.
        itemArea = new Rect2i(-gridWidth / 2, 0, gridWidth, gridHeight + 16);
        for (int index = 0; index < mekanismponder$items.size(); index++) {
            var button = mekanismponder$items.get(index);
            int slot = index - start;
            button.visible = slot >= 0 && slot < count;
            if (button.visible) {
                button.setX((width - gridWidth) / 2 + (slot % columns) * 36 + 4);
                button.setY(y + (slot / columns) * 36 + 4);
            }
        }
        if (mekanismponder$previous != null) {
            mekanismponder$previous.active = mekanismponder$page > 0;
            mekanismponder$next.active = start + count < mekanismponder$items.size();
        }
    }
}
