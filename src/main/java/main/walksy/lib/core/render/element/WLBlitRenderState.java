package main.walksy.lib.core.render.element;

import main.walksy.lib.core.render.gpu.WLTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fc;

@Environment(EnvType.CLIENT)
public record WLBlitRenderState(WLElementKind kind, WLTexture texture, Matrix3x2fc pose, float x0, float y0, float x1, float y1, float u0, float u1, float v0, float v1, int color, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {

    public WLBlitRenderState(final WLElementKind kind, final WLTexture texture, final Matrix3x2fc pose, final float x0, final float y0, final float x1, final float y1, final float u0, final float u1, final float v0, final float v1, final int color, final @Nullable WLRect scissorArea) {
        this(kind, texture, pose, x0, y0, x1, y1, u0, u1, v0, v1, color, scissorArea, getBounds(Mth.floor(x0), Mth.floor(y0), Mth.ceil(x1), Mth.ceil(y1), pose, scissorArea));
    }

    @Override
    public void emit(final WLVertexSink sink) {
        sink.posed(this.pose, this.x0, this.y0).uv(this.u0, this.v0).color(this.color);
        sink.posed(this.pose, this.x0, this.y1).uv(this.u0, this.v1).color(this.color);
        sink.posed(this.pose, this.x1, this.y1).uv(this.u1, this.v1).color(this.color);
        sink.posed(this.pose, this.x1, this.y0).uv(this.u1, this.v0).color(this.color);
    }

    private static @Nullable WLRect getBounds(final int x0, final int y0, final int x1, final int y1, final Matrix3x2fc pose, final @Nullable WLRect scissorArea) {
        return WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
    }
}
