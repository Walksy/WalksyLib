package main.walksy.lib.core.render.glass;

import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLFilter;
import main.walksy.lib.core.render.gpu.WLPass;
import main.walksy.lib.core.render.gpu.WLTexture;
import main.walksy.lib.core.render.perf.UiProfiler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class LiquidGlassComposite {
    private static final Supplier<String> GLASS_PASS_LABEL = () -> "walksylib liquid glass pass";
    private static final String[] BLUR_SAMPLERS = {"Sampler1", "Sampler2", "Sampler3", "Sampler4", "Sampler5"};
    private static final String[] STRATA_SAMPLERS = {"Sampler6", "Sampler7", "Sampler8", "Sampler9", "Sampler10", "Sampler11"};
    private static final Supplier<String> LAYER_PASS_LABEL = () -> "walksylib liquid glass layer";
    private static long decidedFrame = -1L;
    private static boolean decidedLayer;
    private static long claimedFrame = -1L;

    private record Strata(@Nullable WLTexture text, @Nullable WLTexture textBlurred, @Nullable WLTexture content, @Nullable WLTexture contentBlurred, @Nullable WLTexture popover, @Nullable WLTexture popoverBlurred) {
        private static final Strata NONE = new Strata(null, null, null, null, null, null);
    }

    private LiquidGlassComposite() {
    }

    public static void renderMain() {
        final LiquidGlassUniforms uniforms = WLRenderSystem.glassUniforms();
        if (uniforms.getCount() <= 0) {
            return;
        }
        if (decideLayer()) {
            if (uniforms.layerUpdate() && uniforms.splitRequested()) {
                renderLayer(uniforms);
            }
            return;
        }
        final boolean strataOff = WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.STRATA);
        final boolean textUsed = !strataOff && WLRenderSystem.hasEarlyText();
        final boolean contentUsed = !strataOff && (WLRenderSystem.contentStratum().hasRowContent() || WLRenderSystem.hasContentText());
        final boolean popoverUsed = !strataOff && (WLRenderSystem.contentStratum().hasPopoverContent() || WLRenderSystem.hasPopoverText());
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.count(UiProfiler.Counter.GLASS_SHAPES, uniforms.getCount());
            UiProfiler.begin(UiProfiler.Section.STRATA_CLEAR);
        }
        final WLTexture textOverlay = WLRenderSystem.glassRuntime().prepareEarlyTextTarget(textUsed);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.STRATA_CLEAR);
            UiProfiler.begin(UiProfiler.Section.STRATA_TEXT);
        }
        if (textUsed) {
            WLRenderSystem.flushEarlyText(textOverlay);
        }
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.STRATA_TEXT);
            UiProfiler.begin(UiProfiler.Section.STRATA_CLEAR);
        }
        final WLTexture contentOverlay = WLRenderSystem.glassRuntime().prepareContentTarget(contentUsed);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.STRATA_CLEAR);
        }
        if (contentUsed) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.begin(UiProfiler.Section.STRATA_SHAPES);
            }
            WLRenderSystem.contentStratum().flush(contentOverlay);
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.STRATA_SHAPES);
                UiProfiler.begin(UiProfiler.Section.STRATA_TEXT);
            }
            WLRenderSystem.flushContentText(contentOverlay);
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.STRATA_TEXT);
            }
        }
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.STRATA_CLEAR);
        }
        final WLTexture popoverContentOverlay = WLRenderSystem.glassRuntime().preparePopoverContentTarget(popoverUsed);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.STRATA_CLEAR);
        }
        if (popoverUsed) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.begin(UiProfiler.Section.STRATA_SHAPES);
            }
            WLRenderSystem.contentStratum().flushPopover(popoverContentOverlay);
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.STRATA_SHAPES);
                UiProfiler.begin(UiProfiler.Section.STRATA_TEXT);
            }
            WLRenderSystem.flushPopoverText(popoverContentOverlay);
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.STRATA_TEXT);
            }
        }
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.GLASS_UPLOAD);
        }
        uniforms.setStrataUsed(textUsed, contentUsed, popoverUsed);
        uniforms.upload();
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.GLASS_UPLOAD);
        }
        final boolean blurOff = WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.BLUR);
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.begin(UiProfiler.Section.BLUR);
        }
        final WLTexture contentBlurred = WLRenderSystem.glassRuntime().blurContentOverlay(contentOverlay, !blurOff && contentUsed && uniforms.blurredContentIsRead(), WLRenderSystem.contentBounds().toDeviceRect());
        final WLTexture popoverContentBlurred = WLRenderSystem.glassRuntime().blurPopoverContentOverlay(popoverContentOverlay, !blurOff && popoverUsed && uniforms.blurredPopoverIsRead(), WLRenderSystem.popoverContentBounds().toDeviceRect());
        final WLTexture textBlurred = WLRenderSystem.glassRuntime().blurEarlyTextOverlay(textOverlay, !blurOff && textUsed && uniforms.blurredTextIsRead(), WLRenderSystem.earlyTextBounds().toDeviceRect());
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.end(UiProfiler.Section.BLUR);
        }
        uniforms.setStratumBounds(WLRenderSystem.earlyTextBounds().toDeviceRect(), WLRenderSystem.contentBounds().toDeviceRect(), WLRenderSystem.popoverContentBounds().toDeviceRect());
        compositeSlots(uniforms, 0, uniforms.getMainSlotCount(), blurOff, new Strata(textOverlay, textBlurred, contentOverlay, contentBlurred, popoverContentOverlay, popoverContentBlurred));
    }

    private static boolean decideLayer() {
        final long frame = WLRenderSystem.frameId();
        if (decidedFrame == frame) {
            return decidedLayer;
        }
        decidedFrame = frame;
        final LiquidGlassUniforms uniforms = WLRenderSystem.glassUniforms();
        if (uniforms.getCount() <= 0) {
            decidedLayer = false;
            return false;
        }
        final boolean strataOff = WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.STRATA);
        final boolean textUsed = !strataOff && WLRenderSystem.hasEarlyText();
        final boolean contentUsed = !strataOff && (WLRenderSystem.contentStratum().hasRowContent() || WLRenderSystem.hasContentText());
        final boolean popoverUsed = !strataOff && (WLRenderSystem.contentStratum().hasPopoverContent() || WLRenderSystem.hasPopoverText());
        uniforms.setStrataUsed(textUsed, contentUsed, popoverUsed);
        uniforms.upload();
        decidedLayer = uniforms.layerMode();
        return decidedLayer;
    }

    public static int @Nullable [] layerArea(final long frame) {
        if (frame != WLRenderSystem.frameId() || claimedFrame == frame) {
            return null;
        }
        if (!decideLayer()) {
            if (!WLRenderSystem.glassUniforms().splitRequested()) {
                claimedFrame = frame;
                renderMain();
            }
            return null;
        }
        claimedFrame = frame;
        final LiquidGlassUniforms uniforms = WLRenderSystem.glassUniforms();
        if (uniforms.layerUpdate() && !uniforms.splitRequested()) {
            renderLayer(uniforms);
        }
        return uniforms.layerArea();
    }

    private static void renderLayer(final LiquidGlassUniforms uniforms) {
        final LiquidGlassPrecomputeRuntime runtime = WLRenderSystem.glassRuntime();
        final WLTexture mainTarget = WLRenderSystem.mainTarget();
        final int fbW = WLRenderSystem.targetWidth();
        final int fbH = WLRenderSystem.targetHeight();
        runtime.beginComposite();
        final List<Integer> radii = uniforms.getBlurRadiiForSlot(0);
        if (!uniforms.layerBlurInline()) {
            runtime.buildBlurs(mainTarget, radii, uniforms.getBlurRegion(0, fbW, fbH));
        }
        final int[] area = uniforms.layerArea();
        try (final WLPass pass = WLRenderSystem.pass(LAYER_PASS_LABEL, runtime.layerTarget())) {
            pass.pipeline(LiquidGlassPipelines.getGuiPipeline());
            pass.uniform("SamplerInfo", uniforms.getSamplerInfoBuffer());
            pass.uniform("CustomUniforms", uniforms.getCustomUniformsBuffer(0));
            pass.uniform("WidgetInfo", uniforms.getWidgetInfoBuffer(0));
            pass.uniform("BgConfig", uniforms.getBgConfigBuffer());
            pass.texture("Sampler0", mainTarget, WLFilter.LINEAR);
            for (final String sampler : STRATA_SAMPLERS) {
                pass.texture(sampler, mainTarget, WLFilter.LINEAR);
            }
            for (int i = 0; i < BLUR_SAMPLERS.length; i++) {
                WLTexture view = null;
                final int widgetLevel = i - 1;
                if (widgetLevel >= 0 && widgetLevel < radii.size()) {
                    view = runtime.getBlurredViewForRadius(radii.get(widgetLevel));
                }
                pass.texture(BLUR_SAMPLERS[i], view != null ? view : mainTarget, WLFilter.LINEAR);
            }
            pass.scissor(area[0], area[1], area[2], area[3]);
            pass.drawFullscreenQuad();
        }
        uniforms.markLayerValid();
    }

    public static void renderTop() {
        final LiquidGlassUniforms uniforms = WLRenderSystem.glassUniforms();
        if (uniforms.getTopCount() > 0) {
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.count(UiProfiler.Counter.GLASS_SHAPES, uniforms.getTopCount());
                UiProfiler.begin(UiProfiler.Section.GLASS_UPLOAD);
            }
            uniforms.upload();
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.GLASS_UPLOAD);
            }
            final boolean blurOff = WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.BLUR);
            compositeSlots(uniforms, uniforms.getMainSlotCount(), uniforms.getSlotCount(), blurOff, Strata.NONE);
        }
        WLRenderSystem.contentStratum().flushTop(WLRenderSystem.mainTarget());
        WLRenderSystem.flushTopText();
    }

    private static void compositeSlots(final LiquidGlassUniforms uniforms, final int fromSlot, final int toSlot, final boolean blurOff, final Strata strata) {
        if (toSlot <= fromSlot) {
            return;
        }
        final LiquidGlassPrecomputeRuntime runtime = WLRenderSystem.glassRuntime();
        final WLTexture mainTarget = WLRenderSystem.mainTarget();
        final int fbW = WLRenderSystem.targetWidth();
        final int fbH = WLRenderSystem.targetHeight();
        final boolean unscissored = WLRenderSystem.glassAnim().debugStep() <= 1.5f || (WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.SCISSOR));
        final int lastSlot = WalksyLibConstants.DEV_BUILD && UiProfiler.isBypassed(UiProfiler.Bypass.COMPOSITE) ? fromSlot : toSlot;
        if (WalksyLibConstants.DEV_BUILD) {
            UiProfiler.count(UiProfiler.Counter.GLASS_LAYERS, lastSlot - fromSlot);
        }
        runtime.beginComposite();
        for (int slot = fromSlot; slot < lastSlot; slot++) {
            final int[] area = unscissored ? null : uniforms.getPassScissor(slot, fbW, fbH);
            if (area != null && (area[2] <= 0 || area[3] <= 0)) {
                markStrataWritten(runtime, compositeStrata(uniforms, slot, fbW, fbH, mainTarget, strata));
                continue;
            }
            final boolean over = !unscissored && uniforms.slotBlendsOver(slot);
            final List<Integer> radii = blurOff && !over ? List.of() : uniforms.getBlurRadiiForSlot(slot);
            final int[] blurRegion = uniforms.getBlurRegion(slot, fbW, fbH);
            final WLTexture sceneView;
            if (over) {
                sceneView = null;
            } else {
                if (WalksyLibConstants.DEV_BUILD) {
                    UiProfiler.begin(UiProfiler.Section.CAPTURE);
                }
                final int[] captureRegion = unscissored ? null : union(union(area, uniforms.getReadRegion(slot, fbW, fbH)), runtime.blurSourceRegion(blurRegion, radii));
                if (WalksyLibConstants.DEV_BUILD) {
                    UiProfiler.count(UiProfiler.Counter.CAPTURE_PIXELS, captureRegion == null ? (long) fbW * fbH : (long) captureRegion[2] * captureRegion[3]);
                }
                sceneView = runtime.captureSceneRegion(captureRegion);
                if (WalksyLibConstants.DEV_BUILD) {
                    UiProfiler.end(UiProfiler.Section.CAPTURE);
                }
            }
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.begin(UiProfiler.Section.BLUR);
            }
            runtime.buildBlurs(over ? mainTarget : sceneView, radii, blurRegion);
            final int[][] boxes = unscissored ? null : uniforms.getPassScissors(slot, fbW, fbH);
            final WLTexture fallback = over ? runtime.getBlurredViewForRadius(radii.get(0)) : sceneView;
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.BLUR);
                UiProfiler.begin(UiProfiler.Section.COMPOSITE);
                long shaded = 0L;
                if (boxes == null) {
                    shaded = (long) fbW * fbH;
                } else {
                    for (final int[] box : boxes) {
                        shaded += (long) box[2] * box[3];
                    }
                }
                UiProfiler.compositePass(shaded);
                UiProfiler.count(UiProfiler.Counter.RENDER_PASSES, 1L);
            }
            try (final WLPass pass = WLRenderSystem.pass(GLASS_PASS_LABEL, mainTarget)) {
                pass.pipeline(over ? LiquidGlassPipelines.getGuiOverPipeline() : LiquidGlassPipelines.getGuiPipeline());
                pass.uniform("SamplerInfo", uniforms.getSamplerInfoBuffer());
                pass.uniform("CustomUniforms", uniforms.getCustomUniformsBuffer(slot));
                pass.uniform("WidgetInfo", uniforms.getWidgetInfoBuffer(slot));
                pass.uniform("BgConfig", uniforms.getBgConfigBuffer());
                pass.texture("Sampler0", fallback, WLFilter.LINEAR);
                pass.texture("Sampler6", orScene(strata.text(), fallback), WLFilter.LINEAR);
                pass.texture("Sampler7", orScene(strata.content(), fallback), WLFilter.LINEAR);
                pass.texture("Sampler8", orScene(strata.contentBlurred(), fallback), WLFilter.LINEAR);
                pass.texture("Sampler9", orScene(strata.popover(), fallback), WLFilter.LINEAR);
                pass.texture("Sampler10", orScene(strata.popoverBlurred(), fallback), WLFilter.LINEAR);
                pass.texture("Sampler11", orScene(strata.textBlurred(), fallback), WLFilter.LINEAR);
                for (int i = 0; i < BLUR_SAMPLERS.length; i++) {
                    WLTexture view = null;
                    final int widgetLevel = i - 1;
                    if (widgetLevel >= 0 && widgetLevel < radii.size()) {
                        view = runtime.getBlurredViewForRadius(radii.get(widgetLevel));
                    }
                    pass.texture(BLUR_SAMPLERS[i], view != null ? view : fallback, WLFilter.LINEAR);
                }
                if (boxes == null) {
                    pass.noScissor();
                    pass.drawFullscreenQuad();
                } else {
                    for (final int[] box : boxes) {
                        pass.scissor(box[0], box[1], box[2], box[3]);
                        pass.drawFullscreenQuad();
                    }
                }
            }
            if (WalksyLibConstants.DEV_BUILD) {
                UiProfiler.end(UiProfiler.Section.COMPOSITE);
            }
            runtime.markTargetWritten(area);
            markStrataWritten(runtime, compositeStrata(uniforms, slot, fbW, fbH, mainTarget, strata));
        }
    }

    private static void markStrataWritten(final LiquidGlassPrecomputeRuntime runtime, @Nullable final int[] written) {
        if (written != null) {
            runtime.markTargetWritten(written);
        }
    }

    private static int[] union(@Nullable final int[] a, @Nullable final int[] b) {
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

    private static WLTexture orScene(@Nullable final WLTexture view, final WLTexture scene) {
        return view != null ? view : scene;
    }

    private static int[] compositeStrata(final LiquidGlassUniforms uniforms, final int slot, final int fbW, final int fbH, final WLTexture target, final Strata strata) {
        int[] written = null;
        for (int which = 0; which < 2; which++) {
            final boolean content = which == 0;
            if (content ? !uniforms.contentCompositesAfter(slot) : !uniforms.popoverCompositesAfter(slot)) {
                continue;
            }
            final WLTexture overlay = content ? strata.content() : strata.popover();
            if (overlay == null) {
                continue;
            }
            final int[] rect = uniforms.getOverlayCompositeScissor(content, fbW, fbH);
            if (rect != null && (rect[2] <= 0 || rect[3] <= 0)) {
                continue;
            }
            WLRenderSystem.glassRuntime().compositeOverlay(overlay, target, rect);
            final int[] box = rect == null ? new int[] {0, 0, fbW, fbH} : rect;
            if (written == null) {
                written = new int[] {box[0], box[1], box[2], box[3]};
            } else {
                final int x0 = Math.min(written[0], box[0]);
                final int y0 = Math.min(written[1], box[1]);
                final int x1 = Math.max(written[0] + written[2], box[0] + box[2]);
                final int y1 = Math.max(written[1] + written[3], box[1] + box[3]);
                written = new int[] {x0, y0, x1 - x0, y1 - y0};
            }
        }
        return written;
    }
}
