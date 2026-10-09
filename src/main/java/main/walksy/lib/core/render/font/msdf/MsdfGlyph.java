package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.OverlayBounds;
import main.walksy.lib.core.render.font.msdf.FontData.BoundsData;
import main.walksy.lib.core.render.font.msdf.FontData.GlyphData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.nio.ByteBuffer;

public final class MsdfGlyph {
    private final int code;
    private final float minU;
    private final float maxU;
    private final float minV;
    private final float maxV;
    private final float advance;
    private final float topPosition;
    private final float width;
    private final float height;

    public MsdfGlyph(final GlyphData data, final float atlasWidth, final float atlasHeight) {
        this.code = data.unicode();
        this.advance = data.advance();
        final BoundsData atlas = data.atlasBounds();
        if (atlas != null) {
            this.minU = atlas.left() / atlasWidth;
            this.maxU = atlas.right() / atlasWidth;
            this.minV = atlas.top() / atlasHeight;
            this.maxV = atlas.bottom() / atlasHeight;
        } else {
            this.minU = this.maxU = this.minV = this.maxV = 0.0f;
        }
        final BoundsData plane = data.planeBounds();
        if (plane != null) {
            this.width = plane.right() - plane.left();
            this.height = plane.top() - plane.bottom();
            this.topPosition = plane.top();
        } else {
            this.width = this.height = this.topPosition = 0.0f;
        }
    }

    static MsdfGlyph fromRaw(final int code, final float advance, final float pLeft, final float pTop, final float pRight, final float pBottom, final float ax0, final float ay0, final float ax1, final float ay1, final float atlasWidth, final float atlasHeight) {
        return new MsdfGlyph(code, advance, pLeft, pTop, pRight, pBottom, ax0, ay0, ax1, ay1, atlasWidth, atlasHeight);
    }

    private MsdfGlyph(final int code, final float advance, final float pLeft, final float pTop, final float pRight, final float pBottom, final float ax0, final float ay0, final float ax1, final float ay1, final float atlasWidth, final float atlasHeight) {
        this.code = code;
        this.advance = advance;
        this.width = pRight - pLeft;
        this.height = pTop - pBottom;
        this.topPosition = pTop;
        this.minU = ax0 / atlasWidth;
        this.maxU = ax1 / atlasWidth;
        this.minV = ay0 / atlasHeight;
        this.maxV = ay1 / atlasHeight;
    }

    float emitRaw(final ByteBuffer buf, final Matrix3x2f pose, final float size, final float x, final float y, final int color, @Nullable final OverlayBounds bounds, @Nullable final ContentStretch stretch) {
        final float x0 = x;
        final float y0 = y - this.topPosition * size;
        final float x1 = x0 + this.width * size;
        final float y1 = y0 + this.height * size;
        writeVertex(buf, pose, x0, y0, this.minU, this.minV, color, bounds, stretch);
        writeVertex(buf, pose, x0, y1, this.minU, this.maxV, color, bounds, stretch);
        writeVertex(buf, pose, x1, y1, this.maxU, this.maxV, color, bounds, stretch);
        writeVertex(buf, pose, x1, y0, this.maxU, this.minV, color, bounds, stretch);
        return this.advance * size;
    }

    static void writeVertex(final ByteBuffer buf, final Matrix3x2f pose, final float x, final float y, final float u, final float v, final int argb, @Nullable final OverlayBounds bounds, @Nullable final ContentStretch stretch) {
        final float px = pose.m00() * x + pose.m10() * y + pose.m20();
        final float py = pose.m01() * x + pose.m11() * y + pose.m21();
        final float tx = stretch == null ? px : stretch.stretchedX(px, py);
        final float ty = stretch == null ? py : stretch.stretchedY(px, py);
        if (bounds != null) {
            bounds.add(tx, ty);
        }
        buf.putFloat(tx).putFloat(ty).putFloat(0f);
        buf.putFloat(u).putFloat(v);
        buf.put((byte) ((argb >> 16) & 0xFF));
        buf.put((byte) ((argb >> 8) & 0xFF));
        buf.put((byte) (argb & 0xFF));
        buf.put((byte) ((argb >> 24) & 0xFF));
    }

    float getAdvance() {
        return this.advance;
    }

    int getCode() {
        return this.code;
    }

    boolean isRenderable() {
        return this.width > 0f && this.height > 0f;
    }
}
