package main.walksy.lib.core.gui.animation;

import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;

public final class Materialize {
    private static final float START_SCALE = 0.96f;
    private static final float RISE_PX = 2.5f;
    private static final float SPAN = 0.6f;

    private Materialize() {
    }

    public static float stagger(final float reveal, final int index, final int count) {
        if (count <= 1) {
            return Mth.clamp(reveal, 0f, 1f);
        }
        final float start = (1f - SPAN) * (index / (float) (count - 1));
        return Mth.clamp((reveal - start) / SPAN, 0f, 1f);
    }

    public static float enter(final Matrix3x2fStack pose, final float p, final float cx, final float cy, final float uiScale) {
        final float e = easeOutCubic(Mth.clamp(p, 0f, 1f));
        final float scale = START_SCALE + (1f - START_SCALE) * e;
        final float rise = (1f - e) * RISE_PX * uiScale;
        pose.pushMatrix();
        pose.translate(cx, cy + rise);
        pose.scale(scale, scale);
        pose.translate(-cx, -cy);
        return e;
    }

    public static void exit(final Matrix3x2fStack pose) {
        pose.popMatrix();
    }

    private static float easeOutCubic(final float t) {
        final float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
