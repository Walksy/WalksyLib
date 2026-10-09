package main.walksy.lib.core.render.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record CheckerCircleRenderState(Matrix3x2fc pose, float cx, float cy, float radius, float alpha, boolean flip, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    public CheckerCircleRenderState(final Matrix3x2fc pose, final float cx, final float cy, final float radius, final float alpha, final boolean flip, final @Nullable WLRect scissorArea) {
        this(pose, cx, cy, radius, alpha, flip, scissorArea, getBounds(cx, cy, radius, pose, scissorArea));
    }

    @Override
    public WLElementKind kind() {
        return WLElementKind.CHECKER;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        final float r = Math.max(0f, this.radius);
        if (r <= 0.01f) {
            return;
        }
        final float u = this.flip ? -1f : 1f;
        final int color = (Math.round(Math.max(0f, Math.min(1f, this.alpha)) * 255f) << 24);
        this.vertex(sink, this.cx - r, this.cy - r, -u, -1f, color);
        this.vertex(sink, this.cx - r, this.cy + r, -u, 1f, color);
        this.vertex(sink, this.cx + r, this.cy + r, u, 1f, color);
        this.vertex(sink, this.cx + r, this.cy - r, u, -1f, color);
    }

    private void vertex(final WLVertexSink sink, final float x, final float y, final float u, final float v, final int color) {
        sink.posed(this.pose, x, y).uv(u, v).color(color);
    }

    private static @Nullable WLRect getBounds(final float cx, final float cy, final float radius, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        final int r = (int) Math.ceil(radius) + 1;
        return WLRect.clipped(new WLRect((int) cx - r, (int) cy - r, r * 2, r * 2).transformMaxBounds(pose), scissorArea);
    }
}
