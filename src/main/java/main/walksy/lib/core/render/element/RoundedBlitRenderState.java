package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record RoundedBlitRenderState(WLElementKind kind, WLTexture texture, Matrix3x2fc pose, float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color, float radius, int segments, @Nullable ContentStretch stretch, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    private static final int DEFAULT_SEGMENTS = 12;

    public RoundedBlitRenderState(final WLElementKind kind, final WLTexture texture, final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final float radius, final @Nullable WLRect scissorArea) {
        this(kind, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, radius, null, scissorArea);
    }

    public RoundedBlitRenderState(final WLElementKind kind, final WLTexture texture, final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final float radius, final @Nullable ContentStretch stretch, final @Nullable WLRect scissorArea) {
        this(kind, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, radius, DEFAULT_SEGMENTS, stretch, scissorArea, stretch != null ? StretchedBlitRenderState.stretchedBounds(x0, y0, x1, y1, pose, stretch, scissorArea) : getBounds(Mth.floor(x0), Mth.floor(y0), Mth.ceil(x1), Mth.ceil(y1), pose, scissorArea));
    }

    @Override
    public void emit(final WLVertexSink sink) {
        final float halfW = Math.abs(this.x1 - this.x0) * 0.5f;
        final float halfH = Math.abs(this.y1 - this.y0) * 0.5f;
        final float r = Math.max(0f, Math.min(this.radius, Math.min(halfW, halfH)));
        if (r <= 0.01f) {
            this.rect(sink, this.x0, this.y0, this.x1, this.y1);
            return;
        }
        if (this.kind == WLElementKind.ROUNDED_TEXTURED) {
            this.emitSmooth(sink, r);
            return;
        }
        this.rect(sink, this.x0, this.y0 + r, this.x1, this.y1 - r);
        this.rect(sink, this.x0 + r, this.y0, this.x1 - r, this.y0 + r);
        this.rect(sink, this.x0 + r, this.y1 - r, this.x1 - r, this.y1);
        this.corner(sink, this.x0 + r, this.y0 + r, r, (float) Math.PI, (float) (1.5 * Math.PI));
        this.corner(sink, this.x1 - r, this.y0 + r, r, (float) (1.5 * Math.PI), (float) (2 * Math.PI));
        this.corner(sink, this.x1 - r, this.y1 - r, r, 0f, (float) (0.5 * Math.PI));
        this.corner(sink, this.x0 + r, this.y1 - r, r, (float) (0.5 * Math.PI), (float) Math.PI);
    }

    private void emitSmooth(final WLVertexSink sink, final float r) {
        this.rect(sink, this.x0, this.y0 + r, this.x1, this.y1 - r);
        this.rect(sink, this.x0 + r, this.y0, this.x1 - r, this.y0 + r);
        this.rect(sink, this.x0 + r, this.y1 - r, this.x1 - r, this.y1);
        this.smoothCorner(sink, this.x0, this.y0, this.x0 + r, this.y0 + r, r);
        this.smoothCorner(sink, this.x1 - r, this.y0, this.x1, this.y0 + r, r);
        this.smoothCorner(sink, this.x1 - r, this.y1 - r, this.x1, this.y1, r);
        this.smoothCorner(sink, this.x0, this.y1 - r, this.x0 + r, this.y1, r);
    }

    private void smoothCorner(final WLVertexSink sink, final float ax, final float ay, final float bx, final float by, final float r) {
        final float cx = ax == this.x0 ? bx : ax;
        final float cy = ay == this.y0 ? by : ay;
        this.vertex(sink, ax, ay, this.localColor(ax, ay, cx, cy, r));
        this.vertex(sink, ax, by, this.localColor(ax, by, cx, cy, r));
        this.vertex(sink, bx, by, this.localColor(bx, by, cx, cy, r));
        this.vertex(sink, bx, ay, this.localColor(bx, ay, cx, cy, r));
    }

    private int localColor(final float x, final float y, final float cx, final float cy, final float r) {
        final int lx = Math.round(Math.min(1f, Math.abs(x - cx) / r) * 255f);
        final int ly = Math.round(Math.min(1f, Math.abs(y - cy) / r) * 255f);
        return (this.color & 0xFF000000) | (lx << 16) | (ly << 8);
    }

    private void rect(final WLVertexSink sink, final float ax, final float ay, final float bx, final float by) {
        final int color = this.kind == WLElementKind.ROUNDED_TEXTURED ? this.color & 0xFF000000 : this.color;
        this.vertex(sink, ax, ay, color);
        this.vertex(sink, ax, by, color);
        this.vertex(sink, bx, by, color);
        this.vertex(sink, bx, ay, color);
    }

    private void corner(final WLVertexSink sink, final float cx, final float cy, final float r, final float angleStart, final float angleEnd) {
        float prevX = cx + r * (float) Math.cos(angleStart);
        float prevY = cy + r * (float) Math.sin(angleStart);
        for (int i = 1; i <= this.segments; i++) {
            final float t = angleStart + (angleEnd - angleStart) * i / this.segments;
            final float x = cx + r * (float) Math.cos(t);
            final float y = cy + r * (float) Math.sin(t);
            this.vertex(sink, cx, cy, this.color);
            this.vertex(sink, x, y, this.color);
            this.vertex(sink, prevX, prevY, this.color);
            this.vertex(sink, cx, cy, this.color);
            prevX = x;
            prevY = y;
        }
    }

    private void vertex(final WLVertexSink sink, final float x, final float y, final int color) {
        final float u = this.u0 + (x - this.x0) / (this.x1 - this.x0) * (this.u1 - this.u0);
        final float v = this.v0 + (y - this.y0) / (this.y1 - this.y0) * (this.v1 - this.v0);
        if (this.stretch == null) {
            sink.posed(this.pose, x, y).uv(u, v).color(color);
            return;
        }
        final float px = this.pose.m00() * x + this.pose.m10() * y + this.pose.m20();
        final float py = this.pose.m01() * x + this.pose.m11() * y + this.pose.m21();
        sink.vertex(this.stretch.stretchedX(px, py), this.stretch.stretchedY(px, py)).uv(u, v).color(color);
    }

    private static @Nullable WLRect getBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        return WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
    }
}
