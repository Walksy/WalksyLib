package main.walksy.lib.core.render.glass.gui;

import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.glass.LiquidGlassComposite;
import main.walksy.lib.core.render.gpu.WLTexture;
import org.jetbrains.annotations.Nullable;

public final class LiquidGlassLayerElement implements WLElement {
    private final WLTexture texture;
    private final WLRect bounds;
    private final long frame;

    public LiquidGlassLayerElement(final WLTexture texture, final WLRect bounds) {
        this.texture = texture;
        this.bounds = bounds;
        this.frame = WLRenderSystem.frameId();
    }

    @Override
    public WLElementKind kind() {
        return WLElementKind.PREMULTIPLIED;
    }

    @Override
    public @Nullable WLTexture texture() {
        return this.texture;
    }

    @Override
    public @Nullable WLRect scissorArea() {
        return null;
    }

    @Override
    public @Nullable WLRect bounds() {
        return this.bounds;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        final int[] area = LiquidGlassComposite.layerArea(this.frame);
        if (area == null) {
            sink.vertex(0f, 0f).uv(0f, 0f).color(0);
            sink.vertex(0f, 0f).uv(0f, 0f).color(0);
            sink.vertex(0f, 0f).uv(0f, 0f).color(0);
            sink.vertex(0f, 0f).uv(0f, 0f).color(0);
            return;
        }
        final float fbW = WLRenderSystem.targetWidth();
        final float fbH = WLRenderSystem.targetHeight();
        final float scaleX = DevicePixels.perLogicalX();
        final float scaleY = DevicePixels.perLogicalY();
        final float gx0 = area[0];
        final float gy0 = area[1];
        final float gx1 = area[0] + area[2];
        final float gy1 = area[1] + area[3];
        final float x0 = gx0 / scaleX;
        final float x1 = gx1 / scaleX;
        final float top = (fbH - gy1) / scaleY;
        final float bottom = (fbH - gy0) / scaleY;
        final float u0 = gx0 / fbW;
        final float u1 = gx1 / fbW;
        final boolean bottomUp = WLRenderSystem.renderTargetsBottomUp();
        final float vTop = bottomUp ? gy1 / fbH : 1f - gy1 / fbH;
        final float vBottom = bottomUp ? gy0 / fbH : 1f - gy0 / fbH;
        sink.vertex(x0, top).uv(u0, vTop).color(-1);
        sink.vertex(x0, bottom).uv(u0, vBottom).color(-1);
        sink.vertex(x1, bottom).uv(u1, vBottom).color(-1);
        sink.vertex(x1, top).uv(u1, vTop).color(-1);
    }
}
