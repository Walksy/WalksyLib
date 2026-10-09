package main.walksy.lib.core.platform;

import com.mojang.blaze3d.platform.NativeImage;

public final class WLImages {
    private WLImages() {
    }

    public static int getArgb(final NativeImage image, final int x, final int y) {
        return swapRedBlue(image.getPixelRGBA(x, y));
    }

    public static void setArgb(final NativeImage image, final int x, final int y, final int argb) {
        image.setPixelRGBA(x, y, swapRedBlue(argb));
    }

    private static int swapRedBlue(final int color) {
        return (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
    }
}
