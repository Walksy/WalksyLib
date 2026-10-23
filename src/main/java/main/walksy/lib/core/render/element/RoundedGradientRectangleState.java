package main.walksy.lib.core.render.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record RoundedGradientRectangleState(Matrix3x2fc pose, float x0, float y0, float x1, float y1, int colTL, int colTR, int colBR, int colBL, float radius, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    private static final float GRID_CELL = 10f;
    private static final float PILL_EPSILON = 0.01f;

    public RoundedGradientRectangleState(final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final int colTL, final int colTR, final int colBR, final int colBL, final float radius, final @Nullable WLRect scissorArea) {
        this(pose, x0, y0, x1, y1, colTL, colTR, colBR, colBL, radius, scissorArea, getBounds(Mth.floor(x0) - 1, Mth.floor(y0) - 1, Mth.ceil(x1) + 1, Mth.ceil(y1) + 1, pose, scissorArea));
    }

    @Override
    public WLElementKind kind() {
        return WLElementKind.CORNER;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        final float halfW = Math.abs(this.x1 - this.x0) * 0.5f;
        final float halfH = Math.abs(this.y1 - this.y0) * 0.5f;
        final float r = Math.max(0f, Math.min(this.radius, Math.min(halfW, halfH)));
        if (r <= 0.01f) {
            this.band(sink, this.x0, this.y0, this.x1, this.y1);
            return;
        }
        if (this.uniformColor() && (halfH - r < PILL_EPSILON || halfW - r < PILL_EPSILON)) {
            this.pill(sink, halfH - r < PILL_EPSILON, r);
            return;
        }
        this.band(sink, this.x0, this.y0 + r, this.x1, this.y1 - r);
        this.band(sink, this.x0 + r, this.y0, this.x1 - r, this.y0 + r);
        this.band(sink, this.x0 + r, this.y1 - r, this.x1 - r, this.y1);
        this.corner(sink, this.x0 + r, this.y0 + r, r, -1f, -1f);
        this.corner(sink, this.x1 - r, this.y0 + r, r, +1f, -1f);
        this.corner(sink, this.x1 - r, this.y1 - r, r, +1f, +1f);
        this.corner(sink, this.x0 + r, this.y1 - r, r, -1f, +1f);
    }

    private void pill(final WLVertexSink sink, final boolean horizontal, final float r) {
        final int color = this.colTL;
        final float left = Math.min(this.x0, this.x1);
        final float right = Math.max(this.x0, this.x1);
        final float top = Math.min(this.y0, this.y1);
        final float bottom = Math.max(this.y0, this.y1);
        if (horizontal) {
            final float h = (bottom - top) * 0.5f / r;
            this.cap(sink, left, top, left + r, bottom, -1f, -h, 0f, h, color);
            if (right - r > left + r) {
                this.flatQuad(sink, left + r, top, right - r, bottom, color);
            }
            this.cap(sink, right - r, top, right, bottom, 0f, -h, 1f, h, color);
            return;
        }
        final float w = (right - left) * 0.5f / r;
        this.cap(sink, left, top, right, top + r, -w, -1f, w, 0f, color);
        if (bottom - r > top + r) {
            this.flatQuad(sink, left, top + r, right, bottom - r, color);
        }
        this.cap(sink, left, bottom - r, right, bottom, -w, 0f, w, 1f, color);
    }

    private void cap(final WLVertexSink sink, final float ax, final float ay, final float bx, final float by, final float u0, final float v0, final float u1, final float v1, final int color) {
        sink.posed(this.pose, ax, ay).uv(u0, v0).color(color);
        sink.posed(this.pose, ax, by).uv(u0, v1).color(color);
        sink.posed(this.pose, bx, by).uv(u1, v1).color(color);
        sink.posed(this.pose, bx, ay).uv(u1, v0).color(color);
    }

    private void flatQuad(final WLVertexSink sink, final float ax, final float ay, final float bx, final float by, final int color) {
        sink.posed(this.pose, ax, ay).uv(0f, 0f).color(color);
        sink.posed(this.pose, ax, by).uv(0f, 0f).color(color);
        sink.posed(this.pose, bx, by).uv(0f, 0f).color(color);
        sink.posed(this.pose, bx, ay).uv(0f, 0f).color(color);
    }

    private void band(final WLVertexSink sink, final float ax, final float ay, final float bx, final float by) {
        final float w = bx - ax;
        final float h = by - ay;
        if (w <= 0f || h <= 0f) {
            return;
        }
        final boolean uniform = this.uniformColor();
        final int cols = uniform ? 1 : Math.max(1, (int) Math.ceil(w / GRID_CELL));
        final int rows = uniform ? 1 : Math.max(1, (int) Math.ceil(h / GRID_CELL));
        for (int row = 0; row < rows; row++) {
            final float y0 = ay + h * row / rows;
            final float y1 = ay + h * (row + 1) / rows;
            for (int col = 0; col < cols; col++) {
                final float x0 = ax + w * col / cols;
                final float x1 = ax + w * (col + 1) / cols;
                this.flatVertex(sink, x0, y0);
                this.flatVertex(sink, x0, y1);
                this.flatVertex(sink, x1, y1);
                this.flatVertex(sink, x1, y0);
            }
        }
    }

    private void corner(final WLVertexSink sink, final float cx, final float cy, final float r, final float signX, final float signY) {
        final int cells = this.uniformColor() ? 1 : Math.max(1, (int) Math.ceil(r / GRID_CELL));
        final float step = r / cells;
        final float qx0 = signX < 0f ? cx - r : cx;
        final float qy0 = signY < 0f ? cy - r : cy;
        for (int row = 0; row < cells; row++) {
            final float y0 = qy0 + step * row;
            final float y1 = qy0 + step * (row + 1);
            for (int col = 0; col < cells; col++) {
                final float x0 = qx0 + step * col;
                final float x1 = qx0 + step * (col + 1);
                this.cornerVertex(sink, x0, y0, cx, cy, r);
                this.cornerVertex(sink, x0, y1, cx, cy, r);
                this.cornerVertex(sink, x1, y1, cx, cy, r);
                this.cornerVertex(sink, x1, y0, cx, cy, r);
            }
        }
    }

    private void flatVertex(final WLVertexSink sink, final float x, final float y) {
        sink.posed(this.pose, x, y).uv(0f, 0f).color(this.colorAt(x, y));
    }

    private void cornerVertex(final WLVertexSink sink, final float x, final float y, final float cx, final float cy, final float r) {
        sink.posed(this.pose, x, y).uv((x - cx) / r, (y - cy) / r).color(this.colorAt(x, y));
    }

    private boolean uniformColor() {
        return this.colTL == this.colTR && this.colTR == this.colBR && this.colBR == this.colBL;
    }

    private int colorAt(final float x, final float y) {
        if (this.uniformColor()) {
            return this.colTL;
        }
        return GradientColor.at(x, y, this.x0, this.y0, this.x1, this.y1, this.colTL, this.colTR, this.colBR, this.colBL);
    }

    private static @Nullable WLRect getBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        return WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
    }
}
