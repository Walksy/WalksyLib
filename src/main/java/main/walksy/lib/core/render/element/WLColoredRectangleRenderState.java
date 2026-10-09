package main.walksy.lib.core.render.element;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record WLColoredRectangleRenderState(Matrix3x2fc pose, float x0, float y0, float x1, float y1, int col1, int col2, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    public WLColoredRectangleRenderState(final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final int col1, final int col2, final @Nullable WLRect scissorArea) {
        this(pose, x0, y0, x1, y1, col1, col2, scissorArea, getBounds(Mth.floor(Math.min(x0, x1)), Mth.floor(Math.min(y0, y1)), Mth.ceil(Math.max(x0, x1)), Mth.ceil(Math.max(y0, y1)), pose, scissorArea));
    }

    @Override
    public WLElementKind kind() {
        return WLElementKind.FLAT;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        sink.posed(this.pose, this.x0, this.y0).color(this.col1);
        sink.posed(this.pose, this.x0, this.y1).color(this.col2);
        sink.posed(this.pose, this.x1, this.y1).color(this.col2);
        sink.posed(this.pose, this.x1, this.y0).color(this.col1);
    }

    private static @Nullable WLRect getBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        return WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
    }
}
