package main.walksy.lib.core.render.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import main.walksy.lib.api.config.value.ItemSpec;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.GuiContext;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLBlitRenderState;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemModels {
    public static final float NATIVE_SIZE = 16f;
    private static final Map<String, ItemStack> PLAIN_STACKS = new HashMap<>();
    private static final List<Faded> FADED = new ArrayList<>();
    private static final Map<String, TrackingItemStackRenderState> RENDER_STATES = new HashMap<>();
    private static int demandedSlotPx;
    private static int liveSlotPx;
    private static final int MAX_SLOT_PX = 256;

    private ItemModels() {
    }

    public static boolean extractPlain(final WLGraphics2D graphics, final String itemId, final float x, final float y, final float size, final int layer) {
        return extractPlain(graphics, itemId, x, y, size, layer, 1f);
    }

    public static boolean extractPlain(final WLGraphics2D graphics, final String itemId, final float x, final float y, final float size, final int layer, final float alpha) {
        if (itemId == null || alpha <= 0.004f) {
            return false;
        }
        final ItemStack stack = PLAIN_STACKS.computeIfAbsent(itemId, id -> new ItemSpec(id).toItemStack());
        if (stack.isEmpty()) {
            PLAIN_STACKS.remove(itemId);
            return false;
        }
        if (currentChannel() != Channel.DIRECT || alpha < 0.999f) {
            return queue(graphics, "plain#" + itemId, stack, x, y, size, layer, alpha, List.of());
        }
        if (PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
        demand(graphics, size);
        extractStack(graphics, stack, x, y, size);
        return true;
    }

    public static boolean extract(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final int layer) {
        if (spec == null) {
            return false;
        }
        final ItemStack stack = spec.toItemStack();
        if (stack.isEmpty()) {
            return false;
        }
        if (PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
        demand(graphics, size);
        extractStack(graphics, stack, x, y, size);
        return true;
    }

    public static void extractOrFallback(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final int layer, final float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        if (alpha >= 0.999f && extract(graphics, spec, x, y, size, layer)) {
            return;
        }
        if (queueFaded(graphics, spec, x, y, size, layer, alpha)) {
            return;
        }
        ListIcons.extractItem(graphics, spec == null ? null : spec.getItemId(), x, y, size, alpha);
        extractDecorations(graphics, spec, x, y, size, alpha);
    }

    public static void extractFaded(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final int layer, final float alpha) {
        if (alpha >= 0.999f) {
            extractOrFallback(graphics, spec, x, y, size, layer, alpha);
            return;
        }
        if (alpha <= 0.004f || spec == null) {
            return;
        }
        if (queueFaded(graphics, spec, x, y, size, layer, alpha)) {
            return;
        }
        ListIcons.extractItem(graphics, spec.getItemId(), x, y, size, alpha);
        extractDecorations(graphics, spec, x, y, size, alpha);
    }

    private static void extractDecorations(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final float alpha) {
        if (!hasDecorations(spec) || alpha <= 0.004f) {
            return;
        }
        final boolean damaged = spec.isDamageable() && spec.getDamage() > 0;
        final boolean counted = spec.getCount() > 1;
        final Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / NATIVE_SIZE, size / NATIVE_SIZE);
        try {
            if (damaged) {
                final int max = Math.max(1, spec.maxDamage());
                final float left = Math.max(0f, Math.min(1f, (max - spec.getDamage()) / (float) max));
                final int filled = Math.round(left * 13f);
                final int color = 0xFF000000 | (Mth.hsvToRgb(left / 3f, 1f, 1f) & 0xFFFFFF);
                graphics.fill(2f, 13f, 15f, 15f, ColorUtils.withAlpha(0xFF000000, alpha), null);
                graphics.fill(2f, 13f, 2f + filled, 14f, ColorUtils.withAlpha(color, alpha), null);
            }
            if (counted) {
                final String count = String.valueOf(spec.getCount());
                VanillaFont.extract(graphics, count, 19f - 2f - VanillaFont.width(count), 6f + 3f, ColorUtils.withAlpha(0xFFFFFFFF, alpha), true);
            }
        } finally {
            pose.popMatrix();
        }
    }

    private static boolean hasDecorations(final ItemSpec spec) {
        return spec != null && (spec.getCount() > 1 || (spec.isDamageable() && spec.getDamage() > 0));
    }

    private static int premultipliedTint(final float alpha) {
        final int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (a << 16) | (a << 8) | a;
    }

    private enum Channel { DIRECT, ROW, POPOVER, TOP }

    private record Faded(TrackingItemStackRenderState state, Matrix3x2f pose, float x, float y, float size, int tint, WLRect scissor, Channel channel, List<WLElement> decorations) {}

    private static void demand(final WLGraphics2D graphics, final float size) {
        final Matrix3x2f pose = graphics.pose();
        final float poseScale = DevicePixels.isAxisAligned(pose) ? Math.max(Math.abs(pose.m00()), Math.abs(pose.m11())) : 1f;
        final float device = size * poseScale * Math.max(DevicePixels.perLogicalX(), DevicePixels.perLogicalY());
        demandedSlotPx = Math.max(demandedSlotPx, Math.round(device));
    }

    private static int quantise(final int wanted) {
        if (wanted <= 0) {
            return 0;
        }
        int rounded = 16;
        while (rounded < wanted && rounded < MAX_SLOT_PX) {
            rounded <<= 1;
        }
        return rounded;
    }

    public static int demandedAtlasSlotSize() {
        final int rounded = quantise(demandedSlotPx);
        demandedSlotPx = 0;
        liveSlotPx = rounded;
        return rounded;
    }

    public static boolean consumeAtlasSlotSizeChanged() {
        final int wanted = quantise(demandedSlotPx);
        if (wanted == 0 || wanted == liveSlotPx) {
            return false;
        }
        liveSlotPx = wanted;
        return true;
    }

    private static boolean queueFaded(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final int layer, final float alpha) {
        if (spec == null) {
            return false;
        }
        if (currentChannel() == Channel.DIRECT && PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
        final ItemStack stack = spec.toItemStack();
        if (stack.isEmpty()) {
            return false;
        }
        final List<WLElement> decorations;
        if (hasDecorations(spec)) {
            decorations = new ArrayList<>(4);
            WLGraphics2D.capture(decorations, () -> extractDecorations(graphics, spec, x, y, size, alpha));
        } else {
            decorations = List.of();
        }
        return queue(graphics, signatureOf(spec), stack, x, y, size, layer, alpha, decorations);
    }

    private static boolean queue(final WLGraphics2D graphics, final String key, final ItemStack stack, final float x, final float y, final float size, final int layer, final float alpha, final List<WLElement> decorations) {
        final Channel channel = currentChannel();
        if (channel == Channel.DIRECT && PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
        final TrackingItemStackRenderState state = renderStateFor(key, stack);
        if (state == null) {
            return false;
        }
        demand(graphics, size);
        FADED.add(new Faded(state, new Matrix3x2f(graphics.pose()), x, y, size, premultipliedTint(alpha), graphics.scissor(), channel, decorations));
        return true;
    }

    private static Channel currentChannel() {
        if (WLRenderSystem.contentStratum().isTopActive()) {
            return Channel.TOP;
        }
        if (WLRenderSystem.contentStratum().isPopoverActive()) {
            return Channel.POPOVER;
        }
        return WLRenderSystem.contentStratum().isActive() ? Channel.ROW : Channel.DIRECT;
    }

    private static String signatureOf(final ItemSpec spec) {
        return spec.getItemId() + "#" + spec.getDamage() + "#" + spec.getEnchantments();
    }

    private static TrackingItemStackRenderState renderStateFor(final String key, final ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        final TrackingItemStackRenderState cached = RENDER_STATES.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            final TrackingItemStackRenderState built = new TrackingItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(built, stack, ItemDisplayContext.GUI, null, null, 0);
            if (RENDER_STATES.size() > 512) {
                RENDER_STATES.clear();
            }
            RENDER_STATES.put(key, built);
            return built;
        } catch (final RuntimeException notReady) {
            return null;
        }
    }

    public static boolean noFadedQueued() {
        return FADED.isEmpty();
    }

    public static Set<Object> withFadedIdentities(final Set<Object> base) {
        if (FADED.isEmpty()) {
            return base;
        }
        final Set<Object> merged = new HashSet<>(base);
        for (final Faded item : FADED) {
            final Object identity = item.state().getModelIdentity();
            if (identity != null) {
                merged.add(identity);
            }
        }
        return merged;
    }

    public static void flushFaded(final GuiItemAtlas atlas, final GuiRenderState renderState) {
        if (FADED.isEmpty()) {
            return;
        }
        try {
            if (atlas == null || renderState == null) {
                return;
            }
            final var sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
            final List<Faded> drawn = new ArrayList<>(FADED.size());
            for (final Faded item : FADED) {
                final GuiItemAtlas.SlotView slot;
                try {
                    slot = atlas.getOrUpdate(item.state());
                } catch (final RuntimeException failed) {
                    continue;
                }
                if (slot == null) {
                    continue;
                }
                emit(renderState, item.channel(), new WLBlitRenderState(WLElementKind.PREMULTIPLIED, WLTexture.wrap(slot.textureView(), sampler), item.pose(), item.x(), item.y(), item.x() + item.size(), item.y() + item.size(), slot.u0(), slot.u1(), slot.v0(), slot.v1(), item.tint(), item.scissor()));
                drawn.add(item);
            }
            for (final Faded item : drawn) {
                for (final WLElement decoration : item.decorations()) {
                    emit(renderState, item.channel(), decoration);
                }
            }
        } finally {
            FADED.clear();
        }
    }

    private static void emit(final GuiRenderState renderState, final Channel channel, final WLElement element) {
        switch (channel) {
            case ROW -> WLRenderSystem.contentStratum().add(element);
            case POPOVER -> WLRenderSystem.contentStratum().addPopover(element);
            case TOP -> WLRenderSystem.contentStratum().addTop(element);
            case DIRECT -> renderState.addGuiElement(GuiContext.vanilla(element));
        }
    }

    private static void extractStack(final WLGraphics2D graphics, final ItemStack stack, final float x, final float y, final float size) {
        final Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(size / NATIVE_SIZE, size / NATIVE_SIZE);
        graphics.item(stack, 0, 0);
        graphics.itemDecorations(Minecraft.getInstance().font, stack, 0, 0);
        pose.popMatrix();
    }
}
