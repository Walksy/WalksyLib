package main.walksy.lib.core.render.font.msdf;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.OverlayBounds;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.element.WLVertexSink;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BitmapFontAtlas {
    private static final int GLYPH_GUTTER = 2;
    private static final int MIN_ATLAS_SIZE = 256;
    private static final int MAX_ATLAS_SIZE = 4096;
    private static final int ASCII = 128;
    private final AbstractTexture texture;
    private final Map<Integer, Glyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;
    private final Glyph[] asciiGlyphs = new Glyph[ASCII];
    private final float[] asciiKerning = new float[ASCII * ASCII];
    private final float baselinePx;
    private final int physicalSize;
    private boolean pixelArt;
    private @Nullable WLTexture linearTexture;
    private @Nullable WLTexture nearestTexture;

    private BitmapFontAtlas(final AbstractTexture texture, final Map<Integer, Glyph> glyphs, final Map<Integer, Map<Integer, Float>> kernings, final float baselinePx, final int physicalSize) {
        this.texture = texture;
        this.glyphs = glyphs;
        this.kernings = kernings;
        for (final Map.Entry<Integer, Glyph> entry : glyphs.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < ASCII) {
                this.asciiGlyphs[entry.getKey()] = entry.getValue();
            }
        }
        for (final Map.Entry<Integer, Map<Integer, Float>> row : kernings.entrySet()) {
            final int first = row.getKey();
            if (first < 0 || first >= ASCII) {
                continue;
            }
            for (final Map.Entry<Integer, Float> pair : row.getValue().entrySet()) {
                if (pair.getKey() >= 0 && pair.getKey() < ASCII) {
                    this.asciiKerning[first * ASCII + pair.getKey()] = pair.getValue();
                }
            }
        }
        this.baselinePx = baselinePx;
        this.physicalSize = physicalSize;
    }

    static BitmapFontAtlas pixel(final AbstractTexture texture, final int textureWidth, final int textureHeight, final int[] cellWidths, final int cell, final int stride, final int ascent) {
        final Map<Integer, Glyph> glyphs = new HashMap<>();
        for (int code = 0; code < cellWidths.length; code++) {
            final int width = cellWidths[code];
            if (width < 0) {
                continue;
            }
            final float advance = width + 1;
            if (width == 0) {
                glyphs.put(code, new Glyph(advance, 0, 0, 0, 0, 0f, 0f, 0f, 0f));
                continue;
            }
            final int frameX = (code % 16) * stride;
            final int frameY = (code / 16) * stride;
            glyphs.put(code, new Glyph(advance, -1, -ascent - 1, width + 2, cell + 2, frameX / (float) textureWidth, frameY / (float) textureHeight, (frameX + width + 2) / (float) textureWidth, (frameY + cell + 2) / (float) textureHeight));
        }
        final BitmapFontAtlas atlas = new BitmapFontAtlas(texture, glyphs, Map.of(), ascent, cell + 1);
        atlas.pixelArt = true;
        return atlas;
    }

    private static boolean fits(final List<int[]> glyphSizes, final int atlasSize) {
        int curX = 0;
        int curY = 0;
        int rowH = 0;
        for (final int[] size : glyphSizes) {
            if (size[0] <= 0 || size[1] <= 0) {
                continue;
            }
            final int packedW = size[0] + GLYPH_GUTTER * 2;
            final int packedH = size[1] + GLYPH_GUTTER * 2;
            if (curX + packedW > atlasSize) {
                curX = 0;
                curY += rowH + 1;
                rowH = 0;
            }
            if (curY + packedH > atlasSize) {
                return false;
            }
            rowH = Math.max(rowH, packedH);
            curX += packedW + 1;
        }
        return true;
    }

    static BitmapFontAtlas create(final STBTTFontinfo info, final String charset, final int physicalSize) {
        final float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, physicalSize);
        final IntBuffer ascBuf = BufferUtils.createIntBuffer(1);
        final IntBuffer dscBuf = BufferUtils.createIntBuffer(1);
        final IntBuffer gapBuf = BufferUtils.createIntBuffer(1);
        STBTruetype.stbtt_GetFontVMetrics(info, ascBuf, dscBuf, gapBuf);
        final float baselinePx = (ascBuf.get(0) + gapBuf.get(0)) * scale;
        final IntBuffer wb = BufferUtils.createIntBuffer(1);
        final IntBuffer hb = BufferUtils.createIntBuffer(1);
        final IntBuffer xb = BufferUtils.createIntBuffer(1);
        final IntBuffer yb = BufferUtils.createIntBuffer(1);
        final IntBuffer advBuf = BufferUtils.createIntBuffer(1);
        final IntBuffer lsbBuf = BufferUtils.createIntBuffer(1);
        record GlyphBitmap(int code, ByteBuffer pixels, int w, int h, int xoff, int yoff, float advance) {}
        final Map<Integer, GlyphBitmap> bitmaps = new HashMap<>();
        for (int i = 0; i < charset.length(); i++) {
            final int cp = charset.charAt(i);
            STBTruetype.stbtt_GetCodepointHMetrics(info, cp, advBuf, lsbBuf);
            final float advance = advBuf.get(0) * scale;
            final ByteBuffer bm = STBTruetype.stbtt_GetCodepointBitmap(info, 0.0f, scale, cp, wb, hb, xb, yb);
            final int w = wb.get(0);
            final int h = hb.get(0);
            if (bm != null && w > 0 && h > 0) {
                bitmaps.put(cp, new GlyphBitmap(cp, bm, w, h, xb.get(0), yb.get(0), advance));
            } else {
                bitmaps.put(cp, new GlyphBitmap(cp, null, 0, 0, 0, 0, advance));
            }
        }
        final List<int[]> glyphSizes = new ArrayList<>(bitmaps.size());
        for (final GlyphBitmap bm : bitmaps.values()) {
            glyphSizes.add(new int[] {bm.w(), bm.h()});
        }
        int atlasSize = MIN_ATLAS_SIZE;
        while (atlasSize < MAX_ATLAS_SIZE && !fits(glyphSizes, atlasSize)) {
            atlasSize *= 2;
        }
        final NativeImage img = new NativeImage(NativeImage.Format.RGBA, atlasSize, atlasSize, true);
        img.fillRect(0, 0, atlasSize, atlasSize, 0);
        int curX = 0;
        int curY = 0;
        int rowH = 0;
        final Map<Integer, Glyph> glyphs = new HashMap<>();
        for (final GlyphBitmap bm : bitmaps.values()) {
            if (bm.w() <= 0 || bm.h() <= 0) {
                glyphs.put(bm.code(), new Glyph(bm.advance(), 0, 0, 0, 0, 0f, 0f, 0f, 0f));
                continue;
            }
            final int packedW = bm.w() + GLYPH_GUTTER * 2;
            final int packedH = bm.h() + GLYPH_GUTTER * 2;
            if (curX + packedW > atlasSize) {
                curX = 0;
                curY += rowH + 1;
                rowH = 0;
            }
            if (curY + packedH > atlasSize) {
                STBTruetype.stbtt_FreeBitmap(bm.pixels());
                continue;
            }
            final int glyphX = curX + GLYPH_GUTTER;
            final int glyphY = curY + GLYPH_GUTTER;
            AtlasGlyphWriter.writeGrayscaleGlyphWithGutter(img, bm.pixels(), glyphX, glyphY, bm.w(), bm.h(), GLYPH_GUTTER);
            final float u0 = glyphX / (float) atlasSize;
            final float v0 = glyphY / (float) atlasSize;
            final float u1 = (glyphX + bm.w()) / (float) atlasSize;
            final float v1 = (glyphY + bm.h()) / (float) atlasSize;
            glyphs.put(bm.code(), new Glyph(bm.advance(), bm.xoff(), bm.yoff(), bm.w(), bm.h(), u0, v0, u1, v1));
            STBTruetype.stbtt_FreeBitmap(bm.pixels());
            rowH = Math.max(rowH, packedH);
            curX += packedW + 1;
        }
        final Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
        for (int ci = 0; ci < charset.length(); ci++) {
            for (int cj = 0; cj < charset.length(); cj++) {
                final int a = charset.charAt(ci);
                final int b = charset.charAt(cj);
                final int kern = STBTruetype.stbtt_GetCodepointKernAdvance(info, a, b);
                if (kern != 0) {
                    kernings.computeIfAbsent(a, key -> new HashMap<>()).put(b, kern * scale);
                }
            }
        }
        final DynamicTexture dynTex = new DynamicTexture(img::toString, img);
        final Identifier texId = Identifier.fromNamespaceAndPath("walksylib", "bitmap_font_atlas/" + physicalSize + "x_" + System.identityHashCode(info));
        Minecraft.getInstance().getTextureManager().register(texId, dynTex);
        return new BitmapFontAtlas(dynTex, glyphs, kernings, baselinePx, physicalSize);
    }

    AbstractTexture texture() {
        return this.texture;
    }

    int countRenderable(final String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            final Glyph g = this.glyph(text.charAt(i));
            if (g != null && g.renderable()) {
                n++;
            }
        }
        return n;
    }

    boolean pixelArt() {
        return this.pixelArt;
    }

    int physicalSize() {
        return this.physicalSize;
    }

    void buildVerticesRaw(final ByteBuffer buf, final Matrix3x2f pose, final String text, final float x, final float y, final int color, @Nullable final OverlayBounds bounds, final float layoutScale, @Nullable final ContentStretch stretch, final boolean snap) {
        final float sx = DevicePixels.perLogicalX();
        final float sy = DevicePixels.perLogicalY();
        float penXPx = x * sx;
        final float rawBaseY = (y * sy + this.baselinePx * layoutScale) / sy;
        final float baseYPx = (snap ? DevicePixels.snapY(rawBaseY, pose) : rawBaseY) * sy;
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            final int ch = text.charAt(i);
            final Glyph g = this.glyph(ch);
            if (g == null) {
                continue;
            }
            penXPx += this.kerning(prev, ch) * layoutScale;
            if (g.renderable()) {
                final float glyphXPx = snap ? DevicePixels.snapX(penXPx / sx, pose) * sx : penXPx;
                g.emitRaw(buf, pose, glyphXPx, baseYPx, sx, sy, color, bounds, layoutScale, stretch);
            }
            penXPx += g.advance() * layoutScale;
            prev = ch;
        }
    }

    void emitVertices(final WLVertexSink sink, final Matrix3x2f pose, final String text, final float x, final float y, final int color, final float layoutScale, final boolean snap) {
        final float sx = DevicePixels.perLogicalX();
        final float sy = DevicePixels.perLogicalY();
        float penXPx = x * sx;
        final float rawBaseY = (y * sy + this.baselinePx * layoutScale) / sy;
        final float baseYPx = (snap ? DevicePixels.snapY(rawBaseY, pose) : rawBaseY) * sy;
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            final int ch = text.charAt(i);
            final Glyph g = this.glyph(ch);
            if (g == null) {
                continue;
            }
            penXPx += this.kerning(prev, ch) * layoutScale;
            if (g.renderable()) {
                final float glyphXPx = snap ? DevicePixels.snapX(penXPx / sx, pose) * sx : penXPx;
                g.emitSink(sink, pose, glyphXPx, baseYPx, sx, sy, color, layoutScale);
            }
            penXPx += g.advance() * layoutScale;
            prev = ch;
        }
    }

    WLTexture filtered(final boolean linear) {
        if (linear) {
            if (this.linearTexture == null) {
                this.linearTexture = WLRenderSystem.withFilter(WLRenderSystem.texture(this.texture), WLFilter.LINEAR);
            }
            return this.linearTexture;
        }
        if (this.nearestTexture == null) {
            this.nearestTexture = WLRenderSystem.withFilter(WLRenderSystem.texture(this.texture), WLFilter.NEAREST);
        }
        return this.nearestTexture;
    }

    float lineExtent(final float layoutScale) {
        return (this.physicalSize * 1.5f + this.baselinePx) * layoutScale / DevicePixels.perLogicalY();
    }

    float layoutWidth(final String text, final float layoutScale) {
        return this.widthPx(text) * layoutScale / DevicePixels.perLogicalX();
    }

    private @Nullable Glyph glyph(final int ch) {
        return ch < ASCII ? this.asciiGlyphs[ch] : this.glyphs.get(ch);
    }

    private float kerning(final int prev, final int ch) {
        if (prev < 0) {
            return 0f;
        }
        if (prev < ASCII && ch < ASCII) {
            return this.asciiKerning[prev * ASCII + ch];
        }
        final Map<Integer, Float> kern = this.kernings.get(prev);
        return kern == null ? 0f : kern.getOrDefault(ch, 0f);
    }

    float width(final String text) {
        return this.widthPx(text) / DevicePixels.perLogicalX();
    }

    private float widthPx(final String text) {
        float w = 0f;
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            final int ch = text.charAt(i);
            final Glyph g = this.glyph(ch);
            if (g == null) {
                continue;
            }
            w += this.kerning(prev, ch);
            w += g.advance();
            prev = ch;
        }
        return w;
    }

    @Override
    public String toString() {
        return "BitmapFontAtlas{" + this.physicalSize + "px}";
    }

    private record Glyph(float advance, int xoff, int yoff, int w, int h, float u0, float v0, float u1, float v1) {

        boolean renderable() {
            return this.w > 0 && this.h > 0;
        }

        void emitRaw(final ByteBuffer buf, final Matrix3x2f pose, final float xPx, final float baselineYPx, final float sx, final float sy, final int color, @Nullable final OverlayBounds bounds, final float layoutScale, @Nullable final ContentStretch stretch) {
            final float px0 = (xPx + this.xoff * layoutScale) / sx;
            final float py0 = (baselineYPx + this.yoff * layoutScale) / sy;
            final float px1 = (xPx + (this.xoff + this.w) * layoutScale) / sx;
            final float py1 = (baselineYPx + (this.yoff + this.h) * layoutScale) / sy;
            MsdfGlyph.writeVertex(buf, pose, px0, py0, this.u0, this.v0, color, bounds, stretch);
            MsdfGlyph.writeVertex(buf, pose, px0, py1, this.u0, this.v1, color, bounds, stretch);
            MsdfGlyph.writeVertex(buf, pose, px1, py1, this.u1, this.v1, color, bounds, stretch);
            MsdfGlyph.writeVertex(buf, pose, px1, py0, this.u1, this.v0, color, bounds, stretch);
        }

        void emitSink(final WLVertexSink sink, final Matrix3x2f pose, final float xPx, final float baselineYPx, final float sx, final float sy, final int color, final float layoutScale) {
            final float px0 = (xPx + this.xoff * layoutScale) / sx;
            final float py0 = (baselineYPx + this.yoff * layoutScale) / sy;
            final float px1 = (xPx + (this.xoff + this.w) * layoutScale) / sx;
            final float py1 = (baselineYPx + (this.yoff + this.h) * layoutScale) / sy;
            sink.posed(pose, px0, py0).uv(this.u0, this.v0).color(color);
            sink.posed(pose, px0, py1).uv(this.u0, this.v1).color(color);
            sink.posed(pose, px1, py1).uv(this.u1, this.v1).color(color);
            sink.posed(pose, px1, py0).uv(this.u1, this.v0).color(color);
        }
    }
}
