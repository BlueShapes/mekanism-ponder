package dev.mekanismponder.client;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mekanismponder.GuideResources;
import dev.mekanismponder.mixin.PonderTextWindowAccessor;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.createmod.ponder.foundation.instruction.TextInstruction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.TreeSet;

/** Turns a lesson's model and steps into Ponder's virtual scene API. */
public final class GuideScenes {
    private static final int FLOW_TICKS = 36;
    private static final int CALLOUT_SLOT_TICKS = 72;

    private GuideScenes() {}

    public static void build(GuideBook.Lesson lesson, SceneBuilder scene, SceneBuildingUtil util) {
        if (lesson.nodes().stream().anyMatch(node -> PonderRenderGuard.requiresModelOnlyRendering(node.block()))) {
            PonderRenderGuard.markGuideScene(scene.getScene().getWorld());
        }
        scene.title(lesson.id(), lesson.title().en());
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.85f);
        for (var node : lesson.nodes()) {
            scene.world().setBlock(node.position(), state(node), false);
            applyNbt(scene, util, node, node.nbt());
        }
        scene.showBasePlate();
        scene.idle(15);
        for (int index = 0; index < lesson.steps().size(); index++) buildStep(lesson, index, scene, util);
        scene.markAsFinished();
    }

    private static void buildStep(GuideBook.Lesson lesson, int index, SceneBuilder scene, SceneBuildingUtil util) {
        var step = lesson.steps().get(index);
        scene.addKeyframe();
        step.hide().forEach(id -> scene.world().hideSection(util.select().position(lesson.node(id).position()), Direction.UP));
        step.show().forEach(id -> scene.world().showSection(util.select().position(lesson.node(id).position()), Direction.DOWN));
        step.nbt().forEach((id, nbt) -> applyNbt(scene, util, lesson.node(id), nbt));
        scene.idle(12);
        int duration = captionDuration(lesson, index);
        var outlineColor = step.warning() ? PonderPalette.RED : PonderPalette.BLUE;
        for (var id : step.focus())
            scene.overlay().showOutline(outlineColor, "focus_" + id, util.select().position(lesson.node(id).position()), duration);

        var pages = GuidePresentation.units("mekanismponder.ponder.shared." + lesson.id() + "_" + index,
                captionText(lesson, index));
        int slots = Math.max(1, step.callouts().size());
        int slotDuration = duration / slots;
        int pageDuration = duration / pages.size();
        var events = new TreeSet<Integer>();
        for (int page = 0; page < pages.size(); page++) events.add(page * pageDuration);
        for (int slot = 0; slot < slots; slot++) events.add(slot * slotDuration);
        int cursor = 0;
        for (int time : events) {
            scene.idle(time - cursor);
            cursor = time;
            if (time % pageDuration == 0 && time / pageDuration < pages.size()) {
                var element = new TextWindowElement();
                var page = pages.get(time / pageDuration);
                String displayText = GuideTextLayout.captionDisplayText(Minecraft.getInstance().font, page.text());
                ((PonderTextWindowAccessor) element).mekanismponder$setTextGetter(() -> displayText);
                scene.addInstruction(new TextInstruction(element, pageDuration));
                var caption = element.builder(scene.getScene()).colored(page.intent().palette());
                // Keep the standard title at y=19 clear even on a 320x240 GUI.
                caption.independent(138).placeNearTarget();
            }
            if (!step.callouts().isEmpty() && time % slotDuration == 0 && time / slotDuration < slots)
                callout(lesson, step.callouts().get(time / slotDuration), scene, slotDuration);
            if (time == 0 && step.flow() != null) {
                flow(lesson, step.flow(), scene);
                cursor += FLOW_TICKS;
            }
        }
        scene.idle(duration - cursor + 12);
    }

    public static int captionDuration(GuideBook.Step step) {
        return Math.max(Math.max(120, step.callouts().size() * CALLOUT_SLOT_TICKS),
                Math.max(step.text().ja().codePointCount(0, step.text().ja().length()) * 3,
                        step.text().en().codePointCount(0, step.text().en().length())));
    }

    public static int captionDuration(GuideBook.Lesson lesson, int index) {
        String caption = captionText(lesson, index);
        return Math.max(Math.max(captionDuration(lesson.steps().get(index)), caption.codePointCount(0, caption.length()) * 2),
                GuideTextLayout.captionPages(caption).size() * 120);
    }

    private static String captionText(GuideBook.Lesson lesson, int index) {
        return Component.translatable("mekanismponder.ponder.shared." + lesson.id() + "_" + index).getString();
    }

    private static void callout(GuideBook.Lesson lesson, GuideBook.Callout callout, SceneBuilder scene, int duration) {
        var node = lesson.node(callout.anchor());
        var offset = callout.offset();
        var position = Vec3.atCenterOf(node.position()).add(offset.get(0), 0.5 + offset.get(1), offset.get(2));
        var id = GuideResources.parse(callout.item());
        var item = BuiltInRegistries.ITEM.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown callout item: " + id));
        var bubble = scene.overlay().showControls(position,
                        Pointing.valueOf(callout.pointing().toUpperCase(Locale.ROOT)), duration - 12)
                .withItem(new ItemStack(item));
        switch (callout.action()) {
            case "right_click" -> bubble.rightClick();
            case "left_click" -> bubble.leftClick();
            case "scroll" -> bubble.scroll();
            default -> { }
        }
        if (callout.sneak()) bubble.whileSneaking();
    }

    private static void flow(GuideBook.Lesson lesson, GuideBook.Flow flow, SceneBuilder scene) {
        var from = Vec3.atCenterOf(lesson.node(flow.from()).position()).add(0, 0.65, 0);
        var to = Vec3.atCenterOf(lesson.node(flow.to()).position()).add(0, 0.65, 0);
        scene.overlay().showBigLine(PonderPalette.GREEN, from, to, FLOW_TICKS + 12);
        var item = BuiltInRegistries.ITEM.getOptional(GuideResources.parse(flow.item()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown flow item: " + flow.item()));
        var entity = scene.world().createItemEntity(from, Vec3.ZERO, new ItemStack(item));
        scene.world().modifyEntity(entity, value -> { value.setNoGravity(true); value.noPhysics = true; });
        for (int tick = 1; tick <= FLOW_TICKS; tick++) {
            var position = from.lerp(to, tick / (double) FLOW_TICKS);
            scene.world().modifyEntity(entity, value -> value.setPos(position));
            scene.idle(1);
        }
        scene.world().modifyEntity(entity, value -> value.discard());
    }

    private static void applyNbt(SceneBuilder scene, SceneBuildingUtil util, GuideBook.Node node, String snbt) {
        if (snbt.equals("{}")) return;
        var data = parseNbt(snbt);
        scene.world().modifyBlockEntityNBT(util.select().position(node.position()), BlockEntity.class,
                tag -> tag.merge(data.copy()), true);
    }

    public static CompoundTag parseNbt(String snbt) {
        try { return TagParser.parseTag(snbt); }
        catch (CommandSyntaxException exception) {
            throw new IllegalArgumentException("Invalid guide visual NBT: " + snbt, exception);
        }
    }

    public static BlockState state(GuideBook.Node node) {
        var id = GuideResources.parse(node.block());
        var block = BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown guide block: " + id));
        var state = block.defaultBlockState();
        for (var entry : node.state().entrySet()) {
            var property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null) throw new IllegalArgumentException("Unknown state property: " + node.block() + "/" + entry.getKey());
            state = setProperty(state, property, entry.getValue());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> property, String value) {
        return state.setValue(property, property.getValue(value)
                .orElseThrow(() -> new IllegalArgumentException("Invalid block state value: " + value)));
    }
}
