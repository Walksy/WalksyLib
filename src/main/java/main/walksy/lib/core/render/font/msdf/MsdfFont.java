package main.walksy.lib.core.render.font.msdf;

import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.WLRenderSystem;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.gpu.Std140Size;
import main.walksy.lib.core.render.gpu.WLMapping;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import main.walksy.lib.core.render.font.msdf.FontData.GlyphData;
import main.walksy.lib.core.render.font.msdf.FontData.KerningData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import main.walksy.lib.core.render.ContentStretch;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.perf.UiProfiler;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.OverlayBounds;
import main.walksy.lib.core.render.WLGraphics2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class MsdfFont {
    static final int UBO_SIZE;
    public static final String ASCII_PRINTABLE;
    private static final String ELLIPSIS = "...";
    private static final int GLYPH_SIZE = 128;
    private static final int PADDING = 12;
    private static final int ATLAS_GUTTER = 2;
    private static final byte ON_EDGE = (byte) 128;
    private static final float PIXEL_DIST = 255.0f / (2.0f * PADDING);
    private static final float DISTANCE_RANGE = 255.0f / PIXEL_DIST;
    private static final int BITMAP_MAX_DEVICE_PX = 160;
    private static final int STRETCH_OVERSAMPLE = 3;
    private static final int MOTION_OVERSAMPLE = 2;
    private final AbstractTexture texture;
    private final float metricsLineHeight;
    private final float metricsDescender;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;
    private final float onEdge;
    private final float distanceRange;
    private final boolean isMsdf;
    private final ByteBuffer ttfBuffer;
    private final STBTTFontinfo ttfInfo;
    private final String ttfCharset;
    private final Map<BitmapAtlasKey, BitmapFontAtlas> bitmapAtlases;
    private float thickness = 0.0f;
    private float smoothness = 0.5f;
    private boolean outline = false;
    private int outlineColor = 0xFF000000;
    private float outlineThickness = 0.0f;
    final WLUniformBuffer paramsUbo;

    static {
        final Std140Size c = new Std140Size();
        c.putVec4();
        c.putVec4();
        c.putVec4();
        UBO_SIZE = c.get();
    }

    static {
        final StringBuilder sb = new StringBuilder(96);
        for (int i = 32; i < 127; i++) {
            sb.append((char) i);
        }
        ASCII_PRINTABLE = sb.toString();
    }

    private record BitmapAtlasKey(int physicalSize) {}

    private MsdfFont(final AbstractTexture texture, final float lineHeight, final float descender, final Map<Integer, MsdfGlyph> glyphs, final Map<Integer, Map<Integer, Float>> kernings, final float onEdge, final float distanceRange, final boolean isMsdf, final ByteBuffer ttfBuffer, final STBTTFontinfo ttfInfo, final String ttfCharset) {
        this.texture = texture;
        this.metricsLineHeight = lineHeight;
        this.metricsDescender = descender;
        this.glyphs = glyphs;
        this.kernings = kernings;
        this.onEdge = onEdge;
        this.distanceRange = distanceRange;
        this.isMsdf = isMsdf;
        this.ttfBuffer = ttfBuffer;
        this.ttfInfo = ttfInfo;
        this.ttfCharset = ttfCharset;
        this.bitmapAtlases = ttfInfo != null ? new ConcurrentHashMap<>() : Map.of();
        this.paramsUbo = WLRenderSystem.uniformBuffer("walksylib msdf params", UBO_SIZE);
        this.uploadParams();
    }

    public MsdfFont thickness(final float v) {
        this.thickness = v;
        this.uploadParams();
        return this;
    }

    public MsdfFont smoothness(final float v) {
        this.smoothness = v;
        this.uploadParams();
        return this;
    }

    public MsdfFont outline(final int color, final float thick) {
        this.outline = thick > 0f;
        this.outlineColor = color;
        this.outlineThickness = thick;
        this.uploadParams();
        return this;
    }

    public MsdfFont noOutline() {
        return this.outline(0, 0f);
    }

    public void extract(final WLGraphics2D graphics, final String text, final float x, final float y, final float size, final int color) {
        if (this.countRenderable(text) == 0) {
            return;
        }
        if (UiStyle.minecraft() && this.extractPixel(graphics, text, x, y, size, color)) {
            return;
        }
        if (WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.TEXT)) {
            return;
        }
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final WLRect scissor = graphics.scissor();
        final BitmapChoice bitmap = this.bitmapFor(size, pose);
        if (WLRenderSystem.contentDefocus().isActive()) {
            for (int i = 0; i < WLRenderSystem.contentDefocus().tapCount(); i++) {
                final int c = WLRenderSystem.contentDefocus().tapColor(color, i);
                if ((c >>> 24) == 0) {
                    continue;
                }
                final float tx = x + WLRenderSystem.contentDefocus().tapDx(i);
                final float ty = y + WLRenderSystem.contentDefocus().tapDy(i);
                if (bitmap != null) {
                    BitmapTextRenderQueue.enqueue(bitmap.atlas(), bitmap.layoutScale(), text, pose, tx, ty, c, scissor);
                } else {
                    MsdfRenderQueue.enqueue(this, text, pose, tx, ty, size, c, scissor);
                }
            }
            return;
        }
        if (Theme.get().isDark()) {
            final float offset = PixelFont.shadowOffset(size);
            final int shadow = PixelFont.shadowColor(color);
            if (bitmap != null) {
                BitmapTextRenderQueue.enqueue(bitmap.atlas(), bitmap.layoutScale(), text, pose, x + offset, y + offset, shadow, scissor);
            } else {
                MsdfRenderQueue.enqueue(this, text, pose, x + offset, y + offset, size, shadow, scissor);
            }
        }
        if (bitmap != null) {
            BitmapTextRenderQueue.enqueue(bitmap.atlas(), bitmap.layoutScale(), text, pose, x, y, color, scissor);
            return;
        }
        MsdfRenderQueue.enqueue(this, text, pose, x, y, size, color, scissor);
    }

    public void extractWithShadow(final WLGraphics2D graphics, final String text, final float x, final float y, final float size, final int color) {
        this.extractWithShadow(graphics, text, x, y, size, color, 1.0f, 1.0f, shadowColor(color));
    }

    public void extractWithShadow(final WLGraphics2D graphics, final String text, final float x, final float y, final float size, final int color, final float shadowOffsetX, final float shadowOffsetY, final int shadowColor) {
        if (this.countRenderable(text) == 0) {
            return;
        }
        if (WLRenderSystem.contentDefocus().isActive() || UiStyle.minecraft()) {
            this.extract(graphics, text, x, y, size, color);
            return;
        }
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final WLRect scissor = graphics.scissor();
        final BitmapChoice bitmap = this.bitmapFor(size, pose);
        if (bitmap != null) {
            BitmapTextRenderQueue.enqueue(bitmap.atlas(), bitmap.layoutScale(), text, pose, x + shadowOffsetX, y + shadowOffsetY, shadowColor, scissor);
            BitmapTextRenderQueue.enqueue(bitmap.atlas(), bitmap.layoutScale(), text, pose, x, y, color, scissor);
            return;
        }
        MsdfRenderQueue.enqueue(this, text, pose, x + shadowOffsetX, y + shadowOffsetY, size, shadowColor, scissor);
        MsdfRenderQueue.enqueue(this, text, pose, x, y, size, color, scissor);
    }

    private static int shadowColor(final int color) {
        return (color & 0xFF000000) | ((color & 0x00FCFCFC) >> 2);
    }

    void buildVerticesRaw(final ByteBuffer buf, final Matrix3x2f pose, final String text, final float size, float x, final float y, final int color, @Nullable final OverlayBounds bounds, @Nullable final ContentStretch stretch) {
        final float baselineY = y + (this.metricsLineHeight + this.metricsDescender) * size;
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            final int ch = text.charAt(i);
            final MsdfGlyph g = this.glyphs.get(ch);
            if (g == null || !g.isRenderable()) {
                continue;
            }
            final Map<Integer, Float> kern = this.kernings.get(prev);
            if (kern != null) {
                x += kern.getOrDefault(ch, 0f) * size;
            }
            x += g.emitRaw(buf, pose, size, x, baselineY, color, bounds, stretch);
            prev = ch;
        }
    }

    AbstractTexture getTexture() {
        return this.texture;
    }

    WLUniformBuffer getParamsUbo() {
        return this.paramsUbo;
    }

    private record BitmapChoice(BitmapFontAtlas atlas, float layoutScale) {}

    private @Nullable BitmapChoice bitmapFor(final float size, final Matrix3x2f pose) {
        final BitmapFontAtlas normal = this.getBitmapAtlas(size);
        if (normal == null) {
            return null;
        }
        if (ContentStretch.current() != null) {
            return this.denseFor(normal, STRETCH_OVERSAMPLE);
        }
        if (!DevicePixels.isNearlyUnscaled(pose)) {
            return this.denseFor(normal, MOTION_OVERSAMPLE);
        }
        return new BitmapChoice(normal, 1f);
    }

    public void warm(final float size) {
        this.getBitmapAtlas(size);
    }

    public void warmStretched(final float size) {
        final BitmapFontAtlas normal = this.getBitmapAtlas(size);
        if (normal != null) {
            this.denseFor(normal, STRETCH_OVERSAMPLE);
        }
    }

    private BitmapChoice denseFor(final BitmapFontAtlas normal, final int oversample) {
        final int normalPx = normal.physicalSize();
        final int densePx = Math.max(normalPx, Math.min(normalPx * oversample, BITMAP_MAX_DEVICE_PX));
        if (densePx == normalPx) {
            return new BitmapChoice(normal, 1f);
        }
        final BitmapFontAtlas dense = this.bitmapAtlases.computeIfAbsent(new BitmapAtlasKey(densePx), key -> BitmapFontAtlas.create(this.ttfInfo, this.ttfCharset, key.physicalSize()));
        return new BitmapChoice(dense, normalPx / (float) densePx);
    }

    private BitmapFontAtlas getBitmapAtlas(final float size) {
        if (this.ttfInfo == null || size <= 0f) {
            return null;
        }
        final int physicalSize = Math.max(1, Math.round(size * DevicePixels.perLogicalY()));
        if (physicalSize > BITMAP_MAX_DEVICE_PX) {
            return null;
        }
        final BitmapAtlasKey atlasKey = new BitmapAtlasKey(physicalSize);
        return this.bitmapAtlases.computeIfAbsent(atlasKey, key -> BitmapFontAtlas.create(this.ttfInfo, this.ttfCharset, key.physicalSize()));
    }

    private boolean extractPixel(final WLGraphics2D graphics, final String raw, final float x, final float y, final float size, final int color) {
        final BitmapFontAtlas atlas = PixelFont.atlas();
        if (atlas == null) {
            return false;
        }
        if (WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.TEXT)) {
            return true;
        }
        final String text = PixelFont.normalize(raw);
        final Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        final WLRect scissor = graphics.scissor();
        final float layoutScale = PixelFont.layoutScale(size);
        if (Theme.get().isDark()) {
            final float shadow = PixelFont.unit(size);
            BitmapTextRenderQueue.enqueue(atlas, layoutScale, text, pose, x + shadow, y + shadow, PixelFont.shadowColor(color), scissor);
        }
        BitmapTextRenderQueue.enqueue(atlas, layoutScale, text, pose, x, y, color, scissor);
        return true;
    }

    public float width(final String text, final float size) {
        if (UiStyle.minecraft()) {
            final BitmapFontAtlas pixel = PixelFont.atlas();
            if (pixel != null) {
                return PixelFont.width(pixel, text, size);
            }
        }
        final BitmapFontAtlas bitmapAtlas = this.getBitmapAtlas(size);
        if (bitmapAtlas != null) {
            return bitmapAtlas.width(text);
        }
        float w = 0f;
        int prev = -1;
        for (int i = 0; i < text.length(); i++) {
            final int ch = text.charAt(i);
            final MsdfGlyph g = this.glyphs.get(ch);
            if (g == null) {
                continue;
            }
            final Map<Integer, Float> kern = this.kernings.get(prev);
            if (kern != null) {
                w += kern.getOrDefault(ch, 0f) * size;
            }
            w += g.getAdvance() * size;
            prev = ch;
        }
        return w;
    }

    public String truncate(final String text, final float size, final float maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.width(text, size) <= maxWidth) {
            return text;
        }
        final float room = maxWidth - this.width(ELLIPSIS, size);
        if (room <= 0f) {
            return ELLIPSIS;
        }
        int end = text.length();
        while (end > 0 && this.width(text.substring(0, end), size) > room) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + ELLIPSIS;
    }

    public List<String> wrap(final String text, final float size, final float maxWidth) {
        final List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (final String word : text.trim().split("\\s+")) {
            final String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.width(candidate, size) > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    public float height(final float size) {
        if (UiStyle.minecraft() && PixelFont.atlas() != null) {
            return PixelFont.height(size);
        }
        return this.metricsLineHeight * size;
    }

    public boolean pixelated(final float size) {
        return UiStyle.minecraft() && PixelFont.atlas() != null;
    }

    public float topForPixelCenter(final float centerY, final float size) {
        return centerY - PixelFont.ascent(size) / 2f;
    }

    public float leftForPixelCenter(final float centerX, final String text, final float size) {
        return centerX - (this.width(text, size) - PixelFont.unit(size)) / 2f;
    }

    public float topForBaseline(final float baselineY, final float size) {
        if (UiStyle.minecraft() && PixelFont.atlas() != null) {
            return baselineY - PixelFont.ascent(size);
        }
        return baselineY - (this.metricsLineHeight + this.metricsDescender) * size;
    }

    int countRenderable(final String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            final MsdfGlyph g = this.glyphs.get((int) text.charAt(i));
            if (g != null && g.isRenderable()) {
                n++;
            }
        }
        return n;
    }

    void uploadParams() {
        if (this.paramsUbo == null) {
            return;
        }
        final float oA = ((this.outlineColor >> 24) & 0xFF) / 255f;
        final float oR = ((this.outlineColor >> 16) & 0xFF) / 255f;
        final float oG = ((this.outlineColor >> 8) & 0xFF) / 255f;
        final float oB = (this.outlineColor & 0xFF) / 255f;
        try (final WLMapping map = this.paramsUbo.map()) {
            final var b = map.writer();
            b.putVec4(this.onEdge, this.distanceRange, this.thickness, this.smoothness);
            b.putVec4(this.outlineThickness, this.outline ? 1f : 0f, this.isMsdf ? 1f : 0f, 0f);
            b.putVec4(oR, oG, oB, oA);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Identifier fontId;
        private Identifier dataId;
        private Identifier atlasId;
        private String charset = ASCII_PRINTABLE;

        private Builder() {
        }

        public Builder ttf(final Identifier id) {
            this.fontId = id;
            return this;
        }

        public Builder otf(final Identifier id) {
            this.fontId = id;
            return this;
        }

        public Builder font(final Identifier id) {
            this.fontId = id;
            return this;
        }

        public Builder data(final Identifier id) {
            this.dataId = id;
            return this;
        }

        public Builder atlas(final Identifier id) {
            this.atlasId = id;
            return this;
        }

        public Builder charset(final String cs) {
            this.charset = cs;
            return this;
        }

        public MsdfFont build() {
            if (this.fontId != null) {
                return this.buildFromFont();
            }
            if (this.dataId != null) {
                return this.buildFromJson();
            }
            throw new IllegalStateException("MsdfFont.Builder: call ttf()/otf()/font() or data()+atlas()");
        }

        private MsdfFont buildFromFont() {
            final Minecraft mc = Minecraft.getInstance();
            final ByteBuffer ttfBuf;
            final ResourceManager resourceManager = mc.getResourceManager();
            if (resourceManager == null) {
                throw new IllegalStateException("Cannot build MsdfFont before Minecraft resources are initialized: " + this.fontId);
            }
            try (final var stream = resourceManager.open(this.fontId)) {
                final byte[] raw = stream.readAllBytes();
                ttfBuf = BufferUtils.createByteBuffer(raw.length);
                ttfBuf.put(raw).flip();
            } catch (final IOException e) {
                throw new RuntimeException("Cannot open font: " + this.fontId, e);
            }
            final STBTTFontinfo info = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(info, ttfBuf)) {
                throw new RuntimeException("stbtt_InitFont failed (not a valid TTF/OTF?): " + this.fontId);
            }
            final float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, GLYPH_SIZE);
            final IntBuffer ascBuf = BufferUtils.createIntBuffer(1);
            final IntBuffer dscBuf = BufferUtils.createIntBuffer(1);
            final IntBuffer gapBuf = BufferUtils.createIntBuffer(1);
            STBTruetype.stbtt_GetFontVMetrics(info, ascBuf, dscBuf, gapBuf);
            final float ascender = ascBuf.get(0) * scale / GLYPH_SIZE;
            final float descender = dscBuf.get(0) * scale / GLYPH_SIZE;
            final float lineGap = gapBuf.get(0) * scale / GLYPH_SIZE;
            final float lineHeight = ascender - descender + lineGap;
            record GlyphBitmap(int code, ByteBuffer pixels, int w, int h, int xoff, int yoff) {}
            final List<GlyphBitmap> bitmaps = new ArrayList<>(this.charset.length());
            final IntBuffer wb = BufferUtils.createIntBuffer(1);
            final IntBuffer hb = BufferUtils.createIntBuffer(1);
            final IntBuffer xb = BufferUtils.createIntBuffer(1);
            final IntBuffer yb = BufferUtils.createIntBuffer(1);
            for (int i = 0; i < this.charset.length(); i++) {
                final int cp = this.charset.charAt(i);
                final ByteBuffer bm = STBTruetype.stbtt_GetCodepointSDF(info, scale, cp, PADDING, ON_EDGE, PIXEL_DIST, wb, hb, xb, yb);
                if (bm != null && wb.get(0) > 0 && hb.get(0) > 0) {
                    bitmaps.add(new GlyphBitmap(cp, bm, wb.get(0), hb.get(0), xb.get(0), yb.get(0)));
                }
            }
            final int atlasW = 2048;
            final int atlasH = 2048;
            int curX = 0;
            int curY = 0;
            int rowH = 0;
            record PackPos(int u0, int v0) {}
            final Map<Integer, PackPos> packPos = new HashMap<>(bitmaps.size());
            for (final GlyphBitmap bm : bitmaps) {
                final int packedW = bm.w() + ATLAS_GUTTER * 2;
                final int packedH = bm.h() + ATLAS_GUTTER * 2;
                if (curX + packedW > atlasW) {
                    curX = 0;
                    curY += rowH + 1;
                    rowH = 0;
                }
                if (curY + packedH > atlasH) {
                    break;
                }
                packPos.put(bm.code(), new PackPos(curX + ATLAS_GUTTER, curY + ATLAS_GUTTER));
                if (packedH > rowH) {
                    rowH = packedH;
                }
                curX += packedW + 1;
            }
            final NativeImage img = new NativeImage(NativeImage.Format.RGBA, atlasW, atlasH, true);
            img.fillRect(0, 0, atlasW, atlasH, 0);
            for (final GlyphBitmap bm : bitmaps) {
                final PackPos pos = packPos.get(bm.code());
                if (pos != null) {
                    AtlasGlyphWriter.writeGrayscaleGlyphWithGutter(img, bm.pixels(), pos.u0(), pos.v0(), bm.w(), bm.h(), ATLAS_GUTTER);
                }
                STBTruetype.stbtt_FreeSDF(bm.pixels());
            }
            final DynamicTexture dynTex = new DynamicTexture(img::toString, img);
            final String uid = this.fontId.toDebugFileName().replace("/", "_").replace(":", "_");
            final Identifier texId = Identifier.fromNamespaceAndPath("walksylib", "msdf_atlas/" + uid);
            mc.getTextureManager().register(texId, dynTex);
            final IntBuffer advBuf = BufferUtils.createIntBuffer(1);
            final IntBuffer lsbBuf = BufferUtils.createIntBuffer(1);
            final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
            for (final GlyphBitmap bm : bitmaps) {
                final PackPos pos = packPos.get(bm.code());
                if (pos == null) {
                    continue;
                }
                STBTruetype.stbtt_GetCodepointHMetrics(info, bm.code(), advBuf, lsbBuf);
                final float advance = advBuf.get(0) * scale / GLYPH_SIZE;
                final float pLeft = bm.xoff() / (float) GLYPH_SIZE;
                final float pTop = -bm.yoff() / (float) GLYPH_SIZE;
                final float pRight = pLeft + bm.w() / (float) GLYPH_SIZE;
                final float pBottom = pTop - bm.h() / (float) GLYPH_SIZE;
                glyphs.put(bm.code(), MsdfGlyph.fromRaw(bm.code(), advance, pLeft, pTop, pRight, pBottom, pos.u0(), pos.v0(), pos.u0() + bm.w(), pos.v0() + bm.h(), atlasW, atlasH));
            }
            final Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            for (int ci = 0; ci < this.charset.length(); ci++) {
                for (int cj = 0; cj < this.charset.length(); cj++) {
                    final int a = this.charset.charAt(ci);
                    final int b = this.charset.charAt(cj);
                    final int kern = STBTruetype.stbtt_GetCodepointKernAdvance(info, a, b);
                    if (kern != 0) {
                        kernings.computeIfAbsent(a, key -> new HashMap<>()).put(b, kern * scale / GLYPH_SIZE);
                    }
                }
            }
            final float onEdgeNorm = (ON_EDGE & 0xFF) / 255f;
            return new MsdfFont(dynTex, lineHeight, descender, glyphs, kernings, onEdgeNorm, DISTANCE_RANGE, false, ttfBuf, info, this.charset);
        }

        private MsdfFont buildFromJson() {
            if (this.atlasId == null) {
                throw new IllegalStateException("MsdfFont.Builder: atlas() required with data()");
            }
            final Minecraft mc = Minecraft.getInstance();
            final FontData fontData;
            final ResourceManager resourceManager = mc.getResourceManager();
            if (resourceManager == null) {
                throw new IllegalStateException("Cannot build MsdfFont before Minecraft resources are initialized: " + this.dataId);
            }
            try (final var stream = resourceManager.open(this.dataId); final var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                fontData = new Gson().fromJson(reader, FontData.class);
            } catch (final IOException e) {
                throw new RuntimeException("Cannot read MSDF font data: " + this.dataId, e);
            }
            final AbstractTexture tex = mc.getTextureManager().getTexture(this.atlasId);
            final float aw = fontData.atlas().width();
            final float ah = fontData.atlas().height();
            final Map<Integer, MsdfGlyph> glyphs = new HashMap<>();
            for (final GlyphData gd : fontData.glyphs()) {
                glyphs.put(gd.unicode(), new MsdfGlyph(gd, aw, ah));
            }
            final Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            if (fontData.kernings() != null) {
                for (final KerningData k : fontData.kernings()) {
                    kernings.computeIfAbsent(k.leftChar(), key -> new HashMap<>()).put(k.rightChar(), k.advance());
                }
            }
            final float lineHeight = fontData.metrics().lineHeight();
            final float descender = fontData.metrics().descender();
            final float range = fontData.atlas().range();
            return new MsdfFont(tex, lineHeight, descender, glyphs, kernings, 0.5f, range, true, null, null, null);
        }
}
}
