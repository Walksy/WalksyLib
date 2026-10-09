package main.walksy.lib.core.render.texture;

import main.walksy.lib.api.config.value.ItemSpec;
import main.walksy.lib.core.gui.modal.PopoverHost;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.preview.PreviewItems;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.HashMap;
import java.util.Map;

public final class ItemModels {
    public static final float NATIVE_SIZE = 16f;
    private static final Map<String, ItemStack> PLAIN_STACKS = new HashMap<>();

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
        if (!directChannel()) {
            return PreviewItems.submit(graphics, "plain#" + itemId, stack, x, y, size, alpha);
        }
        if (alpha < 0.999f) {
            return false;
        }
        if (PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
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
        if (!directChannel()) {
            return preview(graphics, spec, stack, x, y, size, 1f);
        }
        if (PopoverHost.anyPanelIntersects(x, y, x + size, y + size, layer)) {
            return false;
        }
        extractStack(graphics, stack, x, y, size);
        return true;
    }

    public static void extractOrFallback(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final int layer, final float alpha) {
        if (alpha <= 0.004f) {
            return;
        }
        if (alpha >= 0.999f ? extract(graphics, spec, x, y, size, layer) : previewFaded(graphics, spec, x, y, size, alpha)) {
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
        if (previewFaded(graphics, spec, x, y, size, alpha)) {
            return;
        }
        ListIcons.extractItem(graphics, spec.getItemId(), x, y, size, alpha);
        extractDecorations(graphics, spec, x, y, size, alpha);
    }

    private static boolean previewFaded(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final float alpha) {
        if (spec == null || directChannel()) {
            return false;
        }
        final ItemStack stack = spec.toItemStack();
        return !stack.isEmpty() && preview(graphics, spec, stack, x, y, size, alpha);
    }

    private static boolean preview(final WLGraphics2D graphics, final ItemSpec spec, final ItemStack stack, final float x, final float y, final float size, final float alpha) {
        if (!PreviewItems.submit(graphics, signatureOf(spec), stack, x, y, size, alpha)) {
            return false;
        }
        extractDecorations(graphics, spec, x, y, size, alpha);
        return true;
    }

    private static String signatureOf(final ItemSpec spec) {
        return spec.getItemId() + "#" + spec.getDamage() + "#" + spec.getEnchantments();
    }

    private static boolean directChannel() {
        return !WLRenderSystem.contentStratum()
            .isTopActive() && !WLRenderSystem.contentStratum()
            .isPopoverActive() && !WLRenderSystem.contentStratum()
            .isActive();
    }

    private static void extractDecorations(final WLGraphics2D graphics, final ItemSpec spec, final float x, final float y, final float size, final float alpha) {
        if (spec == null || alpha <= 0.004f) {
            return;
        }
        final boolean damaged = spec.isDamageable() && spec.getDamage() > 0;
        final boolean counted = spec.getCount() > 1;
        if (!damaged && !counted) {
            return;
        }
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
