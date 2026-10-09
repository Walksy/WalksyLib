package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record StretchedBlitRenderState(WLElementKind kind, WLTexture texture, Matrix3x2fc pose, float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color, ContentStretch stretch, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    public StretchedBlitRenderState(final WLElementKind kind, final WLTexture texture, final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final ContentStretch stretch, final @Nullable WLRect scissorArea) {
        this(kind, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, stretch, scissorArea, stretchedBounds(x0, y0, x1, y1, pose, stretch, scissorArea));
    }

    @Override
    public void emit(final WLVertexSink sink) {
        this.vertex(sink, this.x0, this.y0, this.u0, this.v0);
        this.vertex(sink, this.x0, this.y1, this.u0, this.v1);
        this.vertex(sink, this.x1, this.y1, this.u1, this.v1);
        this.vertex(sink, this.x1, this.y0, this.u1, this.v0);
    }

    private void vertex(final WLVertexSink sink, final float x, final float y, final float u, final float v) {
        final float px = this.pose.m00() * x + this.pose.m10() * y + this.pose.m20();
        final float py = this.pose.m01() * x + this.pose.m11() * y + this.pose.m21();
        sink.vertex(this.stretch.stretchedX(px, py), this.stretch.stretchedY(px, py)).uv(u, v).color(this.color);
    }

    static @Nullable WLRect stretchedBounds(final float x0, final float y0, final float x1, final float y1, final Matrix3x2fc pose, final ContentStretch stretch, final @Nullable WLRect scissorArea) {
        final int grow = (int) Math.ceil(stretch.reach());
        final int left = (int) Math.floor(Math.min(x0, x1));
        final int top = (int) Math.floor(Math.min(y0, y1));
        final int right = (int) Math.ceil(Math.max(x0, x1));
        final int bottom = (int) Math.ceil(Math.max(y0, y1));
        final WLRect posed = new WLRect(left, top, right - left, bottom - top).transformMaxBounds(pose);
        final WLRect bounds = new WLRect(posed.left() - grow, posed.top() - grow, posed.width() + 2 * grow, posed.height() + 2 * grow);
        return WLRect.clipped(bounds, scissorArea);
    }
}
