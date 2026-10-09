package main.walksy.lib.core.render;

import main.walksy.lib.core.gui.style.UiStyle;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

public final class ContentStretch {
    public static final float MAX_FRACTION = 0.6f;
    public static final float THIN = 0.3f;
    private static final float MIN_PULL = 1e-3f;
    private static final float MIN_REACH = 0.05f;
    private static @Nullable ContentStretch current;

    private final float m00;
    private final float m01;
    private final float m10;
    private final float m11;
    private final float tx;
    private final float ty;
    private final float reach;

    private ContentStretch(final float cx, final float cy, final float bx, final float by, final float pullX, final float pullY, final float contentScale) {
        final float raw = (float) Math.sqrt(pullX * pullX + pullY * pullY);
        final float nx = raw > 0f ? pullX / raw : 1f;
        final float ny = raw > 0f ? pullY / raw : 0f;
        final float mx = -ny;
        final float my = nx;
        final float len = Math.min(raw, MAX_FRACTION * Math.min(bx, by));
        final float along = Math.max(Math.abs(nx) * bx + Math.abs(ny) * by, 1e-6f);
        final float across = Math.max(Math.abs(mx) * bx + Math.abs(my) * by, 1e-6f);
        final float sa = 1f + len / (2f * along);
        final float sp = 1f - THIN * (sa - 1f) * Math.min(1f, along / across);
        final float k = contentScale;
        this.m00 = k * (sa * nx * nx + sp * mx * mx);
        this.m01 = k * (sa * nx * ny + sp * mx * my);
        this.m10 = this.m01;
        this.m11 = k * (sa * ny * ny + sp * my * my);
        this.tx = cx + nx * 0.5f * len - (this.m00 * cx + this.m01 * cy);
        this.ty = cy + ny * 0.5f * len - (this.m10 * cx + this.m11 * cy);
        this.reach = len + Math.abs(k - 1f) * (bx + by);
    }

    public static @Nullable ContentStretch of(final Matrix3x2fc pose, final float x, final float y, final float w, final float h, final float pullX, final float pullY, final float contentScale) {
        if (UiStyle.minecraft()) {
            return null;
        }
        final float sx = pose.m00() * pullX + pose.m10() * pullY;
        final float sy = pose.m01() * pullX + pose.m11() * pullY;
        final boolean pulled = sx * sx + sy * sy >= MIN_PULL * MIN_PULL;
        if (w <= 0f || h <= 0f) {
            return null;
        }
        final float x0 = pose.m00() * x + pose.m10() * y + pose.m20();
        final float y0 = pose.m01() * x + pose.m11() * y + pose.m21();
        final float x1 = pose.m00() * (x + w) + pose.m10() * (y + h) + pose.m20();
        final float y1 = pose.m01() * (x + w) + pose.m11() * (y + h) + pose.m21();
        final ContentStretch stretch = new ContentStretch(0.5f * (x0 + x1), 0.5f * (y0 + y1), 0.5f * Math.abs(x1 - x0), 0.5f * Math.abs(y1 - y0), pulled ? sx : 0f, pulled ? sy : 0f, contentScale);
        return stretch.reach < MIN_REACH ? null : stretch;
    }

    public static @Nullable ContentStretch current() {
        return current;
    }

    public static void run(@Nullable final ContentStretch stretch, final Runnable body) {
        if (stretch == null) {
            body.run();
            return;
        }
        final ContentStretch previous = current;
        current = stretch;
        try {
            body.run();
        } finally {
            current = previous;
        }
    }

    public float reach() {
        return this.reach;
    }

    public float stretchedX(final float x, final float y) {
        return this.m00 * x + this.m01 * y + this.tx;
    }

    public float stretchedY(final float x, final float y) {
        return this.m10 * x + this.m11 * y + this.ty;
    }
}
