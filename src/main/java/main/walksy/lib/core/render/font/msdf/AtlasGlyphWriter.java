package main.walksy.lib.core.render.font.msdf;

import com.mojang.blaze3d.platform.NativeImage;

import java.nio.ByteBuffer;
import net.minecraft.util.Mth;

final class AtlasGlyphWriter {
    private AtlasGlyphWriter() {
    }

    static void writeGrayscaleGlyphWithGutter(final NativeImage image, final ByteBuffer pixels, final int x, final int y, final int width, final int height, final int gutter) {
        for (int row = -gutter; row < height + gutter; row++) {
            final int srcRow = Mth.clamp(row, 0, height - 1);
            for (int col = -gutter; col < width + gutter; col++) {
                final int srcCol = Mth.clamp(col, 0, width - 1);
                final int value = pixels.get(srcRow * width + srcCol) & 0xFF;
                image.setPixel(x + col, y + row, grayscaleRgba(value));
            }
        }
    }

    private static int grayscaleRgba(final int value) {
        return (0xFF << 24) | (value << 16) | (value << 8) | value;
    }
}
