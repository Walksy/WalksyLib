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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    private final ByteBuffer[] widgetInfoStaging = new ByteBuffer[MAX_LAYERS];
    private final ByteBuffer[] widgetInfoUploaded = new ByteBuffer[MAX_LAYERS];
    private final int widgetInfoSize;
    private final ByteBuffer[] customStaging = new ByteBuffer[MAX_LAYERS];
    private final ByteBuffer[] customUploaded = new ByteBuffer[MAX_LAYERS];
    private final int customUniformsSize;
    private ByteBuffer bgStaging;
    private ByteBuffer bgUploaded;
    private final int bgConfigSize;
    private int samplerInfoW = -1;
    private int samplerInfoH = -1;
    private int hudWidgetCount;
    private boolean layerElementSubmitted;
    private boolean splitThisFrame;
    private boolean splitFromHud;
    private boolean layerMode;
    private boolean layerUpdate;
    private boolean layerValid;
    private double layerClock;
    private final int[] layerArea = new int[4];
    private final int[] lastLayerArea = new int[4];
    private int lastLayerW = -1;
    private int lastLayerH = -1;
    private int refreshRate = 60;
    private long refreshCheckedAt;
    private final List<LiquidGlassGuiElementRenderState> reuseWidgets = new ArrayList<>();
    private final List<LiquidGlassGuiElementRenderState> reuseCandidate = new ArrayList<>();
    private int[] reuseStyleHashes = new int[8];
    private final List<Integer> reuseRadii = new ArrayList<>();
    private boolean layerBlurInline;
    private float reuseScaleX;
    private float reuseScaleY;
    private float reuseDebugStep;
    private long reuseFrame = -1L;
    private final boolean[] slotHasFocus = new boolean[MAX_LAYERS];
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
    private int earlyTextBoundary = EARLY_TEXT_LAYER;
    private boolean passHeld;
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
    private final float[] slotReadRegion = new float[MAX_LAYERS * 4];
    private final boolean[] slotReadRegionEmpty = new boolean[MAX_LAYERS];
    private final boolean[] slotNeedsScene = new boolean[MAX_LAYERS];
    private final float[] slotFastRect = new float[MAX_LAYERS * 8];
    private boolean textUsed = true;
    private boolean contentUsed = true;
    private boolean popoverUsed = true;
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
    private static final int LIT_FROM_BLUR_MAX_RADIUS = 16;
    private static final int CUSTOM_VOLATILE_BYTES = 32;
    private static final float FAST_INTERIOR_EDGE_REACH = 28f;
    private static final float LENS_ZONE = 3f;
    private static final float LENS_ZONE_FRAC = 0.9f;
    private static final float LENS_POWER = 4f;
    private static final float LENS_SOFT_POWER = 6f;
    private static final float LENS_FOLD_MIN = 1f;
    private static final float LENS_FOLD_MAX = 2.5f;

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
        calc.putFloat();
        calc.putFloat();
        calc.putFloat();
        calc.putVec4();
        calc.putVec4();
        calc.putVec4();
        final int customUniformsSize = calc.get();
        this.customUniformsSize = customUniformsSize;
        for (int i = 0; i < MAX_LAYERS; i++) {
            this.customUniforms[i] = WLRenderSystem.uniformBuffer("walksylib CustomUniforms slot " + i, customUniformsSize);
        }
        this.widgetInfoSize = 16 + MAX_LAYERS * 16 + MAX_WIDGETS * (16 * 16);
        for (int i = 0; i < MAX_LAYERS; i++) {
            this.widgetInfoBySlot[i] = WLRenderSystem.uniformBuffer("walksylib WidgetInfo slot " + i, this.widgetInfoSize);
        }
        final Std140Size bcalc = new Std140Size();
        bcalc.putFloat();
        bcalc.putFloat();
        bcalc.putVec2();
        final int bgConfigSize = bcalc.get();
        this.bgConfig = WLRenderSystem.uniformBuffer("walksylib BgConfig", bgConfigSize);
        this.bgConfigSize = bgConfigSize;
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
        this.hudWidgetCount = 0;
        this.layerElementSubmitted = false;
        this.splitThisFrame = false;
        this.layerMode = false;
        this.layerUpdate = false;
        this.earlyTextBoundary = EARLY_TEXT_LAYER;
        this.passHeld = false;
    }

    public boolean hudLayerNeedsSplit() {
        if (!this.layerValid) {
            return true;
        }
        return this.layerClock + this.dtSeconds >= 1.0 / this.refreshRate;
    }

    public boolean splitRequested() {
        return this.splitThisFrame;
    }

    public boolean claimLayerElement() {
        if (this.layerElementSubmitted) {
            return false;
        }
        this.layerElementSubmitted = true;
        return true;
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
        final long frame = WLRenderSystem.frameId();
        if (this.tryReuseLayer(frame)) {
            this.reuseFrame = frame;
            return;
        }
        this.reuseCandidate.clear();
        this.reuseCandidate.addAll(this.widgets);
        this.uploadWidgetInfo();
        final float popoverBoundary = (float) WLRenderSystem.contentStratum().popoverContentBoundary();
        this.scheduleOverlays(popoverBoundary);
        this.layerMode = this.computeLayerMode();
        this.stageSharedUniforms(popoverBoundary);
        this.rememberForReuse(frame);
        if (this.layerMode) {
            this.decideLayerUpdate();
            if (!this.layerUpdate) {
                this.commitAll(this.mainSlotCount);
                return;
            }
        } else {
            this.layerValid = false;
            this.layerUpdate = false;
        }
        this.commitAll(0);
    }

    private float @org.jetbrains.annotations.Nullable [] layerInlineBlur() {
        final List<Integer> radii = this.blurRadiiForSlot(0);
        final float[] inline = radii.size() == 1 ? LiquidGlassPrecomputeRuntime.inlineBlur(radii.get(0)) : null;
        this.layerBlurInline = inline != null;
        return inline;
    }

    public boolean layerBlurInline() {
        return this.layerBlurInline;
    }

    private boolean tryReuseLayer(final long frame) {
        if (!this.layerValid || this.reuseFrame != frame - 1L || !this.layerElementSubmitted || this.splitThisFrame) {
            return false;
        }
        if (!this.topWidgets.isEmpty() || this.textUsed || this.contentUsed || this.popoverUsed) {
            return false;
        }
        final int count = this.widgets.size();
        if (count == 0 || this.hudWidgetCount != count || count != this.reuseWidgets.size()) {
            return false;
        }
        if (WLRenderSystem.targetWidth() != this.lastLayerW || WLRenderSystem.targetHeight() != this.lastLayerH) {
            return false;
        }
        if (DevicePixels.perLogicalX() != this.reuseScaleX || DevicePixels.perLogicalY() != this.reuseScaleY || WLRenderSystem.glassAnim().debugStep() != this.reuseDebugStep) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            final LiquidGlassGuiElementRenderState now = this.widgets.get(i);
            if (!sameWidget(now, this.reuseWidgets.get(i)) || styleHash(now.style()) != this.reuseStyleHashes[i]) {
                return false;
            }
            final FadeState fade = this.fades.get(fadeKey(now));
            if (fade == null || fade.hover != Math.max(0f, Math.min(1f, now.hover())) || fade.focus != Math.max(0f, Math.min(1f, now.focus()))) {
                return false;
            }
        }
        for (int i = 0; i < count; i++) {
            this.fadesSeen.add(fadeKey(this.widgets.get(i)));
        }
        this.reuseWidgets.clear();
        this.reuseWidgets.addAll(this.widgets);
        this.slotCount = 1;
        this.mainSlotCount = 1;
        final List<Integer> radii = this.blurRadiiForSlot(0);
        radii.clear();
        radii.addAll(this.reuseRadii);
        final double interval = 1.0 / this.refreshRate;
        this.layerClock += this.dtSeconds;
        this.layerMode = true;
        this.layerUpdate = this.layerClock >= interval;
        if (this.layerUpdate) {
            this.layerClock = Math.min(this.layerClock - interval, interval);
        }
        return true;
    }

    private void rememberForReuse(final long frame) {
        this.reuseWidgets.clear();
        if (!this.layerMode) {
            this.reuseFrame = -1L;
            return;
        }
        this.reuseWidgets.addAll(this.reuseCandidate);
        if (this.reuseStyleHashes.length < this.reuseWidgets.size()) {
            this.reuseStyleHashes = new int[this.reuseWidgets.size() * 2];
        }
        for (int i = 0; i < this.reuseWidgets.size(); i++) {
            this.reuseStyleHashes[i] = styleHash(this.reuseWidgets.get(i).style());
        }
        this.reuseRadii.clear();
        this.reuseRadii.addAll(this.blurRadiiForSlot(0));
        this.reuseScaleX = DevicePixels.perLogicalX();
        this.reuseScaleY = DevicePixels.perLogicalY();
        this.reuseDebugStep = WLRenderSystem.glassAnim().debugStep();
        this.reuseFrame = frame;
    }

    private static long fadeKey(final LiquidGlassGuiElementRenderState w) {
        final GlassStyle s = w.style();
        return s.hasFadeKey() ? s.getFadeKey() : rectKey(w.x1(), w.y1(), w.x2(), w.y2());
    }

    private static boolean sameWidget(final LiquidGlassGuiElementRenderState a, final LiquidGlassGuiElementRenderState b) {
        return a.x1() == b.x1() && a.y1() == b.y1() && a.x2() == b.x2() && a.y2() == b.y2() && a.cornerRadius() == b.cornerRadius()
            && a.hover() == b.hover() && a.focus() == b.focus() && a.stretchX() == b.stretchX() && a.stretchY() == b.stretchY()
            && a.text() == b.text() && a.pose().equals(b.pose()) && Objects.equals(a.scissorArea(), b.scissorArea());
    }

    private static int styleHash(final GlassStyle s) {
        int h = s.getLayer();
        h = h * 31 + s.getBlurRadius();
        h = h * 31 + (s.hasBlurFalloff() ? 1 : 0);
        h = h * 31 + s.getTintColor();
        h = h * 31 + Float.floatToIntBits(s.getTintAlpha());
        h = h * 31 + Float.floatToIntBits(s.getOpacity());
        h = h * 31 + Float.floatToIntBits(s.getRefThickness());
        h = h * 31 + Float.floatToIntBits(s.getRefFactor());
        h = h * 31 + Float.floatToIntBits(s.getRefStrength());
        h = h * 31 + Float.floatToIntBits(s.getRefractionReach());
        h = h * 31 + Float.floatToIntBits(s.getChroma());
        h = h * 31 + Float.floatToIntBits(s.getToneFloor());
        h = h * 31 + Float.floatToIntBits(s.getToneCeiling());
        h = h * 31 + Float.floatToIntBits(s.getSmoothing());
        h = h * 31 + Float.floatToIntBits(s.getShadowFactor());
        h = h * 31 + Float.floatToIntBits(s.getShadowExpand());
        h = h * 31 + Float.floatToIntBits(s.getShadowColorAlpha());
        h = h * 31 + Float.floatToIntBits(s.getEdgeDarken());
        h = h * 31 + Float.floatToIntBits(s.getBevelHeight());
        h = h * 31 + Float.floatToIntBits(s.getInnerShadow());
        h = h * 31 + Float.floatToIntBits(s.getLensFold());
        h = h * 31 + Float.floatToIntBits(s.getGlareFactor());
        h = h * 31 + Float.floatToIntBits(s.getRimLightIntensity());
        return h * 31 + (s.getBakesText() ? 1 : 0);
    }

    private boolean computeLayerMode() {
        if (!this.layerElementSubmitted || this.widgets.isEmpty() || this.hudWidgetCount != this.widgets.size() || this.mainSlotCount != 1 || (this.splitThisFrame && !this.splitFromHud)) {
            return false;
        }
        if (this.textUsed || this.contentUsed || this.popoverUsed || this.slotHasStrata(0) || this.slotHasFocus[0] || this.slotBoundsEmpty[0]) {
            return false;
        }
        return WLRenderSystem.glassAnim().debugStep() > 1.5f;
    }

    private void decideLayerUpdate() {
        final long now = System.nanoTime();
        if (now - this.refreshCheckedAt > 1_000_000_000L || this.refreshCheckedAt == 0L) {
            this.refreshRate = Math.max(1, main.walksy.lib.core.platform.WLClient.refreshRate());
            this.refreshCheckedAt = now;
        }
        final int fbW = WLRenderSystem.targetWidth();
        final int fbH = WLRenderSystem.targetHeight();
        final int[] area = this.getPassScissor(0, fbW, fbH);
        if (area == null) {
            this.layerArea[0] = 0;
            this.layerArea[1] = 0;
            this.layerArea[2] = fbW;
            this.layerArea[3] = fbH;
        } else {
            System.arraycopy(area, 0, this.layerArea, 0, 4);
        }
        boolean changed = !this.layerValid || fbW != this.lastLayerW || fbH != this.lastLayerH || !Arrays.equals(this.layerArea, this.lastLayerArea);
        changed = changed || differs(this.widgetInfoStaging[0], this.widgetInfoUploaded[0], 0);
        changed = changed || differs(this.customStaging[0], this.customUploaded[0], CUSTOM_VOLATILE_BYTES);
        changed = changed || differs(this.bgStaging, this.bgUploaded, 0);
        final double interval = 1.0 / this.refreshRate;
        this.layerClock += this.dtSeconds;
        if (changed) {
            this.layerUpdate = true;
            this.layerClock = 0.0;
        } else if (this.layerClock >= interval) {
            this.layerUpdate = true;
            this.layerClock = Math.min(this.layerClock - interval, interval);
        } else {
            this.layerUpdate = false;
        }
        if (this.layerUpdate) {
            this.lastLayerW = fbW;
            this.lastLayerH = fbH;
            System.arraycopy(this.layerArea, 0, this.lastLayerArea, 0, 4);
        }
    }

    private static boolean differs(@org.jetbrains.annotations.Nullable final ByteBuffer staged, @org.jetbrains.annotations.Nullable final ByteBuffer uploaded, final int skip) {
        if (staged == null || uploaded == null) {
            return true;
        }
        final int length = staged.capacity() - skip;
        return staged.slice(skip, length).mismatch(uploaded.slice(skip, length)) >= 0;
    }

    public boolean layerMode() {
        return this.layerMode;
    }

    public boolean layerUpdate() {
        return this.layerUpdate;
    }

    public int[] layerArea() {
        return this.layerArea;
    }

    public void markLayerValid() {
        this.layerValid = true;
    }

    private void commitAll(final int fromSlot) {
        final int outW = WLRenderSystem.targetWidth();
        final int outH = WLRenderSystem.targetHeight();
        if (outW != this.samplerInfoW || outH != this.samplerInfoH) {
            this.samplerInfoW = outW;
            this.samplerInfoH = outH;
            try (final WLMapping map = this.samplerInfo.map()) {
                final Std140Writer b = map.writer();
                b.putVec2((float) outW, (float) outH);
                b.putVec2((float) outW, (float) outH);
            }
        }
        for (int slot = fromSlot; slot < this.slotCount; slot++) {
            this.widgetInfoUploaded[slot] = commit(this.widgetInfoBySlot[slot], this.widgetInfoStaging[slot], this.widgetInfoUploaded[slot]);
            this.customUploaded[slot] = commit(this.customUniforms[slot], this.customStaging[slot], this.customUploaded[slot]);
        }
        this.bgUploaded = commit(this.bgConfig, this.bgStaging, this.bgUploaded);
    }

    private static ByteBuffer commit(final WLUniformBuffer target, final ByteBuffer staged, @org.jetbrains.annotations.Nullable ByteBuffer uploaded) {
        staged.clear();
        if (uploaded != null && staged.mismatch(uploaded.clear()) < 0) {
            return uploaded;
        }
        try (final WLMapping map = target.map()) {
            map.data().put(staged);
        }
        if (uploaded == null) {
            uploaded = ByteBuffer.allocateDirect(staged.capacity()).order(ByteOrder.nativeOrder());
        }
        uploaded.clear().put(staged.clear());
        staged.clear();
        return uploaded;
    }

    private static ByteBuffer staging(final ByteBuffer[] buffers, final int slot, final int size) {
        if (buffers[slot] == null) {
            buffers[slot] = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        return buffers[slot].clear();
    }

    private void stageSharedUniforms(final float popoverBoundary) {
        final AppearanceConfig appearance = WalksyLib.getInstance().getAppearancePage();
        final LiquidGlassAnim anim = WLRenderSystem.glassAnim();
        final Minecraft mc = Minecraft.getInstance();
        final int outH = WLRenderSystem.targetHeight();
        final double cursorX = mc.mouseHandler.xpos();
        final double cursorY = mc.mouseHandler.ypos();
        final float scaleX = DevicePixels.perLogicalX();
        final float scaleY = DevicePixels.perLogicalY();
        final int fbH = outH;
        final float time = (System.nanoTime() - START_NANOS) / NANOS_PER_SECOND;
        this.layerBlurInline = false;
        for (int slot = 0; slot < this.slotCount; slot++) {
            {
                final Std140Writer b = Std140Writer.into(staging(this.customStaging, slot, this.customUniformsSize));
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
                final boolean layered = this.layerMode && slot < this.mainSlotCount;
                final boolean over = this.slotBlendsOver(slot);
                b.putFloat(layered || over ? 1f : 0f);
                b.putFloat(this.textUsed ? 1f : 0f);
                b.putFloat(over && !layered ? 1f : 0f);
                final boolean strata = this.slotHasStrata(slot);
                for (int rect = 0; rect < 2; rect++) {
                    final int at = slot * 8 + rect * 4;
                    if (strata) {
                        b.putVec4(1f, 1f, 0f, 0f);
                    } else {
                        b.putVec4(this.slotFastRect[at], this.slotFastRect[at + 1], this.slotFastRect[at + 2], this.slotFastRect[at + 3]);
                    }
                }
                final float[] inline = layered && slot == 0 ? this.layerInlineBlur() : null;
                if (inline != null) {
                    b.putVec4(inline[0], inline[1], inline[2], 1f);
                } else {
                    b.putVec4(0f, 0f, 0f, 0f);
                }
            }
        }
        if (this.bgStaging == null) {
            this.bgStaging = ByteBuffer.allocateDirect(this.bgConfigSize).order(ByteOrder.nativeOrder());
        }
        {
            final Std140Writer b = Std140Writer.into(this.bgStaging.clear());
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
        this.textCompositePass = this.scheduleOverlay(this.textUsed, this.earlyTextBoundary, this.textAtStart, this.textAtEnd);
        this.contentCompositePass = this.scheduleOverlay(this.contentUsed, CONTENT_LAYER, this.contentAtStart, this.contentAtEnd);
        this.popoverCompositePass = this.scheduleOverlay(this.popoverUsed, popoverBoundary, this.popoverAtStart, this.popoverAtEnd);
    }

    public void setStrataUsed(final boolean text, final boolean content, final boolean popover) {
        this.textUsed = text;
        this.contentUsed = content;
        this.popoverUsed = popover;
    }

    private int scheduleOverlay(final boolean used, final float boundary, final boolean[] atStart, final boolean[] atEnd) {
        if (!used || this.mainSlotCount <= 0) {
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

    public void lowerEarlyTextBoundary(final int layer) {
        this.earlyTextBoundary = Math.min(this.earlyTextBoundary, layer);
    }

    public void holdGlassPass() {
        this.passHeld = true;
    }

    public void releaseGlassPass() {
        this.passHeld = false;
    }

    public void requestGlassPass(final WLGraphics2D graphics) {
        if (!this.blurPassRequested && !this.passHeld) {
            graphics.blurBeforeThisStratum();
            this.blurPassRequested = true;
            if (!this.splitThisFrame) {
                this.splitThisFrame = true;
                this.splitFromHud = WLRenderSystem.inHud();
            }
        }
    }

    public void addWidget(final LiquidGlassGuiElementRenderState element, final boolean hud) {
        final int before = this.widgets.size();
        add(element, this.widgets, this.widgetsPerLayer);
        if (hud && this.widgets.size() > before) {
            this.hudWidgetCount++;
        }
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
            {
                final ByteBuffer raw = staging(this.widgetInfoStaging, slotIdx, this.widgetInfoSize);
                this.slotHasFocus[slotIdx] = false;
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
                    if (idx < 0 && s.getOpacity() > 0f) {
                        this.slotNeedsScene[slotIdx] = true;
                    }
                    final var w = slotWidgets.get(i);
                    final long key = s.hasFadeKey() ? s.getFadeKey() : rectKey(w.x1(), w.y1(), w.x2(), w.y2());
                    this.fadesSeen.add(key);
                    final FadeState fs = this.fades.computeIfAbsent(key, k -> new FadeState());
                    fs.hover = this.smoothToward(fs.hover, Math.max(0f, Math.min(1f, w.hover())), this.dtSeconds, 0.12f);
                    fs.focus = this.smoothToward(fs.focus, Math.max(0f, Math.min(1f, w.focus())), this.dtSeconds, 0.18f);
                    if (fs.focus > 0f || w.focus() > 0f) {
                        this.slotHasFocus[slotIdx] = true;
                    }
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
                    final boolean bakes = s.getBakesText() && slotIdx < this.mainSlotCount && this.earlyTextBoundary == EARLY_TEXT_LAYER;
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
            final float zonePx = Math.max(Math.min(LENS_ZONE * thickPx, LENS_ZONE_FRAC * 0.5f * Math.min(widgetW, widgetH)), 1e-4f);
            final float fold = LENS_FOLD_MIN + (LENS_FOLD_MAX - LENS_FOLD_MIN) * Math.max(0f, Math.min(1f, s.getLensFold()));
            base = Math.min(Math.abs(reachFrac) * Math.min(widgetW, widgetH), fold * zonePx / (s.isSoftLens() ? LENS_SOFT_POWER : LENS_POWER));
            inward = reachFrac > 0f;
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
        this.slotReadRegionEmpty[slotIdx] = true;
        this.slotNeedsScene[slotIdx] = false;
        for (int rect = 0; rect < 2; rect++) {
            final int at = slotIdx * 8 + rect * 4;
            this.slotFastRect[at] = 1f;
            this.slotFastRect[at + 1] = 1f;
            this.slotFastRect[at + 2] = 0f;
            this.slotFastRect[at + 3] = 0f;
        }
        if (slotWidgets.isEmpty()) {
            return;
        }
        float tx0 = Float.MAX_VALUE;
        float ty0 = Float.MAX_VALUE;
        float tx1 = -Float.MAX_VALUE;
        float ty1 = -Float.MAX_VALUE;
        float qx0 = Float.MAX_VALUE;
        float qy0 = Float.MAX_VALUE;
        float qx1 = -Float.MAX_VALUE;
        float qy1 = -Float.MAX_VALUE;
        final List<Integer> slotRadii = this.blurRadiiForSlot(slotIdx);
        final boolean litNeedsScene = slotRadii.isEmpty() || slotRadii.get(0) > LIT_FROM_BLUR_MAX_RADIUS;
        final float hoverInflate = Math.max(0f, WLRenderSystem.glassAnim().hoverScalePx());
        final float focusInflate = Math.max(0f, WLRenderSystem.glassAnim().focusScalePx());
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
        final float smoothSlack = (positives > 1 ? kSum * 0.25f * fbH : 0f) + 2f;
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
            final int blurRadius = appearance.blurRadius(Math.max(0, s.getBlurRadius()), s.getLayer());
            if (blurRadius <= 0 || s.hasBlurFalloff() || (litNeedsScene && s.getShadowExpand() < 0f && s.getShadowFactor() < 0f && s.getShadowColorAlpha() > 0f)) {
                this.slotNeedsScene[slotIdx] = true;
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
            final float hoverReach = Math.max(Math.max(0f, Math.min(1f, w.hover())), fade != null ? fade.hover : 0f);
            final float focusReach = Math.max(Math.max(0f, Math.min(1f, w.focus())), fade != null ? fade.focus : 0f);
            final float inflate = hoverInflate * hoverReach + focusInflate * focusReach;
            final float bodySlack = (ringed ? 0.01f : 0.001f) * fbH + smoothSlack + inflate;
            if (slotWidgets.size() == 1 && hoverReach <= 0f && focusReach <= 0f && stretchReach <= 0f && this.fastInteriorAllowed(s, blurRadius)) {
                final float depth = fastInteriorDepth(s, scaleY, ry1 - ry0);
                final float corner = Math.max(depth, w.cornerRadius() * scaleY + 2f);
                for (int rect = 0; rect < 2; rect++) {
                    final float insetX = rect == 0 ? corner : depth;
                    final float insetY = rect == 0 ? depth : corner;
                    final float fx0 = Math.max(rx0 + insetX, scX0 + 1f);
                    final float fy0 = Math.max(ry0 + insetY, scY0 + 1f);
                    final float fx1 = Math.min(rx1 - insetX, scX1 - 1f);
                    final float fy1 = Math.min(ry1 - insetY, scY1 - 1f);
                    if (fx1 > fx0 && fy1 > fy0) {
                        final int at = slotIdx * 8 + rect * 4;
                        this.slotFastRect[at] = fx0;
                        this.slotFastRect[at + 1] = fy0;
                        this.slotFastRect[at + 2] = fx1;
                        this.slotFastRect[at + 3] = fy1;
                    }
                }
            }
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
                final float reach = maxBackdropReachPx(s, scaleY, rx1 - rx0, ry1 - ry0, fbH, ownNormals);
                tx0 = Math.min(tx0, bodyX0 - reach);
                ty0 = Math.min(ty0, bodyY0 - reach);
                tx1 = Math.max(tx1, bodyX1 + reach);
                ty1 = Math.max(ty1, bodyY1 + reach);
                this.slotReadRegionEmpty[slotIdx] = false;
                if (blurRadius > 0) {
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
        this.slotReadRegion[base] = tx0;
        this.slotReadRegion[base + 1] = ty0;
        this.slotReadRegion[base + 2] = tx1;
        this.slotReadRegion[base + 3] = ty1;
    }

    private boolean fastInteriorAllowed(final GlassStyle s, final int blurRadius) {
        if (blurRadius <= 0 || s.hasBlurFalloff() || s.getOpacity() < 1f || s.getSmoothing() < 0f || s.getRefractionShift() != 0f || LiquidGlassConstants.PIXELATED_GRID || WLRenderSystem.glassAnim().debugStep() <= 1.5f) {
            return false;
        }
        if (this.textUsed && (s.getBakesText() || s.getLayer() > this.earlyTextBoundary)) {
            return false;
        }
        if (this.contentUsed && s.getLayer() > CONTENT_LAYER) {
            return false;
        }
        return !this.popoverUsed || s.getLayer() <= WLRenderSystem.contentStratum().popoverContentBoundary();
    }

    private static float fastInteriorDepth(final GlassStyle s, final float scaleY, final float heightPx) {
        float depth = FAST_INTERIOR_EDGE_REACH;
        final float lensZone = Math.abs(s.getRefractionReach()) > 1e-4f && s.getBevelHeight() * scaleY <= 1e-4f ? LENS_ZONE : 1f;
        depth = Math.max(depth, s.getRefThickness() * scaleY * lensZone + 2f);
        if (s.getInnerShadow() > 0.0001f) {
            final float inset = Math.min(7f, 0.5f * heightPx * 0.45f);
            final float width = Math.max(1f, inset * (4f / 7f));
            depth = Math.max(depth, inset + 6f * width + 2f);
        }
        if (s.getBevelHeight() * scaleY <= 0.0001f) {
            final float fresnelRange = s.getRefFresnelRange() * scaleY;
            final float glareRange = s.getGlareRange() * scaleY;
            depth = Math.max(depth, (1f + Math.max(0f, s.getRefFresnelHardness() / 100f)) * 0.006f * fresnelRange * fresnelRange + 2f);
            depth = Math.max(depth, (1f + Math.max(0f, s.getGlareHardness() / 100f)) * 0.006f * glareRange * glareRange + 2f);
        }
        return depth;
    }

    public boolean slotBlendsOver(final int slot) {
        if (slot < 0 || slot >= this.slotCount || this.slotNeedsScene[slot] || WLRenderSystem.glassAnim().debugStep() <= 1.5f || this.getBlurRadiiForSlot(slot).isEmpty()) {
            return false;
        }
        return !this.slotHasStrata(slot);
    }

    private boolean slotHasStrata(final int slot) {
        return this.textAtStart[slot] || this.textAtEnd[slot] || this.contentAtStart[slot] || this.contentAtEnd[slot] || this.popoverAtStart[slot] || this.popoverAtEnd[slot];
    }

    public int[] getReadRegion(final int slot, final int fbW, final int fbH) {
        return this.regionOf(this.slotReadRegion, this.slotReadRegionEmpty, slot, fbW, fbH);
    }

    public int[] getBlurRegion(final int slot, final int fbW, final int fbH) {
        return this.regionOf(this.slotBlurRegion, this.slotBlurRegionEmpty, slot, fbW, fbH);
    }

    private int[] regionOf(final float[] regions, final boolean[] empty, final int slot, final int fbW, final int fbH) {
        if (slot < 0 || slot >= this.slotCount || empty[slot]) {
            return new int[] {0, 0, 0, 0};
        }
        final int base = slot * 4;
        final int ix0 = Math.max(0, (int) Math.floor(regions[base]));
        final int iy0 = Math.max(0, (int) Math.floor(regions[base + 1]));
        final int ix1 = Math.min(fbW, (int) Math.ceil(regions[base + 2]));
        final int iy1 = Math.min(fbH, (int) Math.ceil(regions[base + 3]));
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
