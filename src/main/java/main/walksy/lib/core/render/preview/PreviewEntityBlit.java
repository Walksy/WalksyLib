package main.walksy.lib.core.render.preview;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLTexture;
import org.jetbrains.annotations.Nullable;

public final class PreviewEntityBlit implements WLElement {
    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;
    private final float u0;
    private final float v0;
    private final float u1;
    private final float v1;
    private final int tint;
    private final @Nullable WLRect scissorArea;
    private final @Nullable WLRect bounds;
    private @Nullable WLTexture texture;

    public PreviewEntityBlit(final float x0, final float y0, final float x1, final float y1, final float alpha, @Nullable final WLRect scissorArea) {
        this(x0, y0, x1, y1, 0f, 1f, 1f, 0f, alpha, scissorArea);
    }

    public PreviewEntityBlit(final float x0, final float y0, final float x1, final float y1, final float u0, final float v0, final float u1, final float v1, final float alpha, @Nullable final WLRect scissorArea) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.u0 = u0;
        this.v0 = WLRenderSystem.renderTargetsBottomUp() ? v0 : 1f - v0;
        this.u1 = u1;
        this.v1 = WLRenderSystem.renderTargetsBottomUp() ? v1 : 1f - v1;
        final int a = Math.round(255f * Math.max(0f, Math.min(1f, alpha)));
        this.tint = (a << 24) | (a << 16) | (a << 8) | a;
        this.scissorArea = scissorArea;
        final int left = (int) Math.floor(x0);
        final int top = (int) Math.floor(y0);
        this.bounds = WLRect.clipped(new WLRect(left, top, (int) Math.ceil(x1) - left, (int) Math.ceil(y1) - top), scissorArea);
    }

    public void texture(final WLTexture texture) {
        this.texture = texture;
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
        return this.scissorArea;
    }

    @Override
    public @Nullable WLRect bounds() {
        return this.bounds;
    }

    @Override
    public void emit(final WLVertexSink sink) {
        if (this.texture == null) {
            return;
        }
        sink.vertex(this.x0, this.y0).uv(this.u0, this.v0).color(this.tint);
        sink.vertex(this.x0, this.y1).uv(this.u0, this.v1).color(this.tint);
        sink.vertex(this.x1, this.y1).uv(this.u1, this.v1).color(this.tint);
        sink.vertex(this.x1, this.y0).uv(this.u1, this.v0).color(this.tint);
    }
}
