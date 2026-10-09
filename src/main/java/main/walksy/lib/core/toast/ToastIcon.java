package main.walksy.lib.core.toast;

import main.walksy.lib.api.hud.HudElement;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.texture.Icons;
import main.walksy.lib.core.render.texture.ItemModels;
import main.walksy.lib.core.render.texture.ListIcons;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.resources.Identifier;

@FunctionalInterface
public interface ToastIcon {

    void extract(WLGraphics2D graphics, float x, float y, float size, int tint, float alpha, int layer);

    static ToastIcon glyph(final Icons.Icon icon) {
        return (graphics, x, y, size, tint, alpha, layer) -> icon.extract(graphics, x, y, size, ColorUtils.withAlpha(tint, alpha));
    }

    static ToastIcon glyph(final Icons.Icon icon, final int argb) {
        return (graphics, x, y, size, tint, alpha, layer) -> icon.extract(graphics, x, y, size, ColorUtils.withAlpha(argb, alpha));
    }

    static ToastIcon texture(final Identifier id, final int textureSize) {
        return (graphics, x, y, size, tint, alpha, layer) -> graphics.blit(id, x, y, 0f, 0f, size, size, textureSize, textureSize, textureSize, textureSize, ColorUtils.withAlpha(0xFFFFFFFF, alpha));
    }

    static ToastIcon item(final String itemId) {
        return (graphics, x, y, size, tint, alpha, layer) -> {
            if (!ItemModels.extractPlain(graphics, itemId, x, y, size, layer, alpha)) {
                ListIcons.extractItem(graphics, itemId, x, y, size, alpha);
            }
        };
    }

    static ToastIcon hud(final HudElement element) {
        return (graphics, x, y, size, tint, alpha, layer) -> {
            final float width = Math.max(1f, element.width(1f));
            final float height = Math.max(1f, element.height(1f));
            final float scale = Math.min(size / width, size / height);
            element.render(graphics, x + (size - width * scale) / 2f, y + (size - height * scale) / 2f, scale, false);
        };
    }
}
