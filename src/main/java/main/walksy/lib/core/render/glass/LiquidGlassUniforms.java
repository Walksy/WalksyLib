package main.walksy.lib.core.render.glass;

import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.glass.runtime.LiquidGlassAnim;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.internal.AppearanceConfig;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.element.WLRect;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.render.glass.gui.LiquidGlassGuiElementRenderState;
import main.walksy.lib.core.render.gpu.Std140Size;
import main.walksy.lib.core.render.gpu.Std140Writer;
import main.walksy.lib.core.render.gpu.WLMapping;
import main.walksy.lib.core.render.gpu.WLUniformBuffer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3x2f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LiquidGlassUniforms {
    public static final int MAX_WIDGETS = 64;
    public static final int MAX_BLUR_LEVELS = 5;
    private static final long START_NANOS = System.nanoTime();
    private static final float NANOS_PER_SECOND = 1.0E9f;
    private static final float BLUR_FALLOFF_BIAS = 8.0f;
    private static final float SOFT_LENS_FLAG = 2.0f;
    private static final int MAX_LAYERS = 24;
    private static final Set<Integer> REPORTED_LAYER_OVERFLOW = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean REPORTED_WIDGET_OVERFLOW = new AtomicBoolean(false);
    private static final int EARLY_TEXT_LAYER = 20;
    private static final int CONTENT_LAYER = 15;
    private final WLUniformBuffer samplerInfo;
    private final WLUniformBuffer[] customUniforms = new WLUniformBuffer[MAX_LAYERS];
    private final WLUniformBuffer[] widgetInfoBySlot = new WLUniformBuffer[MAX_LAYERS];
    private final WLUniformBuffer bgConfig;
    private int slotCount;
    private int mainSlotCount;
    private boolean uploaded;
    private final float[] layerValuesBySlot = new float[MAX_LAYERS];
    private final boolean[] textAtStart = new boolean[MAX_LAYERS];
    private final boolean[] textAtEnd = new boolean[MAX_LAYERS];
    private final boolean[] contentAtStart = new boolean[MAX_LAYERS];
    private final boolean[] contentAtEnd = new boolean[MAX_LAYERS];
    private final boolean[] popoverAtStart = new boolean[MAX_LAYERS];
    private final boolean[] popoverAtEnd = new boolean[MAX_LAYERS];
    private int textCompositePass;
    private int contentCompositePass;
    private int popoverCompositePass;
    private final List<LiquidGlassGuiElementRenderState> widgets = new ArrayList<>();
    private final HashMap<Integer, Integer> widgetsPerLayer = new HashMap<>();
    private final List<LiquidGlassGuiElementRenderState> topWidgets = new ArrayList<>();
    private final HashMap<Integer, Integer> topWidgetsPerLayer = new HashMap<>();
    private final List<LiquidGlassGuiElementRenderState> ordered = new ArrayList<>();
    private boolean blurPassRequested = false;
    private boolean vanillaBlurWanted = false;
    private final List<List<Integer>> blurRadiiBySlot = new ArrayList<>();
    private final float[] slotBounds = new float[MAX_LAYERS * 4];
    private final boolean[] slotBoundsEmpty = new boolean[MAX_LAYERS];
    private final float[][] slotWidgetBounds = new float[MAX_LAYERS][];
    private final int[] slotWidgetBoundsCount = new int[MAX_LAYERS];
    private final float[] slotBlurRegion = new float[MAX_LAYERS * 4];
    private final boolean[] slotBlurRegionEmpty = new boolean[MAX_LAYERS];
    private float[] textStratumBounds;
    private float[] contentStratumBounds;
    private float[] popoverStratumBounds;
    private final HashMap<Long, FadeState> fades = new HashMap<>();
    private final HashSet<Long> fadesSeen = new HashSet<>();
    private double dtSeconds = 0.0;
    private final int[] layerStarts = new int[MAX_LAYERS];
    private final int[] layerCounts = new int[MAX_LAYERS];
    private final float[] layerValues = new float[MAX_LAYERS];
    private final int[] distinctRadii = new int[MAX_BLUR_LEVELS - 1];
    private static final float INVISIBLE = 1.0e-4f;

    private static final class FadeState {
        float hover;
        float focus;
}

    public LiquidGlassUniforms() {
        this.samplerInfo = WLRenderSystem.uniformBuffer("walksylib SamplerInfo", 16);
        final Std140Size calc = new Std140Size();
        calc.putFloat();
        calc.align(16);
        calc.putVec4();
        calc.putFloat();
        calc.align(16);
        calc.putVec3();
        calc.align(16);
        calc.putVec4();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        final int customUniformsSize = calc.get();
        for (int i = 0; i < MAX_LAYERS; i++) {
            this.customUniforms[i] = WLRenderSystem.uniformBuffer("walksylib CustomUniforms slot " + i, customUniformsSize);
        }
        final int widgetInfoSize = 16 + MAX_LAYERS * 16 + MAX_WIDGETS * (16 * 16);
        for (int i = 0; i < MAX_LAYERS; i++) {
            this.widgetInfoBySlot[i] = WLRenderSystem.uniformBuffer("walksylib WidgetInfo slot " + i, widgetInfoSize);
        }
        final Std140Size bcalc = new Std140Size();
        bcalc.putFloat();
        bcalc.putFloat();
        bcalc.putVec2();
        final int bgConfigSize = bcalc.get();
        this.bgConfig = WLRenderSystem.uniformBuffer("walksylib BgConfig", bgConfigSize);
    }

    private static float red(final int argb) {
        return ((argb >> 16) & 0xFF) / 255f;
    }

    private static float green(final int argb) {
        return ((argb >> 8) & 0xFF) / 255f;
    }

    private static float blue(final int argb) {
        return (argb & 0xFF) / 255f;
    }

    public void beginFrame(final double dtSeconds) {
        if (this.uploaded) {
            this.fades.keySet().retainAll(this.fadesSeen);
            this.fadesSeen.clear();
        }
        this.beginMainPass();
        this.topWidgets.clear();
        this.topWidgetsPerLayer.clear();
        this.dtSeconds = Math.max(0.0, dtSeconds);
    }

    public void beginMainPass() {
        this.widgets.clear();
        this.widgetsPerLayer.clear();
        this.blurPassRequested = false;
        this.vanillaBlurWanted = false;
        for (final List<Integer> radii : this.blurRadiiBySlot) {
            radii.clear();
        }
        this.slotCount = 0;
        this.mainSlotCount = 0;
        this.uploaded = false;
    }

    public void upload() {
        if (this.uploaded) {
            return;
        }
        this.uploaded = true;
        this.uploadWidgetInfo();
        this.uploadSharedUniforms();
    }

    public void uploadSharedUniforms() {
        final AppearanceConfig appearance = WalksyLib.getInstance().getAppearancePage();
        final LiquidGlassAnim anim = WLRenderSystem.glassAnim();
        final Minecraft mc = Minecraft.getInstance();
        final int outW = WLRenderSystem.targetWidth();
        final int outH = WLRenderSystem.targetHeight();
        try (final WLMapping map = this.samplerInfo.map()) {
            final Std140Writer b = map.writer();
            b.putVec2((float) outW, (float) outH);
            b.putVec2((float) outW, (float) outH);
        }
        final double cursorX = mc.mouseHandler.xpos();
        final double cursorY = mc.mouseHandler.ypos();
        final float scaleX = DevicePixels.perLogicalX();
        final float scaleY = DevicePixels.perLogicalY();
        final int fbH = outH;
        final float time = (System.nanoTime() - START_NANOS) / NANOS_PER_SECOND;
        final float popoverBoundary = (float) WLRenderSystem.contentStratum().popoverContentBoundary();
        this.scheduleOverlays(popoverBoundary);
        for (int slot = 0; slot < this.slotCount; slot++) {
            try (final WLMapping map = this.customUniforms[slot].map()) {
                final Std140Writer b = map.writer();
                b.putFloat(time);
                b.align(16);
                final float x = (float) (cursorX * scaleX);
                final float y = fbH - (float) (cursorY * scaleY);
                b.putVec4(x, y, 0f, 0f);
                b.putFloat(0.0f);
                b.align(16);
                final var dir2 = LiquidGlassConstants.RIM_LIGHT.direction();
                b.putVec3(dir2.x, dir2.y, 0.0f);
                b.align(16);
                final int rc = LiquidGlassConstants.RIM_LIGHT.color();
                b.putVec4(red(rc), green(rc), blue(rc), LiquidGlassConstants.RIM_LIGHT.intensity());
                b.putFloat(LiquidGlassConstants.PIXEL_EPSILON);
                b.putFloat(anim.debugStep());
                b.putFloat(LiquidGlassConstants.PIXELATED_GRID ? 1.0f : 0.0f);
                b.putFloat(anim.pixelatedGridSize());
                b.putFloat(anim.hoverScalePx());
                b.putFloat(anim.focusScalePx());
                b.putFloat(anim.focusBorderWidthPx());
                b.putFloat(appearance.focusRing() ? anim.focusBorderIntensity() : 0f);
                b.putFloat(anim.focusBorderSpeed());
                b.putFloat(popoverBoundary);
                b.putFloat((float) slot);
                b.putFloat(this.textAtStart[slot] ? 1f : 0f);
                b.putFloat(this.contentAtStart[slot] ? 1f : 0f);
                b.putFloat(this.popoverAtStart[slot] ? 1f : 0f);
                b.putFloat(this.textAtEnd[slot] ? 1f : 0f);
                b.putFloat(0f);
                b.putFloat(0f);
                final boolean top = slot >= this.mainSlotCount;
                b.putFloat(top || (this.textCompositePass >= 0 && slot > this.textCompositePass) ? 1f : 0f);
                b.putFloat(top || (this.contentCompositePass >= 0 && slot > this.contentCompositePass) ? 1f : 0f);
                b.putFloat(top || (this.popoverCompositePass >= 0 && slot > this.popoverCompositePass) ? 1f : 0f);
            }
        }
        try (final WLMapping map = this.bgConfig.map()) {
            final Std140Writer b = map.writer();
            b.putFloat(anim.shadowExpand());
            b.putFloat(anim.shadowFactor());
            b.putVec2(anim.shadowOffsetX() * scaleX, anim.shadowOffsetY() * scaleY);
        }
    }

    private void scheduleOverlays(final float popoverBoundary) {
        Arrays.fill(this.textAtStart, false);
        Arrays.fill(this.textAtEnd, false);
        Arrays.fill(this.contentAtStart, false);
        Arrays.fill(this.contentAtEnd, false);
        Arrays.fill(this.popoverAtStart, false);
        Arrays.fill(this.popoverAtEnd, false);
        this.textCompositePass = this.scheduleOverlay(EARLY_TEXT_LAYER, this.textAtStart, this.textAtEnd);
        this.contentCompositePass = this.scheduleOverlay(CONTENT_LAYER, this.contentAtStart, this.contentAtEnd);
        this.popoverCompositePass = this.scheduleOverlay(popoverBoundary, this.popoverAtStart, this.popoverAtEnd);
    }

    private int scheduleOverlay(final float boundary, final boolean[] atStart, final boolean[] atEnd) {
        if (this.mainSlotCount <= 0) {
            return -1;
        }
        if (this.layerValuesBySlot[0] > boundary + 0.5f) {
            atStart[0] = true;
            return 0;
        }
        int last = 0;
        for (int i = 0; i < this.mainSlotCount; i++) {
            if (!(this.layerValuesBySlot[i] > boundary + 0.5f)) {
                last = i;
            }
        }
        atEnd[last] = true;
        return last;
    }

    public void requestScreenBlur(final WLGraphics2D graphics) {
        this.vanillaBlurWanted = true;
        this.requestGlassPass(graphics);
    }

    public boolean wantsVanillaBlur() {
        return this.vanillaBlurWanted;
    }

    public void requestGlassPass(final WLGraphics2D graphics) {
        if (!this.blurPassRequested) {
            graphics.blurBeforeThisStratum();
            this.blurPassRequested = true;
        }
    }

    public void addWidget(final LiquidGlassGuiElementRenderState element) {
        add(element, this.widgets, this.widgetsPerLayer);
    }

    public void addTopWidget(final LiquidGlassGuiElementRenderState element) {
        add(element, this.topWidgets, this.topWidgetsPerLayer);
    }

    private static void add(final LiquidGlassGuiElementRenderState element, final List<LiquidGlassGuiElementRenderState> into, final HashMap<Integer, Integer> perLayer) {
        final int layer = element.style().getLayer();
        final int onLayer = perLayer.getOrDefault(layer, 0);
        if (onLayer >= MAX_WIDGETS) {
            warnWidgetOverflow(element, into);
            return;
        }
        perLayer.put(layer, onLayer + 1);
        into.add(element);
    }

    private static void warnWidgetOverflow(final LiquidGlassGuiElementRenderState dropped, final List<LiquidGlassGuiElementRenderState> accepted) {
        if (!REPORTED_WIDGET_OVERFLOW.compareAndSet(false, true)) {
            return;
        }
        final TreeMap<Integer, Integer> byLayer = new TreeMap<>();
        for (final LiquidGlassGuiElementRenderState w : accepted) {
            byLayer.merge(w.style().getLayer(), 1, Integer::sum);
        }
        WalksyLib.getLogger().err("glass widget budget EXHAUSTED on LAYER " + dropped.style().getLayer() + ": more than " + MAX_WIDGETS + " glass shapes on one layer. That shape, and everything after it on that layer," + " is invisible this frame. Shapes accepted, by layer: " + byLayer + ". The budget is per layer (one WidgetInfo UBO per layer slot), so the fix is to" + " emit fewer shapes on that ONE layer, or to spread them over two - not to grow" + " the UBO, which is at 15760 of the 16384 bytes GL guarantees.");
    }

    private static long rectKey(final float x1, final float y1, final float x2, final float y2) {
        final long a = ((long) Math.round(x1) & 0xFFFFFFFFL) | (((long) Math.round(y1) & 0xFFFFFFFFL) << 32);
        final long b = ((long) Math.round(x2) & 0xFFFFFFFFL) | (((long) Math.round(y2) & 0xFFFFFFFFL) << 32);
        long h = 1469598103934665603L;
        h ^= a;
        h *= 1099511628211L;
        h ^= b;
        h *= 1099511628211L;
        return h;
    }

    private float smoothToward(final float current, final float target, final double dt, final float tau) {
        if (tau <= 1e-5f) {
            return target;
        }
        final float a = (float) (1.0 - Math.exp(-Math.max(0.0, dt) / Math.max(1e-4, tau)));
        final float v = current + (target - current) * a;
        if (Math.abs(v - target) < 1e-4f) {
            return target;
        }
        return v;
    }

    private static void warnLayerOverflow(final int droppedLayer, final float[] keptLayers) {
        if (!REPORTED_LAYER_OVERFLOW.add(droppedLayer)) {
            return;
        }
        final StringBuilder kept = new StringBuilder();
        for (int i = 0; i < MAX_LAYERS; i++) {
            kept.append(i == 0 ? "" : ", ").append((int) keptLayers[i]);
        }
        WalksyLib.getLogger().err("glass layer " + droppedLayer + " and everything above it" + " was DROPPED: more than " + MAX_LAYERS + " distinct glass layers on screen at" + " once. Rendered: [" + kept + "]. Those widgets will be invisible. Either give" + " something a layer it can share, or raise MAX_LAYERS here AND LayerRanges in" + " liquid_glass_gui.fsh (each layer costs one full render pass).");
    }

    public void uploadWidgetInfo() {
        final AppearanceConfig appearance = WalksyLib.getInstance().getAppearancePage();
        final int outW = WLRenderSystem.targetWidth();
        final int fbH = WLRenderSystem.targetHeight();
        final float scaleX = DevicePixels.perLogicalX();
        final float scaleY = DevicePixels.perLogicalY();
        this.widgets.sort((a, b) -> Integer.compare(a.style().getLayer(), b.style().getLayer()));
        this.topWidgets.sort((a, b) -> Integer.compare(a.style().getLayer(), b.style().getLayer()));
        final List<LiquidGlassGuiElementRenderState> renderWidgets = this.ordered;
        renderWidgets.clear();
        renderWidgets.addAll(this.widgets);
        renderWidgets.addAll(this.topWidgets);
        final int firstTop = this.widgets.size();
        final int[] layerStarts = this.layerStarts;
        final int[] layerCounts = this.layerCounts;
        final float[] layerValues = this.layerValues;
        Arrays.fill(layerStarts, -1);
        Arrays.fill(layerCounts, 0);
        Arrays.fill(layerValues, 0f);
        int slot = -1;
        int prevLayer = Integer.MIN_VALUE;
        int droppedLayer = Integer.MIN_VALUE;
        int mainSlots = -1;
        for (int i = 0; i < renderWidgets.size(); i++) {
            final LiquidGlassGuiElementRenderState w = renderWidgets.get(i);
            final int layer = w.style().getLayer();
            if (i == firstTop) {
                mainSlots = slot + 1;
            }
            if (layer != prevLayer || i == firstTop) {
                if (slot + 1 >= MAX_LAYERS) {
                    droppedLayer = layer;
                    break;
                }
                slot++;
                layerStarts[slot] = i;
                layerValues[slot] = layer;
                prevLayer = layer;
            }
            layerCounts[slot]++;
        }
        if (droppedLayer != Integer.MIN_VALUE) {
            warnLayerOverflow(droppedLayer, layerValues);
        }
        this.slotCount = slot + 1;
        this.mainSlotCount = mainSlots < 0 ? this.slotCount : mainSlots;
        System.arraycopy(layerValues, 0, this.layerValuesBySlot, 0, MAX_LAYERS);
        for (int s = 0; s < this.slotCount; s++) {
            int distinctCount = 0;
            final int from = layerStarts[s] < 0 ? 0 : layerStarts[s];
            for (int i = from; i < from + layerCounts[s] && i < renderWidgets.size(); i++) {
                final var bs = renderWidgets.get(i).style();
                final int radius = appearance.blurRadius(Math.max(0, bs.getBlurRadius()), bs.getLayer());
                if (radius > 0) {
                    distinctCount = insertRadius(this.distinctRadii, distinctCount, radius);
                }
            }
            final List<Integer> slotRadii = this.blurRadiiForSlot(s);
            slotRadii.clear();
            for (int i = 0; i < distinctCount; i++) {
                slotRadii.add(this.distinctRadii[i]);
            }
        }
        for (int slotIdx = 0; slotIdx < this.slotCount; slotIdx++) {
            final int from = layerStarts[slotIdx] < 0 ? 0 : layerStarts[slotIdx];
            final int count = Math.min(layerCounts[slotIdx], Math.max(0, renderWidgets.size() - from));
            final List<LiquidGlassGuiElementRenderState> slotWidgets = renderWidgets.subList(from, from + count);
            final int slotSize = slotWidgets.size();
            this.accumulateSlotBounds(slotIdx, slotWidgets, scaleX, scaleY, outW, fbH);
            try (final WLMapping map = this.widgetInfoBySlot[slotIdx].map()) {
                final ByteBuffer raw = map.data();
                final Std140Writer b = Std140Writer.into(raw);
                b.putFloat((float) slotWidgets.size());
                b.align(16);
                for (int i = 0; i < MAX_LAYERS; i++) {
                    final boolean mine = i == slotIdx;
                    b.putVec4(0f, mine ? (float) count : 0f, layerValues[i], mine ? slotFlags(slotWidgets) : 0f);
                }
                for (int i = 0; i < slotSize; i++) {
                    final var w = slotWidgets.get(i);
                    final Matrix3x2f p = w.pose();
                    final float sx1 = p.m00() * w.x1() + p.m10() * w.y1() + p.m20();
                    final float sy1 = p.m01() * w.x1() + p.m11() * w.y1() + p.m21();
                    final float sx2 = p.m00() * w.x2() + p.m10() * w.y2() + p.m20();
                    final float sy2 = p.m01() * w.x2() + p.m11() * w.y2() + p.m21();
                    final float px = sx1 * scaleX;
                    final float pyTop = sy1 * scaleY;
                    final float pW = (sx2 - sx1) * scaleX;
                    final float pH = (sy2 - sy1) * scaleY;
                    final float cx = px + 0.5f * pW;
                    final float cyTop = pyTop + 0.5f * pH;
                    final float cyFB = (float) fbH - cyTop;
                    final float rectX = cx - 0.5f * pW;
                    final float rectY = cyFB - 0.5f * pH;
                    b.putVec4(rectX, rectY, pW, pH);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final var w = slotWidgets.get(i);
                    final float rad = w.cornerRadius() * scaleY;
                    b.putVec4(rad, rad, rad, rad);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final var style = slotWidgets.get(i).style();
                    final int c = style.getTintColor();
                    b.putVec4(red(c), green(c), blue(c), style.getTintAlpha());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    b.putVec4(s.getRefThickness() * scaleY, s.getRefFactor(), s.getRefDispersion(), s.getRefFresnelRange() * scaleY);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    b.putVec4(s.getRefFresnelHardness(), s.getRefFresnelFactor(), s.getGlareRange() * scaleY, s.getGlareHardness());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final int ec = s.getEdgeDarkenColor();
                    b.putVec4(s.getEdgeDarken(), red(ec), green(ec), blue(ec));
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    b.putVec4(s.getSmoothing(), s.getRefStrength(), s.getChroma(), s.getOpacity());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final var w = slotWidgets.get(i);
                    final WLRect sc = w.scissorArea();
                    if (sc != null) {
                        final float sL = sc.left() * scaleX;
                        final float sR = sc.right() * scaleX;
                        final float sT = sc.top() * scaleY;
                        final float sB = sc.bottom() * scaleY;
                        b.putVec4(sL, fbH - sB, sR, fbH - sT);
                    } else {
                        b.putVec4(0f, 0f, (float) outW, (float) fbH);
                    }
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final float sx = s.getShadowOffsetX() * scaleX;
                    final float sy = s.getShadowOffsetY() * scaleY;
                    final float expand = s.getShadowExpand();
                    final float z = expand < 0f ? s.getShadowOffsetX() * scaleY : sx;
                    b.putVec4(expand < 0f ? expand : expand * scaleY, s.getShadowFactor(), z, sy);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final int col = s.getShadowColor();
                    b.putVec4(red(col), green(col), blue(col), s.getShadowColorAlpha());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final int radius = appearance.blurRadius(Math.max(0, s.getBlurRadius()), s.getLayer());
                    int idx = -1;
                    final int wSlot = slotIdx;
                    if (radius > 0 && wSlot >= 0 && wSlot < this.blurRadiiBySlot.size()) {
                        idx = this.blurRadiiBySlot.get(wSlot).indexOf(radius);
                    }
                    final var w = slotWidgets.get(i);
                    final long key = s.hasFadeKey() ? s.getFadeKey() : rectKey(w.x1(), w.y1(), w.x2(), w.y2());
                    this.fadesSeen.add(key);
                    final FadeState fs = this.fades.computeIfAbsent(key, k -> new FadeState());
                    fs.hover = this.smoothToward(fs.hover, Math.max(0f, Math.min(1f, w.hover())), this.dtSeconds, 0.12f);
                    fs.focus = this.smoothToward(fs.focus, Math.max(0f, Math.min(1f, w.focus())), this.dtSeconds, 0.18f);
                    final float blurCode = idx >= 0 && s.hasBlurFalloff() ? idx + BLUR_FALLOFF_BIAS : idx;
                    b.putVec4(blurCode, fs.hover, fs.focus, s.getRefractionShift() * scaleY);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final int rl = s.getRimLightColor();
                    b.putVec4(red(rl), green(rl), blue(rl), s.getRimLightIntensity());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final float dirFactor = s.hasDirectionalGlare() ? s.getGlareFactor() : 0f;
                    b.putVec4(s.getGlareAngleRad(), s.getGlareConvergence(), s.getGlareOppositeFactor(), dirFactor);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    b.putVec4(s.getBevelHeight() * scaleY, s.getRefractionReach(), s.getBrightEdge(), LiquidGlassConstants.BRIGHT_EDGE_WIDTH_PX * scaleY);
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final GlassStyle s = slotWidgets.get(i).style();
                    final boolean bakes = s.getBakesText() && slotIdx < this.mainSlotCount;
                    final float foldCode = s.getLensFold() + (s.isSoftLens() ? SOFT_LENS_FLAG : 0f);
                    final float fold = bakes ? foldCode : -(foldCode + 1f);
                    b.putVec4(s.getInnerShadow(), fold, s.getToneFloor(), s.getToneCeiling());
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
                for (int i = 0; i < slotSize; i++) {
                    final LiquidGlassGuiElementRenderState w = slotWidgets.get(i);
                    final GlassStyle s = w.style();
                    b.putVec4(s.getTextScale(), s.getTextRefract(), stretchDeviceX(w, scaleX), stretchDeviceY(w, scaleY));
                }
                skipVec4s(raw, MAX_WIDGETS - slotSize);
            }
        }
    }

    private static float stretchDeviceX(final LiquidGlassGuiElementRenderState w, final float scaleX) {
        final Matrix3x2f p = w.pose();
        return (p.m00() * w.stretchX() + p.m10() * w.stretchY()) * scaleX;
    }

    private static float stretchDeviceY(final LiquidGlassGuiElementRenderState w, final float scaleY) {
        final Matrix3x2f p = w.pose();
        return -(p.m01() * w.stretchX() + p.m11() * w.stretchY()) * scaleY;
    }

    private static void skipVec4s(final ByteBuffer buf, final int count) {
        if (count > 0) {
            buf.position(buf.position() + count * 16);
        }
    }

    private static float shadowReach(final float expand, final float peak) {
        if (expand <= 0f || peak <= INVISIBLE) {
            return 0f;
        }
        return expand * (float) Math.log(peak / INVISIBLE);
    }

    private static float maxBackdropReachPx(final GlassStyle s, final float scaleY, final float widgetW, final float widgetH, final int fbH, final boolean ownNormals) {
        final float n = s.getRefFactor();
        final float grazing = (float) Math.sqrt(Math.max(n * n - 1f, 0f));
        final float thickPx = Math.max(s.getRefThickness() * scaleY, 1e-4f);
        final float bevelPx = s.getBevelHeight() * scaleY;
        final float reachFrac = s.getRefractionReach();
        final float base;
        final boolean inward;
        if (bevelPx > 1e-4f) {
            base = grazing * bevelPx * Math.max(0f, Math.min(4f, s.getRefStrength()));
            inward = true;
        } else if (Math.abs(reachFrac) > 1e-4f) {
            final float peakCap = 0.75f + 2.25f * Math.max(0f, Math.min(1f, s.getLensFold()));
            base = Math.min(Math.abs(reachFrac) * Math.min(widgetW, widgetH), peakCap * thickPx);
            inward = false;
        } else {
            base = grazing * 0.08f * fbH;
            inward = true;
        }
        final float spread = Math.max(1f + 0.015f * Math.abs(s.getRefDispersion()), 1.35f);
        final float ray = base * spread;
        final float escapes = (inward && ownNormals) ? Math.max(0f, ray - 0.5f * Math.min(widgetW, widgetH)) : ray;
        final float snap = LiquidGlassConstants.PIXELATED_GRID ? Math.abs(WLRenderSystem.glassAnim().pixelatedGridSize()) : 0f;
        final float loose = Math.abs(s.getRefractionShift() * scaleY) * spread + 1.5f * spread + snap;
        return (escapes + loose) * 1.25f + 16f;
    }

    private static float slotFlags(final List<LiquidGlassGuiElementRenderState> slotWidgets) {
        int flags = 0;
        for (int i = 0; i < slotWidgets.size(); i++) {
            final GlassStyle s = slotWidgets.get(i).style();
            if (s.getSmoothing() < 0f) {
                flags |= 1;
            }
            final boolean outlineMode = s.getShadowExpand() < 0f;
            if (s.getShadowColorAlpha() > 0f && s.getOpacity() > 0f && (outlineMode || s.getShadowFactor() != 0f)) {
                flags |= 2;
            }
            if (flags == 3) {
                break;
            }
        }
        return flags;
    }

    private void accumulateSlotBounds(final int slotIdx, final List<LiquidGlassGuiElementRenderState> slotWidgets, final float scaleX, final float scaleY, final int fbW, final int fbH) {
        final AppearanceConfig appearance = WalksyLib.getInstance().getAppearancePage();
        this.slotBoundsEmpty[slotIdx] = true;
        this.slotBlurRegionEmpty[slotIdx] = true;
        if (slotWidgets.isEmpty()) {
            return;
        }
        float qx0 = Float.MAX_VALUE;
        float qy0 = Float.MAX_VALUE;
        float qx1 = -Float.MAX_VALUE;
        float qy1 = -Float.MAX_VALUE;
        final float inflate = Math.max(0f, WLRenderSystem.glassAnim().hoverScalePx()) + Math.max(0f, WLRenderSystem.glassAnim().focusScalePx());
        float kSum = 0f;
        int positives = 0;
        boolean repulsion = false;
        float maxSmoothing = 0f;
        for (int i = 0; i < slotWidgets.size(); i++) {
            final float k = slotWidgets.get(i).style().getSmoothing();
            kSum += Math.abs(k);
            if (k < 0f) {
                repulsion = true;
            } else {
                positives++;
                maxSmoothing = Math.max(maxSmoothing, k);
            }
        }
        final boolean ownNormals = !repulsion && (positives <= 1 || maxSmoothing == 0f);
        final float smoothSlack = inflate + kSum * 0.25f * fbH + 2f;
        float bx0 = Float.MAX_VALUE;
        float by0 = Float.MAX_VALUE;
        float bx1 = -Float.MAX_VALUE;
        float by1 = -Float.MAX_VALUE;
        if (this.slotWidgetBounds[slotIdx] == null) {
            this.slotWidgetBounds[slotIdx] = new float[MAX_WIDGETS * 4];
        }
        this.slotWidgetBoundsCount[slotIdx] = 0;
        for (int i = 0; i < slotWidgets.size(); i++) {
            final LiquidGlassGuiElementRenderState w = slotWidgets.get(i);
            final GlassStyle s = w.style();
            float wx0 = Float.MAX_VALUE;
            float wy0 = Float.MAX_VALUE;
            float wx1 = -Float.MAX_VALUE;
            float wy1 = -Float.MAX_VALUE;
            if (s.getOpacity() <= 0f) {
                continue;
            }
            final Matrix3x2f p = w.pose();
            final float sx1 = p.m00() * w.x1() + p.m10() * w.y1() + p.m20();
            final float sy1 = p.m01() * w.x1() + p.m11() * w.y1() + p.m21();
            final float sx2 = p.m00() * w.x2() + p.m10() * w.y2() + p.m20();
            final float sy2 = p.m01() * w.x2() + p.m11() * w.y2() + p.m21();
            final float stretchReach = (float) Math.hypot(stretchDeviceX(w, scaleX), stretchDeviceY(w, scaleY));
            final float rx0 = Math.min(sx1, sx2) * scaleX - stretchReach;
            final float rx1 = Math.max(sx1, sx2) * scaleX + stretchReach;
            final float rTop = Math.min(sy1, sy2) * scaleY - stretchReach;
            final float rBot = Math.max(sy1, sy2) * scaleY + stretchReach;
            final float ry0 = fbH - rBot;
            final float ry1 = fbH - rTop;
            float scX0 = 0f;
            float scY0 = 0f;
            float scX1 = fbW;
            float scY1 = fbH;
            final WLRect sc = w.scissorArea();
            if (sc != null) {
                scX0 = sc.left() * scaleX;
                scX1 = sc.right() * scaleX;
                scY0 = fbH - sc.bottom() * scaleY;
                scY1 = fbH - sc.top() * scaleY;
            }
            final long fadeKey = s.hasFadeKey() ? s.getFadeKey() : rectKey(w.x1(), w.y1(), w.x2(), w.y2());
            final FadeState fade = this.fades.get(fadeKey);
            final boolean ringed = w.focus() > 0f || (fade != null && fade.focus > 0f);
            final float bodySlack = (ringed ? 0.01f : 0.001f) * fbH + smoothSlack;
            final float bodyX0 = Math.max(rx0 - bodySlack, scX0);
            final float bodyY0 = Math.max(ry0 - bodySlack, scY0);
            final float bodyX1 = Math.min(rx1 + bodySlack, scX1);
            final float bodyY1 = Math.min(ry1 + bodySlack, scY1);
            if (bodyX1 > bodyX0 && bodyY1 > bodyY0) {
                bx0 = Math.min(bx0, bodyX0);
                by0 = Math.min(by0, bodyY0);
                bx1 = Math.max(bx1, bodyX1);
                by1 = Math.max(by1, bodyY1);
                wx0 = Math.min(wx0, bodyX0);
                wy0 = Math.min(wy0, bodyY0);
                wx1 = Math.max(wx1, bodyX1);
                wy1 = Math.max(wy1, bodyY1);
                this.slotBoundsEmpty[slotIdx] = false;
                if (appearance.blurRadius(Math.max(0, s.getBlurRadius()), s.getLayer()) > 0) {
                    final float reach = maxBackdropReachPx(s, scaleY, rx1 - rx0, ry1 - ry0, fbH, ownNormals);
                    qx0 = Math.min(qx0, bodyX0 - reach);
                    qy0 = Math.min(qy0, bodyY0 - reach);
                    qx1 = Math.max(qx1, bodyX1 + reach);
                    qy1 = Math.max(qy1, bodyY1 + reach);
                    this.slotBlurRegionEmpty[slotIdx] = false;
                }
            }
            final float rawExpand = s.getShadowExpand();
            final boolean outlineMode = rawExpand < 0f;
            final float expandPx = outlineMode ? rawExpand : rawExpand * scaleY;
            final float offX = outlineMode ? 0f : s.getShadowOffsetX() * scaleX;
            final float offY = s.getShadowOffsetY() * scaleY;
            final float softExpand = Math.max(outlineMode ? s.getShadowOffsetX() * scaleY : expandPx, 0f);
            final float factor = Math.abs(s.getShadowFactor());
            final float peak = Math.max(0f, s.getShadowColorAlpha()) * 0.6f * factor * Math.max(0f, Math.min(1f, s.getOpacity()));
            float reach = peak > INVISIBLE ? Math.max(shadowReach(softExpand, peak), 1f) : 0f;
            if (outlineMode) {
                reach = Math.max(reach, -expandPx + 1.5f);
            }
            if (reach > 0f || outlineMode) {
                final float grow = reach + inflate + Math.abs(offX) + Math.abs(offY) + 1f;
                final boolean bodyClipped = rx0 < scX0 - 0.5f || ry0 < scY0 - 0.5f || rx1 > scX1 + 0.5f || ry1 > scY1 + 0.5f;
                final float bleed = bodyClipped ? 0f : softExpand * 3f + Math.max(Math.abs(offX), Math.abs(offY));
                final float shX0 = Math.max(rx0 - grow, scX0 - bleed);
                final float shY0 = Math.max(ry0 - grow, scY0 - bleed);
                final float shX1 = Math.min(rx1 + grow, scX1 + bleed);
                final float shY1 = Math.min(ry1 + grow, scY1 + bleed);
                if (shX1 > shX0 && shY1 > shY0) {
                    bx0 = Math.min(bx0, shX0);
                    by0 = Math.min(by0, shY0);
                    bx1 = Math.max(bx1, shX1);
                    by1 = Math.max(by1, shY1);
                    wx0 = Math.min(wx0, shX0);
                    wy0 = Math.min(wy0, shY0);
                    wx1 = Math.max(wx1, shX1);
                    wy1 = Math.max(wy1, shY1);
                    this.slotBoundsEmpty[slotIdx] = false;
                }
            }
            if (wx1 > wx0 && wy1 > wy0 && this.slotWidgetBoundsCount[slotIdx] < MAX_WIDGETS) {
                final int wb = this.slotWidgetBoundsCount[slotIdx]++ * 4;
                this.slotWidgetBounds[slotIdx][wb] = wx0;
                this.slotWidgetBounds[slotIdx][wb + 1] = wy0;
                this.slotWidgetBounds[slotIdx][wb + 2] = wx1;
                this.slotWidgetBounds[slotIdx][wb + 3] = wy1;
            }
        }
        final int base = slotIdx * 4;
        this.slotBounds[base] = bx0;
        this.slotBounds[base + 1] = by0;
        this.slotBounds[base + 2] = bx1;
        this.slotBounds[base + 3] = by1;
        this.slotBlurRegion[base] = qx0;
        this.slotBlurRegion[base + 1] = qy0;
        this.slotBlurRegion[base + 2] = qx1;
        this.slotBlurRegion[base + 3] = qy1;
    }

    public int[] getBlurRegion(final int slot, final int fbW, final int fbH) {
        if (slot < 0 || slot >= this.slotCount || this.slotBlurRegionEmpty[slot]) {
            return new int[] {0, 0, 0, 0};
        }
        final int base = slot * 4;
        final int ix0 = Math.max(0, (int) Math.floor(this.slotBlurRegion[base]));
        final int iy0 = Math.max(0, (int) Math.floor(this.slotBlurRegion[base + 1]));
        final int ix1 = Math.min(fbW, (int) Math.ceil(this.slotBlurRegion[base + 2]));
        final int iy1 = Math.min(fbH, (int) Math.ceil(this.slotBlurRegion[base + 3]));
        if (ix1 <= ix0 || iy1 <= iy0) {
            return new int[] {0, 0, 0, 0};
        }
        if (ix0 == 0 && iy0 == 0 && ix1 == fbW && iy1 == fbH) {
            return null;
        }
        return new int[] {ix0, iy0, ix1 - ix0, iy1 - iy0};
    }

    public void setStratumBounds(final float[] text, final float[] content, final float[] popover) {
        this.textStratumBounds = text;
        this.contentStratumBounds = content;
        this.popoverStratumBounds = popover;
    }

    public int[] getPassScissor(final int slot, final int fbW, final int fbH) {
        if (slot < 0 || slot >= this.slotCount) {
            return new int[] {0, 0, 0, 0};
        }
        final int base = slot * 4;
        float x0 = this.slotBounds[base];
        float y0 = this.slotBounds[base + 1];
        float x1 = this.slotBounds[base + 2];
        float y1 = this.slotBounds[base + 3];
        boolean any = !this.slotBoundsEmpty[slot];
        for (int which = 0; which < 3; which++) {
            final float[] stratum = switch (which) {
                case 0 -> (this.textAtStart[slot] || this.textAtEnd[slot]) ? this.textStratumBounds : null;
                case 1 -> this.contentAtStart[slot] ? this.contentStratumBounds : null;
                default -> this.popoverAtStart[slot] ? this.popoverStratumBounds : null;
            };
            if (stratum == null) {
                continue;
            }
            if (!any) {
                x0 = stratum[0];
                y0 = stratum[1];
                x1 = stratum[2];
                y1 = stratum[3];
                any = true;
            } else {
                x0 = Math.min(x0, stratum[0]);
                y0 = Math.min(y0, stratum[1]);
                x1 = Math.max(x1, stratum[2]);
                y1 = Math.max(y1, stratum[3]);
            }
        }
        if (!any) {
            return new int[] {0, 0, 0, 0};
        }
        final int ix0 = Math.max(0, (int) Math.floor(x0));
        final int iy0 = Math.max(0, (int) Math.floor(y0));
        final int ix1 = Math.min(fbW, (int) Math.ceil(x1));
        final int iy1 = Math.min(fbH, (int) Math.ceil(y1));
        if (ix1 <= ix0 || iy1 <= iy0) {
            return new int[] {0, 0, 0, 0};
        }
        if (ix0 == 0 && iy0 == 0 && ix1 == fbW && iy1 == fbH) {
            return null;
        }
        return new int[] {ix0, iy0, ix1 - ix0, iy1 - iy0};
    }

    public int[][] getPassScissors(final int slot, final int fbW, final int fbH) {
        final int[] union = this.getPassScissor(slot, fbW, fbH);
        if (union == null) {
            return null;
        }
        if (union[2] <= 0 || union[3] <= 0) {
            return new int[0][];
        }
        if (slot < 0 || slot >= this.slotCount || this.slotWidgetBounds[slot] == null) {
            return new int[][] {union};
        }
        final List<int[]> boxes = new ArrayList<>();
        final int count = this.slotWidgetBoundsCount[slot];
        for (int i = 0; i < count; i++) {
            final int b = i * 4;
            final int[] box = clampBox(this.slotWidgetBounds[slot][b], this.slotWidgetBounds[slot][b + 1], this.slotWidgetBounds[slot][b + 2], this.slotWidgetBounds[slot][b + 3], fbW, fbH);
            if (box != null) {
                boxes.add(box);
            }
        }
        for (int which = 0; which < 3; which++) {
            final float[] stratum = switch (which) {
                case 0 -> (this.textAtStart[slot] || this.textAtEnd[slot]) ? this.textStratumBounds : null;
                case 1 -> this.contentAtStart[slot] ? this.contentStratumBounds : null;
                default -> this.popoverAtStart[slot] ? this.popoverStratumBounds : null;
            };
            if (stratum != null) {
                final int[] box = clampBox(stratum[0], stratum[1], stratum[2], stratum[3], fbW, fbH);
                if (box != null) {
                    boxes.add(box);
                }
            }
        }
        if (boxes.isEmpty()) {
            return new int[][] {union};
        }
        boolean merged = true;
        while (merged && boxes.size() > 1) {
            merged = false;
            outer:
            for (int i = 0; i < boxes.size(); i++) {
                for (int j = i + 1; j < boxes.size(); j++) {
                    final int[] a = boxes.get(i);
                    final int[] b = boxes.get(j);
                    final int x0 = Math.min(a[0], b[0]);
                    final int y0 = Math.min(a[1], b[1]);
                    final int x1 = Math.max(a[0] + a[2], b[0] + b[2]);
                    final int y1 = Math.max(a[1] + a[3], b[1] + b[3]);
                    if ((long) (x1 - x0) * (y1 - y0) <= (long) a[2] * a[3] + (long) b[2] * b[3]) {
                        boxes.set(i, new int[] {x0, y0, x1 - x0, y1 - y0});
                        boxes.remove(j);
                        merged = true;
                        break outer;
                    }
                }
            }
        }
        long sum = 0L;
        for (final int[] box : boxes) {
            sum += (long) box[2] * box[3];
        }
        if (sum >= (long) union[2] * union[3]) {
            return new int[][] {union};
        }
        return boxes.toArray(new int[0][]);
    }

    private static int[] clampBox(final float x0, final float y0, final float x1, final float y1, final int fbW, final int fbH) {
        final int ix0 = Math.max(0, (int) Math.floor(x0));
        final int iy0 = Math.max(0, (int) Math.floor(y0));
        final int ix1 = Math.min(fbW, (int) Math.ceil(x1));
        final int iy1 = Math.min(fbH, (int) Math.ceil(y1));
        if (ix1 <= ix0 || iy1 <= iy0) {
            return null;
        }
        return new int[] {ix0, iy0, ix1 - ix0, iy1 - iy0};
    }

    public boolean contentCompositesAfter(final int slot) {
        return slot >= 0 && slot < this.slotCount && this.contentAtEnd[slot];
    }

    public boolean popoverCompositesAfter(final int slot) {
        return slot >= 0 && slot < this.slotCount && this.popoverAtEnd[slot];
    }

    public int[] getOverlayCompositeScissor(final boolean content, final int fbW, final int fbH) {
        final float[] bounds = content ? this.contentStratumBounds : this.popoverStratumBounds;
        if (bounds == null) {
            return new int[] {0, 0, 0, 0};
        }
        final int ix0 = Math.max(0, (int) Math.floor(bounds[0]));
        final int iy0 = Math.max(0, (int) Math.floor(bounds[1]));
        final int ix1 = Math.min(fbW, (int) Math.ceil(bounds[2]));
        final int iy1 = Math.min(fbH, (int) Math.ceil(bounds[3]));
        if (ix1 <= ix0 || iy1 <= iy0) {
            return new int[] {0, 0, 0, 0};
        }
        if (ix0 == 0 && iy0 == 0 && ix1 == fbW && iy1 == fbH) {
            return null;
        }
        return new int[] {ix0, iy0, ix1 - ix0, iy1 - iy0};
    }

    public boolean anySlotAbove(final float boundary) {
        for (int i = 0; i < this.mainSlotCount; i++) {
            if (this.layerValuesBySlot[i] > boundary + 0.5f) {
                return true;
            }
        }
        return false;
    }

    public boolean blurredTextIsRead() {
        return this.anySlotAbove(EARLY_TEXT_LAYER);
    }

    public boolean blurredContentIsRead() {
        return this.anySlotAbove(CONTENT_LAYER);
    }

    public boolean blurredPopoverIsRead() {
        return this.anySlotAbove((float) WLRenderSystem.contentStratum().popoverContentBoundary());
    }

    public int getCount() {
        return this.widgets.size();
    }

    public int getTopCount() {
        return this.topWidgets.size();
    }

    public int getSlotCount() {
        return this.slotCount;
    }

    public int getMainSlotCount() {
        return this.mainSlotCount;
    }

    public WLUniformBuffer getSamplerInfoBuffer() {
        return this.samplerInfo;
    }

    public WLUniformBuffer getCustomUniformsBuffer(final int slot) {
        return this.customUniforms[Math.max(0, Math.min(MAX_LAYERS - 1, slot))];
    }

    public WLUniformBuffer getWidgetInfoBuffer(final int slot) {
        return this.widgetInfoBySlot[slot];
    }

    public WLUniformBuffer getBgConfigBuffer() {
        return this.bgConfig;
    }

    public List<Integer> getBlurRadiiForSlot(final int slot) {
        if (slot < 0 || slot >= this.slotCount || slot >= this.blurRadiiBySlot.size()) {
            return List.of();
        }
        return this.blurRadiiBySlot.get(slot);
    }

    private List<Integer> blurRadiiForSlot(final int slot) {
        while (this.blurRadiiBySlot.size() <= slot) {
            this.blurRadiiBySlot.add(new ArrayList<>(MAX_BLUR_LEVELS - 1));
        }
        return this.blurRadiiBySlot.get(slot);
    }

    private static int insertRadius(final int[] into, final int count, final int radius) {
        int at = 0;
        while (at < count && into[at] < radius) {
            at++;
        }
        if (at < count && into[at] == radius) {
            return count;
        }
        if (at >= into.length) {
            return count;
        }
        for (int i = Math.min(count, into.length - 1); i > at; i--) {
            into[i] = into[i - 1];
        }
        into[at] = radius;
        return Math.min(count + 1, into.length);
    }
}
