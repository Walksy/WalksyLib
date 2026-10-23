package main.walksy.lib.core.render;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.element.WLElement;
import main.walksy.lib.core.render.font.msdf.BitmapTextRenderQueue;
import main.walksy.lib.core.render.font.msdf.MsdfRenderQueue;
import main.walksy.lib.core.render.glass.LiquidGlassComposite;
import main.walksy.lib.core.render.glass.LiquidGlassPrecomputeRuntime;
import main.walksy.lib.core.render.glass.LiquidGlassUniforms;
import main.walksy.lib.core.render.glass.runtime.LiquidGlassAnim;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLGpu;
import main.walksy.lib.core.render.gpu.WLPass;
import main.walksy.lib.core.render.gpu.WLPipeline;
import main.walksy.lib.core.render.gpu.WLPipelineSpec;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.gpu.WLTransform;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import main.walksy.lib.core.render.gpu.WLVertexBuffer;
import main.walksy.lib.core.render.perf.UiProfiler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WLRenderSystem {
    private static final double MAX_FRAME_DELTA_SECONDS = 0.1;
    private static final boolean VULKANMOD = FabricLoader.getInstance().isModLoaded("vulkanmod");
    private static final float TICKS_PER_SECOND = 20f;
    private static final ContentStratum CONTENT_STRATUM = new ContentStratum();
    private static final ContentDefocus CONTENT_DEFOCUS = new ContentDefocus();
    private static final Cursors CURSORS = new Cursors();
    private static final LiquidGlassAnim GLASS_ANIM = new LiquidGlassAnim();
    private static final OverlayBounds EARLY_TEXT_BOUNDS = new OverlayBounds();
    private static final OverlayBounds CONTENT_BOUNDS = new OverlayBounds();
    private static final OverlayBounds POPOVER_CONTENT_BOUNDS = new OverlayBounds();
    private static LiquidGlassUniforms glassUniforms;
    private static LiquidGlassPrecomputeRuntime glassRuntime;
    private static float frameDeltaSeconds;
    private static long lastFrameNanos = -1L;
    private static int motionDepth;
    private static int hudDepth;
    private static final List<WLElement> HUD_TEXT = new ArrayList<>();
    private static GuiContext hudTextContext;
    private static long frameId;
    private static boolean presentHookSeen;
    private static boolean frameEndHookSeen;
    private static boolean topDeferred;

    private WLRenderSystem() {
    }

    public static void beginFrame() {
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.beginFrame();
        }
        final long now = System.nanoTime();
        final double deltaSeconds = lastFrameNanos < 0L ? 0.0 : Math.min(Math.max((now - lastFrameNanos) / 1_000_000_000.0, 0.0), MAX_FRAME_DELTA_SECONDS);
        lastFrameNanos = now;
        frameDeltaSeconds = (float) deltaSeconds;
        glassUniforms().beginFrame(deltaSeconds);
        GLASS_ANIM.update(deltaSeconds);
        motionDepth = 0;
        HUD_TEXT.clear();
        hudTextContext = null;
        frameId++;
        CONTENT_STRATUM.discardStale();
        EARLY_TEXT_BOUNDS.reset();
        CONTENT_BOUNDS.reset();
        POPOVER_CONTENT_BOUNDS.reset();
    }

    public static long frameId() {
        return frameId;
    }

    public static void beginHud() {
        hudDepth++;
    }

    public static void endHud() {
        hudDepth = Math.max(0, hudDepth - 1);
        if (hudDepth == 0 && !HUD_TEXT.isEmpty()) {
            final GuiContext context = hudTextContext;
            for (final WLElement element : HUD_TEXT) {
                context.submit(element);
            }
            HUD_TEXT.clear();
            hudTextContext = null;
        }
    }

    public static void deferHudText(final GuiContext context, final WLElement element) {
        hudTextContext = context;
        HUD_TEXT.add(element);
    }

    public static boolean inHud() {
        return hudDepth > 0;
    }
    public static void beginMotion() {
        motionDepth++;
    }

    public static void endMotion() {
        motionDepth = Math.max(0, motionDepth - 1);
    }

    public static boolean inMotion() {
        return motionDepth > 0;
    }

    public static void discardFrameTime() {
        lastFrameNanos = System.nanoTime();
    }

    public static void beginScreenFrame() {
        CURSORS.beginFrame();
    }

    public static void endScreenFrame(final WLGraphics2D graphics) {
        CURSORS.apply(graphics);
    }

    public static void renderGlass() {
        LiquidGlassComposite.renderMain();
    }

    public static void renderGlassTop() {
        LiquidGlassComposite.renderTop();
    }

    public static void finishGuiFrame() {
        if (presentHookSeen && glassUniforms().getTopCount() > 0) {
            topDeferred = true;
            return;
        }
        renderGlassTop();
    }

    public static boolean takeDeferredTop() {
        presentHookSeen = true;
        final boolean deferred = topDeferred;
        topDeferred = false;
        return deferred;
    }

    public static boolean takeDeferredTopAtFrameEnd() {
        frameEndHookSeen = true;
        return takeDeferredTop();
    }

    public static boolean frameEndHookSeen() {
        return frameEndHookSeen;
    }

    public static void flushText() {
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.TEXT_TAIL);
        }
        MsdfRenderQueue.flush();
        BitmapTextRenderQueue.flush();
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.TEXT_TAIL);
        }
    }

    public static void flushDev() {
        if (!WalksyLibConstants.DEV_BUILD) {
            return;
        }
        UiProfiler.begin(UiProfiler.Section.MONITOR);
        CONTENT_STRATUM.flushDev(mainTarget());
        MsdfRenderQueue.flushDev();
        BitmapTextRenderQueue.flushDev();
        UiProfiler.end(UiProfiler.Section.MONITOR);
    }

    public static TextQueueMark textMark() {
        return new TextQueueMark(MsdfRenderQueue.queuedCount(), BitmapTextRenderQueue.queuedCount());
    }

    public static void markEarlyText(final TextQueueMark mark) {
        MsdfRenderQueue.markEarlySince(mark.msdf());
        BitmapTextRenderQueue.markEarlySince(mark.bitmap());
    }

    public static void markContentText(final TextQueueMark mark) {
        MsdfRenderQueue.markContentSince(mark.msdf());
        BitmapTextRenderQueue.markContentSince(mark.bitmap());
    }

    public static void markPopoverText(final TextQueueMark mark) {
        MsdfRenderQueue.markPopoverContentSince(mark.msdf());
        BitmapTextRenderQueue.markPopoverContentSince(mark.bitmap());
    }

    public static void markDevText(final TextQueueMark mark) {
        MsdfRenderQueue.markDevSince(mark.msdf());
        BitmapTextRenderQueue.markDevSince(mark.bitmap());
    }

    public static void markTopText(final TextQueueMark mark) {
        MsdfRenderQueue.markTopSince(mark.msdf());
        BitmapTextRenderQueue.markTopSince(mark.bitmap());
    }

    public static boolean hasEarlyText() {
        return CONTENT_STRATUM.hasEarlyContent() || MsdfRenderQueue.hasEarly() || BitmapTextRenderQueue.hasEarly();
    }

    public static boolean hasContentText() {
        return MsdfRenderQueue.hasContent() || BitmapTextRenderQueue.hasContent();
    }

    public static boolean hasPopoverText() {
        return MsdfRenderQueue.hasPopoverContent() || BitmapTextRenderQueue.hasPopoverContent();
    }

    public static void flushEarlyText(final WLTexture target) {
        CONTENT_STRATUM.flushEarly(target);
        MsdfRenderQueue.flushEarly(target);
        BitmapTextRenderQueue.flushEarly(target);
    }

    public static void flushContentText(final WLTexture target) {
        MsdfRenderQueue.flushContent(target);
        BitmapTextRenderQueue.flushContent(target);
    }

    public static void flushPopoverText(final WLTexture target) {
        MsdfRenderQueue.flushPopoverContent(target);
        BitmapTextRenderQueue.flushPopoverContent(target);
    }

    public static void flushTopText() {
        MsdfRenderQueue.flushTop();
        BitmapTextRenderQueue.flushTop();
    }

    public static ContentStratum contentStratum() {
        return CONTENT_STRATUM;
    }

    public static ContentDefocus contentDefocus() {
        return CONTENT_DEFOCUS;
    }

    public static Cursors cursors() {
        return CURSORS;
    }

    public static float frameDeltaSeconds() {
        return frameDeltaSeconds;
    }

    public static float frameDeltaTicks() {
        return frameDeltaSeconds * TICKS_PER_SECOND;
    }

    public static LiquidGlassAnim glassAnim() {
        return GLASS_ANIM;
    }

    public static LiquidGlassUniforms glassUniforms() {
        if (glassUniforms == null) {
            glassUniforms = new LiquidGlassUniforms();
        }
        return glassUniforms;
    }

    public static LiquidGlassPrecomputeRuntime glassRuntime() {
        if (glassRuntime == null) {
            glassRuntime = new LiquidGlassPrecomputeRuntime();
        }
        return glassRuntime;
    }

    public static OverlayBounds earlyTextBounds() {
        return EARLY_TEXT_BOUNDS;
    }

    public static OverlayBounds contentBounds() {
        return CONTENT_BOUNDS;
    }

    public static OverlayBounds popoverContentBounds() {
        return POPOVER_CONTENT_BOUNDS;
    }

    public static WLPipeline pipeline(final WLPipelineSpec spec) {
        return WLGpu.pipeline(spec);
    }

    public static WLUniformBuffer uniformBuffer(final String label, final int size) {
        return WLGpu.uniformBuffer(label, size);
    }

    public static WLVertexBuffer vertexBuffer(final String label) {
        return WLGpu.vertexBuffer(label);
    }

    public static WLTexture colorTexture(final String label, final int width, final int height) {
        return WLGpu.colorTexture(label, width, height);
    }

    public static WLTexture texture(final AbstractTexture texture) {
        return WLGpu.texture(texture);
    }

    public static WLTexture texture(final Identifier id) {
        return WLGpu.texture(id);
    }

    public static WLTexture withFilter(final WLTexture texture, final WLFilter filter) {
        return WLGpu.withFilter(texture, filter);
    }

    public static WLTexture mainTarget() {
        return WLGpu.mainTarget();
    }

    public static int targetWidth() {
        return WLGpu.targetWidth();
    }

    public static int targetHeight() {
        return WLGpu.targetHeight();
    }

    public static void clear(final WLTexture texture) {
        WLGpu.clear(texture);
    }

    public static void copyMainTo(final WLTexture destination) {
        WLGpu.copyMainTo(destination);
    }

    public static boolean supportsTextureCopy() {
        return !VULKANMOD;
    }

    public static boolean vulkanMod() {
        return VULKANMOD;
    }

    public static boolean hudBatching() {
        return !VULKANMOD && GuiContext.vanillaBatching();
    }

    public static boolean renderTargetsBottomUp() {
        return WLGpu.renderTargetsBottomUp();
    }

    public static WLTransform transform(final Matrix4f modelView) {
        return WLGpu.transform(modelView);
    }

    public static WLPass pass(final Supplier<String> label, final WLTexture target) {
        return WLGpu.pass(label, target);
    }
}
