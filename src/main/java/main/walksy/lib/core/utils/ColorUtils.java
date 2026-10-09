package main.walksy.lib.core.utils;

import net.minecraft.util.Mth;

public final class ColorUtils {
    private ColorUtils() {
    }

    public static int withAlpha(final int argb, final float factor) {
        final int alpha = Math.round(((argb >>> 24) & 0xFF) * Mth.clamp(factor, 0f, 1f));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    public static int lerpColor(final int from, final int to, final float t) {
        final float f = Mth.clamp(t, 0f, 1f);
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            final int a = (from >>> shift) & 0xFF;
            final int b = (to >>> shift) & 0xFF;
            result |= Math.round(a + (b - a) * f) << shift;
        }
        return result;
    }
}
