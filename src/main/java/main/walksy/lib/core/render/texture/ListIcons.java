package main.walksy.lib.core.render.texture;

import main.walksy.lib.api.config.value.ListEntries;
import main.walksy.lib.api.config.value.ListIcon;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import main.walksy.lib.core.render.WLGraphics2D;

public final class ListIcons {
    private static final Map<String, ListIcon> ITEM_ICONS = new HashMap<>();
    private static final ListEntries ITEMS = ListEntries.items();

    private ListIcons() {
    }

    public static ListIcon forItem(final String itemId) {
        if (itemId == null) {
            return null;
        }
        if (ITEM_ICONS.containsKey(itemId)) {
            return ITEM_ICONS.get(itemId);
        }
        final ListIcon icon = Identifier.tryParse(itemId) == null ? null : ITEMS.icon(itemId);
        ITEM_ICONS.put(itemId, icon);
        return icon;
    }

    public static void extractItem(final WLGraphics2D graphics, final String itemId, final float x, final float y, final float size, final float alpha) {
        extract(graphics, forItem(itemId), x, y, size, alpha);
    }

    public static void extract(final WLGraphics2D graphics, final ListIcon icon, final float x, final float y, final float size, final float alpha) {
        if (icon == null || alpha <= 0.001f) {
            return;
        }
        final int tint = ColorUtils.withAlpha(0xFFFFFFFF, alpha);
        for (final ListIcon.Ref ref : icon.candidates()) {
            final TextureAtlasSprite sprite;
            switch (ref) {
                case ListIcon.Ref.Texture texture -> {
                    graphics.blit(texture.texture(), x, y, 0f, 0f, size, size, 1, 1, 1, 1, tint);
                    return;
                }
                case ListIcon.Ref.Sprite named -> {
                    TextureAtlasSprite found;
                    try {
                        found = graphics.getSprite(named.atlas(), named.texture());
                    } catch (final IllegalArgumentException unknownAtlas) {
                        continue;
                    }
                    if (found == null || !found.contents().name().equals(named.texture())) {
                        continue;
                    }
                    sprite = found;
                }
                case ListIcon.Ref.Resolved lookup -> {
                    final TextureAtlasSprite found = lookup.sprite().get();
                    if (found == null) {
                        continue;
                    }
                    sprite = found;
                }
            }
            graphics.blitUv(sprite.atlasLocation(), x, y, size, size, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), tint);
            return;
        }
    }
}
