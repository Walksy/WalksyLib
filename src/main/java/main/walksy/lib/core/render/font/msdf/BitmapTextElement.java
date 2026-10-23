package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.element.WLElementKind;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

record BitmapTextElement(BitmapFontAtlas atlas, float layoutScale, String text, Matrix3x2f pose, float x, float y, int color, float shadowX, float shadowY, int shadowColor, boolean snap, @Nullable WLRect scissorArea, @Nullable WLRect bounds) implements WLElement {
    private static final int BOUNDS_PAD = 2;
    private static final int NO_SHADOW = 0;

    static BitmapTextElement of(final BitmapFontAtlas atlas, final float layoutScale, final String text, final Matrix3x2f pose, final float x, final float y, final int color, final boolean snap, @Nullable final WLRect scissorArea) {
        return shadowed(atlas, layoutScale, text, pose, x, y, color, 0f, 0f, NO_SHADOW, snap, scissorArea);
    }

    static BitmapTextElement shadowed(final BitmapFontAtlas atlas, final float layoutScale, final String text, final Matrix3x2f pose, final float x, final float y, final int color, final float shadowX, final float shadowY, final int shadowColor, final boolean snap, @Nullable final WLRect scissorArea) {
        final float left = Math.min(x, x + shadowX);
        final float top = Math.min(y, y + shadowY);
        final int x0 = (int) Math.floor(left) - BOUNDS_PAD;
        final int y0 = (int) Math.floor(top) - BOUNDS_PAD;
        final int x1 = (int) Math.ceil(Math.max(x, x + shadowX) + atlas.layoutWidth(text, layoutScale)) + BOUNDS_PAD;
        final int y1 = (int) Math.ceil(Math.max(y, y + shadowY) + atlas.lineExtent(layoutScale)) + BOUNDS_PAD;
        final WLRect bounds = WLRect.clipped(new WLRect(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose), scissorArea);
        return new BitmapTextElement(atlas, layoutScale, text, pose, x, y, color, shadowX, shadowY, shadowColor, snap, scissorArea, bounds);
    }

    @Override
    public WLElementKind kind() {
        return this.atlas.pixelArt() ? WLElementKind.BITMAP_TEXT_PIXEL : WLElementKind.BITMAP_TEXT;
    }

    @Override
    public WLTexture texture() {
        final boolean smooth = this.atlas.pixelArt() ? Math.abs(this.layoutScale - Math.round(this.layoutScale)) > 1e-4f : !this.snap;
        return this.atlas.filtered(smooth);
    }

    @Override
    public void emit(final WLVertexSink sink) {
        if (this.shadowColor != NO_SHADOW) {
            this.atlas.emitVertices(sink, this.pose, this.text, this.x + this.shadowX, this.y + this.shadowY, this.shadowColor, this.layoutScale, this.snap);
        }
        this.atlas.emitVertices(sink, this.pose, this.text, this.x, this.y, this.color, this.layoutScale, this.snap);
    }
}
