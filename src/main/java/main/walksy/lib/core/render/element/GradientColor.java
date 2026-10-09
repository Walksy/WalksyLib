package main.walksy.lib.core.render.element;

import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.util.Mth;

final class GradientColor {
    private GradientColor() {
    }

    static int at(final float x, final float y, final float x0, final float y0, final float x1, final float y1, final int colTL, final int colTR, final int colBR, final int colBL) {
        final float u = Mth.clamp((x - x0) / (x1 - x0), 0f, 1f);
        final float v = Mth.clamp((y - y0) / (y1 - y0), 0f, 1f);
        final int top = ColorUtils.lerpColor(colTL, colTR, u);
        final int bottom = ColorUtils.lerpColor(colBL, colBR, u);
        return ColorUtils.lerpColor(top, bottom, v);
    }
}
