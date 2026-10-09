package main.walksy.lib.core.render.font.msdf;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.gui.style.Type;
import main.walksy.lib.core.render.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

final class PixelFont {
    private static final Identifier SOURCE = Identifier.withDefaultNamespace("textures/font/ascii.png");
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath("walksylib", "pixel_font/ascii");
    private static final int GRID = 16;
    private static final int BASE_CELL = 8;
    private static final int ASCENT = 7;
    private static final int LINE = 9;
    private static final int FIRST = 32;
    private static final int LAST = 126;
    private static final int SPACE_WIDTH = 3;
    private static final float BODY_SIZE = Type.CAPTION1.size();
    private static final float HEADING_SIZE = Type.HEADLINE.size();
    private static BitmapFontAtlas atlas;
    private static DynamicTexture texture;
    private static int textureWidth;
    private static int textureHeight;
    private static int[] widths;
    private static int cell = BASE_CELL;
    private static boolean failed;

    private PixelFont() {
    }

    private static float pixelScale(final float size) {
        final float perPixel = DevicePixels.perLogicalY() / Fonts.PIXEL_SIZE_PER_PIXEL;
        final int scale = Math.max(1, Math.round(size * perPixel));
        if (size < HEADING_SIZE) {
            return scale;
        }
        return Math.max(scale, Math.max(1, Math.round(BODY_SIZE * perPixel)) + 1);
    }

    static @Nullable BitmapFontAtlas atlas() {
        if (!load()) {
            return null;
        }
        if (atlas == null) {
            atlas = BitmapFontAtlas.pixel(texture, textureWidth, textureHeight, widths, cell, cell + 2, ASCENT * cell / BASE_CELL);
        }
        return atlas;
    }

    static float layoutScale(final float size) {
        return pixelScale(size) * BASE_CELL / cell;
    }

    static float width(final BitmapFontAtlas atlas, final String text, final float size) {
        return atlas.width(normalize(text)) * layoutScale(size);
    }

    static float unit(final float size) {
        return pixelScale(size) / DevicePixels.perLogicalY();
    }

    static float height(final float size) {
        return LINE * unit(size);
    }

    static float ascent(final float size) {
        return ASCENT * unit(size);
    }

    static float shadowOffset(final float size) {
        return Math.max(1, Math.round(size * DevicePixels.perLogicalY() / Fonts.PIXEL_SIZE_PER_PIXEL)) / DevicePixels.perLogicalY();
    }

    static int shadowColor(final int color) {
        return (color & 0xFF000000) | ((color >> 2) & 0x003F3F3F);
    }

    static String normalize(final String text) {
        return text.indexOf('…') < 0 ? text : text.replace("…", "...");
    }

    private static boolean load() {
        if (texture != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try (final InputStream stream = Minecraft.getInstance().getResourceManager().open(SOURCE)) {
            final NativeImage source = NativeImage.read(stream);
            final int w = source.getWidth();
            final int h = source.getHeight();
            cell = Math.max(1, w / GRID);
            final int stride = cell + 2;
            final int rows = h / cell;
            final NativeImage out = new NativeImage(NativeImage.Format.RGBA, GRID * stride, rows * stride, true);
            for (int y = 0; y < rows * cell; y++) {
                for (int x = 0; x < GRID * cell; x++) {
                    final int alpha = (source.getPixel(x, y) >>> 24) & 0xFF;
                    out.setPixel((x / cell) * stride + 1 + x % cell, (y / cell) * stride + 1 + y % cell, 0xFF000000 | (alpha << 16) | (alpha << 8) | alpha);
                }
            }
            final int[] measured = new int[LAST + 1];
            for (int code = 0; code <= LAST; code++) {
                measured[code] = code < FIRST ? -1 : code == ' ' ? SPACE_WIDTH * cell / BASE_CELL : columnWidth(source, code, w, h);
            }
            source.close();
            textureWidth = out.getWidth();
            textureHeight = out.getHeight();
            widths = measured;
            texture = new DynamicTexture(out::toString, out);
            Minecraft.getInstance().getTextureManager().register(ATLAS, texture);
            return true;
        } catch (final Exception e) {
            failed = true;
            WalksyLib.getLogger().err("Could not load the Minecraft font for the Minecraft UI style: " + e.getMessage());
            return false;
        }
    }

    private static int columnWidth(final NativeImage source, final int code, final int w, final int h) {
        final int cellX = (code % GRID) * cell;
        final int cellY = (code / GRID) * cell;
        if (cellX + cell > w || cellY + cell > h) {
            return -1;
        }
        for (int col = cell - 1; col >= 0; col--) {
            for (int row = 0; row < cell; row++) {
                if (((source.getPixel(cellX + col, cellY + row) >>> 24) & 0xFF) != 0) {
                    return col + 1;
                }
            }
        }
        return 0;
    }
}
