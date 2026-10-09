package main.walksy.lib.core.render.glass;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.render.gpu.Std140Writer;
import main.walksy.lib.core.render.gpu.WLBlend;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLMapping;
import main.walksy.lib.core.render.gpu.WLPass;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import main.walksy.lib.core.render.gpu.WLVertexFormat;
import main.walksy.lib.core.render.perf.UiProfiler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public final class LiquidGlassPrecomputeRuntime {
    private static final int MAX_DOWNSAMPLE_LEVEL = 3;
    private static final int MIN_EFFECTIVE_RADIUS = 12;
    private static final List<String> BLUR_UNIFORMS = List.of("SamplerInfo", "Config");
    private static final List<String> BLUR_SAMPLERS = List.of("DiffuseSampler");
    private static final WLPipelineSpec BLUR_SPEC = new WLPipelineSpec("blur", "core/blit_fullscreen", "program/blur", WLVertexFormat.POSITION, WLBlend.OPAQUE, BLUR_UNIFORMS, BLUR_SAMPLERS);
    private static final WLPipelineSpec OVERLAY_COMPOSITE_SPEC = new WLPipelineSpec("overlay_composite", "core/blit_fullscreen", "program/blur", WLVertexFormat.POSITION, WLBlend.PREMULTIPLIED, BLUR_UNIFORMS, BLUR_SAMPLERS);
    private WLPipeline blurPipeline;
    private final WLTexture[] chain = new WLTexture[MAX_DOWNSAMPLE_LEVEL + 1];
    private final WLTexture[] temp = new WLTexture[MAX_DOWNSAMPLE_LEVEL + 1];
    private int pyramidW = -1;
    private int pyramidH = -1;
    private WLTexture scene;
    private final HashMap<Integer, WLTexture> blurredByRadius = new HashMap<>();
    private final WLUniformBuffer[] samplerInfoByLevel = new WLUniformBuffer[MAX_DOWNSAMPLE_LEVEL + 1];
    private final HashMap<Integer, WLUniformBuffer> blurConfigX = new HashMap<>();
    private final HashMap<Integer, WLUniformBuffer> blurConfigY = new HashMap<>();
    private WLUniformBuffer downsampleConfig;
    private static final int MAX_RADIUS = LiquidGlassConstants.MAX_BLUR_RADIUS;
    private static final int SHADER_WEIGHTS = 257;
    private WLPipeline overlayCompositePipeline;
    private static final Supplier<String> OVERLAY_COMPOSITE_LABEL = () -> "walksylib overlay composite";
    private final boolean[] overlayHasInk = new boolean[3];
    private final boolean[] overlayBlurHasInk = new boolean[3];
    private static final String[] OVERLAY_NAMES = { "walksylib earlyText", "walksylib contentOverlay", "walksylib popoverContentOverlay" };
    private final WLTexture[] overlay = new WLTexture[3];
    private static final Supplier<String> SCENE_REGION_LABEL = () -> "walksylib scene capture (region)";
    private WLUniformBuffer copySamplerInfo;
    private int copySamplerW = -1;
    private int copySamplerH = -1;
    private final HashMap<Integer, int[]> blurBuiltRegion = new HashMap<>();
    private final HashMap<Integer, int[]> blurStaleRegion = new HashMap<>();
    private final WLTexture[] overlayBlur = new WLTexture[3];
    private final int[][] overlayBlurRegion = new int[3][];
    private static final int[] EMPTY_REGION = {0, 0, 0, 0};

    private static int levelForRadius(final int radius) {
        final int r = Math.max(0, Math.min(radius, MAX_RADIUS));
        int level = 0;
        while (level < MAX_DOWNSAMPLE_LEVEL && (r >> (level + 1)) >= MIN_EFFECTIVE_RADIUS) {
            level++;
        }
        return level;
    }

    public LiquidGlassPrecomputeRuntime() {
    }

    private void ensurePipelines() {
        if (this.blurPipeline == null) {
            this.blurPipeline = WLRenderSystem.pipeline(BLUR_SPEC);
        }
        if (this.overlayCompositePipeline == null) {
            this.overlayCompositePipeline = WLRenderSystem.pipeline(OVERLAY_COMPOSITE_SPEC);
        }
        if (this.downsampleConfig == null) {
            this.downsampleConfig = createBlurConfigBuffer("walksylib blur downsample");
            this.uploadBlur(this.downsampleConfig, 0f, 0f, 0);
        }
    }

    public void compositeOverlay(final WLTexture overlay, final WLTexture target, final int[] scissorPx) {
        if (scissorPx != null && (scissorPx[2] <= 0 || scissorPx[3] <= 0)) {
            return;
        }
        this.ensurePipelines();
        this.ensureCopySamplerInfo(WLRenderSystem.targetWidth(), WLRenderSystem.targetHeight());
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
        }
        try (final WLPass pass = WLRenderSystem.pass(OVERLAY_COMPOSITE_LABEL, target)) {
            pass.pipeline(this.overlayCompositePipeline);
            pass.uniform("SamplerInfo", this.copySamplerInfo);
            pass.uniform("Config", this.downsampleConfig);
            pass.texture("DiffuseSampler", overlay, WLFilter.NEAREST);
            if (scissorPx != null) {
                pass.scissor(scissorPx[0], scissorPx[1], scissorPx[2], scissorPx[3]);
            }
            pass.drawFullscreenQuad();
        }
    }

    private static int blurConfigSize() {
        return 16 + SHADER_WEIGHTS * 16;
    }

    private static WLUniformBuffer createBlurConfigBuffer(final String name) {
        return WLRenderSystem.uniformBuffer(name, blurConfigSize());
    }

    private void ensurePyramid(final int w, final int h) {
        if (this.pyramidW == w && this.pyramidH == h) {
            return;
        }
        this.pyramidW = w;
        this.pyramidH = h;
        for (int level = 0; level <= MAX_DOWNSAMPLE_LEVEL; level++) {
            final int lw = Math.max(1, w >> level);
            final int lh = Math.max(1, h >> level);
            if (this.chain[level] != null) {
                this.chain[level].close();
                this.chain[level] = null;
            }
            if (this.temp[level] != null) {
                this.temp[level].close();
                this.temp[level] = null;
            }
            if (level > 0) {
                this.chain[level] = WLRenderSystem.colorTexture("walksylib blurChain L" + level, lw, lh);
            }
            this.temp[level] = WLRenderSystem.colorTexture("walksylib blurTemp L" + level, lw, lh);
            if (this.samplerInfoByLevel[level] == null) {
                this.samplerInfoByLevel[level] = WLRenderSystem.uniformBuffer("walksylib SamplerInfo L" + level, 16);
            }
            try (final WLMapping map = this.samplerInfoByLevel[level].map()) {
                map.writer().putVec2((float) lw, (float) lh).putVec2((float) lw, (float) lh);
            }
        }
    }

    private boolean ensureScene(final int w, final int h) {
        if (this.scene == null || this.scene.width() != w || this.scene.height() != h) {
            if (this.scene != null) {
                this.scene.close();
            }
            this.scene = WLRenderSystem.colorTexture("walksylib sceneCopy", w, h);
            return true;
        }
        return false;
    }

    private WLTexture prepareOverlay(final int index, final boolean willDraw) {
        final int w = WLRenderSystem.targetWidth();
        final int h = WLRenderSystem.targetHeight();
        WLTexture tex = this.overlay[index];
        if (tex == null || tex.width() != w || tex.height() != h) {
            if (tex != null) {
                tex.close();
            }
            tex = WLRenderSystem.colorTexture(OVERLAY_NAMES[index], w, h);
            this.overlay[index] = tex;
            this.overlayHasInk[index] = true;
        }
        if (willDraw || this.overlayHasInk[index]) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.count(UiProfiler.Counter.OVERLAY_PIXELS, (long) w * h);
            }
            WLRenderSystem.clear(tex);
        }
        this.overlayHasInk[index] = willDraw;
        return tex;
    }

    public WLTexture prepareEarlyTextTarget(final boolean willDraw) {
        return this.prepareOverlay(0, willDraw);
    }

    public WLTexture prepareContentTarget(final boolean willDraw) {
        return this.prepareOverlay(1, willDraw);
    }

    public WLTexture preparePopoverContentTarget(final boolean willDraw) {
        return this.prepareOverlay(2, willDraw);
    }

    public WLTexture captureScene() {
        final int w = WLRenderSystem.targetWidth();
        final int h = WLRenderSystem.targetHeight();
        this.ensureScene(w, h);
        this.copyMainToScene(w, h);
        this.markSceneDirtyAll();
        return this.scene;
    }

    public WLTexture lastSceneView() {
        return this.scene;
    }

    public WLTexture captureSceneRegion(final int x, final int y, final int rw, final int rh) {
        final int w = WLRenderSystem.targetWidth();
        final int h = WLRenderSystem.targetHeight();
        if (this.ensureScene(w, h) || rw <= 0 || rh <= 0 || (x == 0 && y == 0 && rw == w && rh == h)) {
            this.copyMainToScene(w, h);
            this.markSceneDirtyAll();
            return this.scene;
        }
        this.markSceneDirty(x, y, rw, rh);
        this.drawMainToScene(w, h, new int[] { x, y, rw, rh });
        return this.scene;
    }

    private void copyMainToScene(final int w, final int h) {
        if (WLRenderSystem.supportsTextureCopy()) {
            WLRenderSystem.copyMainTo(this.scene);
            return;
        }
        this.drawMainToScene(w, h, null);
    }

    private void drawMainToScene(final int w, final int h, final int[] scissorPx) {
        this.ensurePipelines();
        this.ensureCopySamplerInfo(w, h);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
        }
        try (final WLPass pass = WLRenderSystem.pass(SCENE_REGION_LABEL, this.scene)) {
            pass.pipeline(this.blurPipeline);
            pass.uniform("SamplerInfo", this.copySamplerInfo);
            pass.uniform("Config", this.downsampleConfig);
            pass.texture("DiffuseSampler", WLRenderSystem.mainTarget(), WLFilter.NEAREST);
            if (scissorPx != null) {
                pass.scissor(scissorPx[0], scissorPx[1], scissorPx[2], scissorPx[3]);
            }
            pass.drawFullscreenQuad();
        }
    }

    private void ensureCopySamplerInfo(final int w, final int h) {
        if (this.copySamplerInfo == null) {
            this.copySamplerInfo = WLRenderSystem.uniformBuffer("walksylib SamplerInfo copy", 16);
        }
        if (this.copySamplerW != w || this.copySamplerH != h) {
            this.copySamplerW = w;
            this.copySamplerH = h;
            try (final WLMapping map = this.copySamplerInfo.map()) {
                map.writer().putVec2((float) w, (float) h).putVec2((float) w, (float) h);
            }
        }
    }

    private void markSceneDirtyAll() {
        for (final Integer key : this.blurStaleRegion.keySet()) {
            this.blurStaleRegion.put(key, null);
        }
    }

    private void markSceneDirty(final int x, final int y, final int w, final int h) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final int[] rect = {x, y, w, h};
        for (final Integer key : this.blurStaleRegion.keySet()) {
            this.blurStaleRegion.put(key, unionRegion(this.blurStaleRegion.get(key), rect));
        }
    }

    private boolean blurIsReusable(final int radius, final int lw, final int lh, final int[] padded) {
        final WLTexture tex = this.blurredByRadius.get(radius);
        if (tex == null || tex.width() != lw || tex.height() != lh) {
            return false;
        }
        if (!this.blurBuiltRegion.containsKey(radius) || !this.blurStaleRegion.containsKey(radius)) {
            return false;
        }
        return regionCovers(this.blurBuiltRegion.get(radius), padded) && !regionIntersects(this.blurStaleRegion.get(radius), padded);
    }

    private void markBlurBuilt(final int radius, final int[] padded) {
        this.blurBuiltRegion.put(radius, padded);
        this.blurStaleRegion.put(radius, EMPTY_REGION);
    }

    private static boolean regionCovers(@Nullable final int[] outer, @Nullable final int[] inner) {
        if (outer == null) {
            return true;
        }
        if (outer[2] <= 0 || outer[3] <= 0) {
            return inner != null && (inner[2] <= 0 || inner[3] <= 0);
        }
        if (inner == null) {
            return false;
        }
        if (inner[2] <= 0 || inner[3] <= 0) {
            return true;
        }
        return inner[0] >= outer[0] && inner[1] >= outer[1] && inner[0] + inner[2] <= outer[0] + outer[2] && inner[1] + inner[3] <= outer[1] + outer[3];
    }

    private static boolean regionIntersects(@Nullable final int[] a, @Nullable final int[] b) {
        if (a != null && (a[2] <= 0 || a[3] <= 0)) {
            return false;
        }
        if (b != null && (b[2] <= 0 || b[3] <= 0)) {
            return false;
        }
        if (a == null || b == null) {
            return true;
        }
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    private void ensureOutputForRadius(final int w, final int h, final int radius) {
        final WLTexture tex = this.blurredByRadius.get(radius);
        if (tex == null || tex.width() != w || tex.height() != h) {
            if (tex != null) {
                tex.close();
            }
            this.blurredByRadius.put(radius, WLRenderSystem.colorTexture("walksylib blurred r=" + radius, w, h));
        }
    }

    private static float[] gaussian(int radius) {
        radius = Math.max(0, Math.min(radius, MAX_RADIUS));
        final float sigma = radius / 3.0f;
        if (radius == 0) {
            return new float[] {1f};
        }
        final float[] kernel = new float[radius + 1];
        float sum = 0f;
        for (int i = 0; i <= radius; i++) {
            final float w = (float) Math.exp(-0.5 * ((float) i * (float) i) / (sigma * sigma));
            kernel[i] = w;
            sum += (i == 0) ? w : (2f * w);
        }
        for (int i = 0; i <= radius; i++) {
            kernel[i] /= sum;
        }
        return kernel;
    }

    static int sideTaps(final int effectiveRadius) {
        return (Math.max(0, effectiveRadius) + 1) / 2;
    }

    private void uploadBlur(final WLUniformBuffer ubo, final float dx, final float dy, int radius) {
        radius = Math.max(0, Math.min(radius, MAX_RADIUS));
        final float[] kernel = gaussian(radius);
        final int taps = Math.min(sideTaps(radius), SHADER_WEIGHTS - 1);
        final float[] tapWeight = new float[taps + 1];
        final float[] tapOffset = new float[taps + 1];
        tapWeight[0] = kernel[0];
        for (int t = 1; t <= taps; t++) {
            final int a = 2 * t - 1;
            final int b = 2 * t;
            final float wa = kernel[a];
            final float wb = b <= radius ? kernel[b] : 0f;
            final float w = wa + wb;
            tapWeight[t] = w;
            tapOffset[t] = w > 0f ? (a * wa + b * wb) / w : a;
        }
        try (final WLMapping map = ubo.map()) {
            final Std140Writer b = map.writer();
            b.putVec4(dx, dy, (float) taps, 0f);
            for (int i = 0; i < SHADER_WEIGHTS; i++) {
                b.putFloat(i <= taps ? tapWeight[i] : 0f);
                b.putFloat(i <= taps ? tapOffset[i] : 0f);
                b.align(16);
            }
        }
    }

    private WLUniformBuffer configFor(final int radius, final boolean vertical) {
        final HashMap<Integer, WLUniformBuffer> cache = vertical ? this.blurConfigY : this.blurConfigX;
        WLUniformBuffer ubo = cache.get(radius);
        if (ubo == null) {
            final int effective = Math.max(0, Math.min(radius, MAX_RADIUS)) >> levelForRadius(radius);
            ubo = createBlurConfigBuffer("walksylib blur " + (vertical ? "Y" : "X") + " r=" + radius);
            this.uploadBlur(ubo, vertical ? 0f : 1f, vertical ? 1f : 0f, effective);
            cache.put(radius, ubo);
        }
        return ubo;
    }

    private void blit(final Supplier<String> label, final WLTexture source, final WLTexture target, final WLUniformBuffer config, final int level, final int[] scissor) {
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
            if (scissor != null) {
                UiProfiler.blurPass((long) scissor[2] * scissor[3]);
            } else {
                UiProfiler.blurPass((long) Math.max(1, WLRenderSystem.targetWidth() >> level) * Math.max(1, WLRenderSystem.targetHeight() >> level));
            }
        }
        try (final WLPass pass = WLRenderSystem.pass(label, target)) {
            pass.pipeline(this.blurPipeline);
            if (scissor != null) {
                pass.scissor(scissor[0], scissor[1], scissor[2], scissor[3]);
            }
            pass.uniform("SamplerInfo", this.samplerInfoByLevel[level]);
            pass.uniform("Config", config);
            pass.texture("DiffuseSampler", source, WLFilter.LINEAR);
            pass.drawFullscreenQuad();
        }
    }

    private static int[] paddedRegion(final int[] readRegionPx, final Collection<Integer> radii, final int w, final int h) {
        int padPx = 0;
        for (final int r : radii) {
            if (r <= 0) {
                continue;
            }
            final int clamped = Math.min(r, MAX_RADIUS);
            final int level = levelForRadius(clamped);
            final int effective = clamped >> level;
            padPx = Math.max(padPx, (2 * effective + 8) << level);
        }
        final int x0 = Math.max(0, readRegionPx[0] - padPx);
        final int y0 = Math.max(0, readRegionPx[1] - padPx);
        final int x1 = Math.min(w, readRegionPx[0] + readRegionPx[2] + padPx);
        final int y1 = Math.min(h, readRegionPx[1] + readRegionPx[3] + padPx);
        if (x0 == 0 && y0 == 0 && x1 == w && y1 == h) {
            return null;
        }
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    private static int[] regionAtLevel(final int[] regionPx, final int level, final int lw, final int lh) {
        if (regionPx == null) {
            return null;
        }
        final int x0 = Math.max(0, regionPx[0] >> level);
        final int y0 = Math.max(0, regionPx[1] >> level);
        final int step = 1 << level;
        final int x1 = Math.min(lw, (regionPx[0] + regionPx[2] + step - 1) >> level);
        final int y1 = Math.min(lh, (regionPx[1] + regionPx[3] + step - 1) >> level);
        if (x1 <= x0 || y1 <= y0) {
            return null;
        }
        if (x0 == 0 && y0 == 0 && x1 == lw && y1 == lh) {
            return null;
        }
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    public void buildBlurs(final WLTexture source, final Collection<Integer> radii, final int[] readRegionPx) {
        if (radii == null || radii.isEmpty()) {
            return;
        }
        this.ensurePipelines();
        final int w = WLRenderSystem.targetWidth();
        final int h = WLRenderSystem.targetHeight();
        this.ensurePyramid(w, h);
        final int[] padded = (readRegionPx == null || readRegionPx[2] <= 0 || readRegionPx[3] <= 0) ? null : paddedRegion(readRegionPx, radii, w, h);
        final List<Integer> toBuild = new ArrayList<>(radii.size());
        for (final int radius : radii) {
            if (radius <= 0) {
                continue;
            }
            final int level = levelForRadius(Math.min(radius, MAX_RADIUS));
            if (this.blurIsReusable(radius, Math.max(1, w >> level), Math.max(1, h >> level), padded)) {
                continue;
            }
            toBuild.add(radius);
        }
        if (toBuild.isEmpty()) {
            return;
        }
        int maxLevel = 0;
        for (final int radius : toBuild) {
            maxLevel = Math.max(maxLevel, levelForRadius(Math.min(radius, MAX_RADIUS)));
        }
        for (int level = 1; level <= maxLevel; level++) {
            final WLTexture src = (level == 1) ? source : this.chain[level - 1];
            final int lv = level;
            this.blit(() -> "walksylib blur downsample L" + lv, src, this.chain[level], this.downsampleConfig, level, regionAtLevel(padded, level, Math.max(1, w >> level), Math.max(1, h >> level)));
        }
        for (final int radius : toBuild) {
            final int r = Math.min(radius, MAX_RADIUS);
            final int level = levelForRadius(r);
            final int lw = Math.max(1, w >> level);
            final int lh = Math.max(1, h >> level);
            this.ensureOutputForRadius(lw, lh, radius);
            final WLTexture src = (level == 0) ? source : this.chain[level];
            final int[] lvlScissor = regionAtLevel(padded, level, lw, lh);
            if (WalksyLibConstants.DEV_BUILD) {
                final long area = lvlScissor != null ? (long) lvlScissor[2] * lvlScissor[3] : (long) lw * lh;
                UiProfiler.count(UiProfiler.Counter.BLUR_TAPS, 2L * area * (2L * sideTaps(r >> level) + 1L));
            }
            this.blit(() -> "walksylib blur X r=" + r, src, this.temp[level], this.configFor(radius, false), level, lvlScissor);
            this.blit(() -> "walksylib blur Y r=" + r, this.temp[level], this.blurredByRadius.get(radius), this.configFor(radius, true), level, lvlScissor);
            this.markBlurBuilt(radius, padded);
        }
    }

    public WLTexture getBlurredViewForRadius(final int radius) {
        return this.blurredByRadius.get(radius);
    }

    public WLTexture blurContentOverlay(final WLTexture contentView, final boolean hasInk, @Nullable final float[] inkRect) {
        return this.blurOverlay(contentView, LiquidGlassConstants.BLUR_MILKY, "contentBlurred", 0, hasInk, inkRect);
    }

    public WLTexture blurPopoverContentOverlay(final WLTexture popoverContentView, final boolean hasInk, @Nullable final float[] inkRect) {
        return this.blurOverlay(popoverContentView, LiquidGlassConstants.BLUR_MILKY, "popoverContentBlurred", 1, hasInk, inkRect);
    }

    public WLTexture blurEarlyTextOverlay(final WLTexture textView, final boolean hasInk, @Nullable final float[] inkRect) {
        return this.blurOverlay(textView, LiquidGlassConstants.BLUR_MILKY, "earlyTextBlurred", 2, hasInk, inkRect);
    }

    private static int[] regionFromDeviceRect(@Nullable final float[] rect, final int w, final int h) {
        if (rect == null) {
            return null;
        }
        final int x0 = Math.max(0, (int) Math.floor(rect[0]));
        final int y0 = Math.max(0, (int) Math.floor(rect[1]));
        final int x1 = Math.min(w, (int) Math.ceil(rect[2]));
        final int y1 = Math.min(h, (int) Math.ceil(rect[3]));
        if (x1 <= x0 || y1 <= y0) {
            return EMPTY_REGION;
        }
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    private static int[] unionRegion(@Nullable final int[] a, @Nullable final int[] b) {
        if (a == null || b == null) {
            return null;
        }
        if (b[2] <= 0 || b[3] <= 0) {
            return a;
        }
        if (a[2] <= 0 || a[3] <= 0) {
            return b;
        }
        final int x0 = Math.min(a[0], b[0]);
        final int y0 = Math.min(a[1], b[1]);
        final int x1 = Math.max(a[0] + a[2], b[0] + b[2]);
        final int y1 = Math.max(a[1] + a[3], b[1] + b[3]);
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    private WLTexture blurOverlay(final WLTexture source, final int radius, final String name, final int index, final boolean hasInk, @Nullable final float[] inkRect) {
        this.ensurePipelines();
        final int w = WLRenderSystem.targetWidth();
        final int h = WLRenderSystem.targetHeight();
        final int level = levelForRadius(radius);
        final int lw = Math.max(1, w >> level);
        final int lh = Math.max(1, h >> level);
        if (this.overlayBlur[index] == null || this.overlayBlur[index].width() != lw || this.overlayBlur[index].height() != lh) {
            if (this.overlayBlur[index] != null) {
                this.overlayBlur[index].close();
            }
            this.overlayBlur[index] = WLRenderSystem.colorTexture("walksylib " + name, lw, lh);
            this.overlayBlurHasInk[index] = true;
            WLRenderSystem.clear(this.overlayBlur[index]);
            this.overlayBlurRegion[index] = EMPTY_REGION;
        }
        if (!hasInk) {
            if (this.overlayBlurHasInk[index]) {
                WLRenderSystem.clear(this.overlayBlur[index]);
                this.overlayBlurHasInk[index] = false;
                this.overlayBlurRegion[index] = EMPTY_REGION;
            }
            return this.overlayBlur[index];
        }
        this.ensurePyramid(w, h);
        final int[] current = regionFromDeviceRect(inkRect, w, h);
        final int[] padded = current == null ? null : paddedRegion(current, List.of(radius), w, h);
        final int[] area = unionRegion(padded, this.overlayBlurRegion[index]);
        this.overlayBlurRegion[index] = padded;
        for (int l = 1; l <= level; l++) {
            final WLTexture src = (l == 1) ? source : this.chain[l - 1];
            final int lv = l;
            this.blit(() -> "walksylib " + name + " downsample L" + lv, src, this.chain[l], this.downsampleConfig, l, regionAtLevel(area, l, Math.max(1, w >> l), Math.max(1, h >> l)));
        }
        final WLTexture src = (level == 0) ? source : this.chain[level];
        final int[] levelArea = regionAtLevel(area, level, lw, lh);
        this.blit(() -> "walksylib " + name + " X", src, this.temp[level], this.configFor(radius, false), level, levelArea);
        this.blit(() -> "walksylib " + name + " Y", this.temp[level], this.overlayBlur[index], this.configFor(radius, true), level, levelArea);
        this.overlayBlurHasInk[index] = true;
        return this.overlayBlur[index];
    }
}
