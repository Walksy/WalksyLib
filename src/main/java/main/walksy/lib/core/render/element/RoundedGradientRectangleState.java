package main.walksy.lib.core.render.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record RoundedGradientRectangleState(Matrix3x2fc pose, float x0, float y0, float x1, float y1, int colTL, int colTR, int colBR, int colBL, float radius, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    private static final float GRID_CELL = 10f;

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
        this.band(sink, this.x0, this.y0 + r, this.x1, this.y1 - r);
        this.band(sink, this.x0 + r, this.y0, this.x1 - r, this.y0 + r);
        this.band(sink, this.x0 + r, this.y1 - r, this.x1 - r, this.y1);
        this.corner(sink, this.x0 + r, this.y0 + r, r, -1f, -1f);
        this.corner(sink, this.x1 - r, this.y0 + r, r, +1f, -1f);
        this.corner(sink, this.x1 - r, this.y1 - r, r, +1f, +1f);
        this.corner(sink, this.x0 + r, this.y1 - r, r, -1f, +1f);
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
        return GradientColor.at(x, y, this.x0, this.y0, this.x1, this.y1, this.colTL, this.colTR, this.colBR, this.colBL);
    }

    private static @Nullable WLRect getBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        return WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
    }
}
