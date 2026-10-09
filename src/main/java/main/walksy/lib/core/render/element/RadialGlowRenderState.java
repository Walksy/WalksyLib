package main.walksy.lib.core.render.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record RadialGlowRenderState(Matrix3x2fc pose, float cx, float cy, float radius, int centerColor, int segments, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    private static final int DEFAULT_SEGMENTS = 48;

    private static final float[] RING_T = {0f, 0.25f, 0.5f, 0.75f, 1f};

    public RadialGlowRenderState(final Matrix3x2fc pose, final float cx, final float cy, final float radius, final int centerColor, final @Nullable WLRect scissorArea) {
        this(pose, cx, cy, radius, centerColor, DEFAULT_SEGMENTS, scissorArea, getBounds(cx, cy, radius, pose, scissorArea));
    }

    @Override
    public WLElementKind kind() {
        return WLElementKind.FLAT;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        final int baseAlpha = (this.centerColor >>> 24) & 0xFF;
        final int rgb = this.centerColor & 0x00FFFFFF;
        float prevCos = 1f;
        float prevSin = 0f;
        for (int i = 1; i <= this.segments; i++) {
            final double a = Math.PI * 2.0 * i / this.segments;
            final float cos = (float) Math.cos(a);
            final float sin = (float) Math.sin(a);
            for (int ring = 0; ring < RING_T.length - 1; ring++) {
                final float t0 = RING_T[ring];
                final float t1 = RING_T[ring + 1];
                final int c0 = colorAt(rgb, baseAlpha, t0);
                final int c1 = colorAt(rgb, baseAlpha, t1);
                final float r0 = this.radius * t0;
                final float r1 = this.radius * t1;
                this.vertex(sink, this.cx + r0 * prevCos, this.cy + r0 * prevSin, c0);
                this.vertex(sink, this.cx + r1 * prevCos, this.cy + r1 * prevSin, c1);
                this.vertex(sink, this.cx + r1 * cos, this.cy + r1 * sin, c1);
                this.vertex(sink, this.cx + r0 * cos, this.cy + r0 * sin, c0);
            }
            prevCos = cos;
            prevSin = sin;
        }
    }

    private static int colorAt(final int rgb, final int baseAlpha, final float t) {
        final float falloff = (1f - t) * (1f - t);
        return (Math.round(baseAlpha * falloff) << 24) | rgb;
    }

    private void vertex(final WLVertexSink sink, final float x, final float y, final int color) {
        sink.posed(this.pose, x, y).color(color);
    }

    private static @Nullable WLRect getBounds(final float cx, final float cy, final float radius, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        final int r = (int) Math.ceil(radius) + 1;
        return WLRect.clipped(new WLRect((int) cx - r, (int) cy - r, r * 2, r * 2).transformMaxBounds(pose), scissorArea);
    }
}
