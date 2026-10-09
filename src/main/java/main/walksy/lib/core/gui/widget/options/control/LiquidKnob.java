package main.walksy.lib.core.gui.widget.options.control;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.gui.style.Theme;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.util.Mth;

public final class LiquidKnob {
    static final float DRAG_STIFFNESS = 300f;
    static final float DRAG_DAMPING = 34.6f;
    static final float SETTLE_STIFFNESS = 120f;
    static final float SETTLE_DAMPING = 15f;
    private static final float STRETCH_STIFFNESS = 150f;
    private static final float STRETCH_DAMPING = 9f;
    private static final float STRETCH_PER_VELOCITY = 0.035f;
    private static final float MAX_KNOB_STRETCH = 4f;
    private static final float MIN_HEIGHT_SCALE = 0.8f;
    private static final float DISABLED_MIX = 0.5f;
    static final float MATERIAL_IN_SPEED = 20f;
    static final float MATERIAL_OUT_SPEED = 12f;
    static final float MELT_BLEND_SPAN = 0.4f;
    static final float SIZE_STIFFNESS = 400f;
    static final float SIZE_DAMPING = 20f;
    static final float SIZE_FOLLOW_SPEED = 40f;
    static final float REACH_GATE_START = 0.85f;
    private final Spring1D posSpring;
    private final Spring1D stretchSpring = new Spring1D(0f);
    private float materialAmount;
    private final Spring1D sizeSpring = new Spring1D(0f);
    private final float uiScale;
    private final float widthRatio;
    private float restWidthRatio;
    private final GlassStyle restStyle;
    private final GlassStyle glassStyle;
    private final long fadeKey;
    private float availFade = 1f;

    public static int ACCENT_COLOR() {
        return Theme.get().accentRgb();
    }

    public void setAvailability(final float amount) {
        this.availFade = Math.max(0f, Math.min(1f, amount));
    }

    static GlassStyle dimmed(final GlassStyle style, final float availFade) {
        if (availFade >= 0.999f) {
            return style;
        }
        final GlassStyle flat = GlassStyle.inactive(style);
        flat.tint(ColorUtils.lerpColor(flat.getTintColor(), Theme.get().trackFillSolid(), DISABLED_MIX), Math.max(flat.getTintAlpha(), Theme.tintAlpha(Theme.SURFACE_OPACITY)));
        final GlassStyle blend = availFade <= 0.001f ? flat : GlassStyle.lerp(flat, style, availFade);
        blend.layer(style.getLayer());
        if (style.hasFadeKey()) {
            blend.fadeKey(style.getFadeKey());
        }
        return blend;
    }

    static final class Spring1D {
        private static final float MAX_STEP = 1f / 240f;
        float value;
        float velocity;

        Spring1D(final float initial) {
            this.value = initial;
        }

        void step(final float target, final float stiffness, final float damping, final float dt) {
            if (UiStyle.minecraft()) {
                this.value = target;
                this.velocity = 0f;
                return;
            }
            if (dt <= 0f) {
                return;
            }
            int steps = (int) Math.ceil(dt / MAX_STEP);
            float h = dt / steps;
            for (int i = 0; i < steps; i++) {
                final float accel = (target - this.value) * stiffness - this.velocity * damping;
                this.velocity += accel * h;
                this.value += this.velocity * h;
            }
        }
}

    public static int knobLayer() {
        return ControlLayers.KNOB;
    }

    public LiquidKnob(final float initial01, final float widthRatio, final float uiScale, final long fadeKey) {
        this(initial01, widthRatio, uiScale, fadeKey, ControlLayers.KNOB);
    }

    public LiquidKnob(final float initial01, final float widthRatio, final float uiScale, final long fadeKey, final int knobLayer) {
        this(initial01, widthRatio, uiScale, fadeKey, knobLayer, false);
    }

    public LiquidKnob(final float initial01, final float widthRatio, final float uiScale, final long fadeKey, final int knobLayer, final boolean overAccent) {
        this.posSpring = new Spring1D(initial01);
        this.widthRatio = widthRatio;
        this.restWidthRatio = widthRatio;
        this.uiScale = uiScale;
        this.fadeKey = fadeKey;
        this.restStyle = restKnobStyle(knobLayer);
        this.glassStyle = overAccent ? toggleBevelKnobStyle(knobLayer) : bevelKnobStyle(knobLayer);
    }

    LiquidKnob restWidthRatio(final float ratio) {
        this.restWidthRatio = ratio;
        return this;
    }

    static GlassStyle restKnobStyle(final int knobLayer) {
        return new GlassStyle()
            .layer(knobLayer)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .tint(Theme.get().knobBody(), 1.25f)
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .refractionReach(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .brightEdge(0f)
            .sideDarken(0x000000, 0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .innerShadow(0f)
            .outlineDirectional(Theme.get().outlineWidth(), Theme.get().shadowColor(), 0f, 0f, 0f, 0f);
    }

    static GlassStyle glassKnobStyle(final int knobLayer) {
        return new GlassStyle()
            .layer(knobLayer)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .tint(Theme.get().controlTint(), Theme.get().controlTintAlpha())
            .refractionStrength(1f)
            .refractionThickness(3f)
            .refractionFactor(1f)
            .refractionDispersion(3f)
            .refractionReach(0.2f)
            .fresnelRange(8f)
            .fresnelHardness(6f)
            .fresnelFactor(20f)
            .glareRange(6f)
            .glareHardness(9f)
            .brightEdge(LiquidGlassConstants.KNOB_BRIGHT_EDGE)
            .edgeDarken(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .standardShadowDirectional(Theme.get().outlineWidth(), Theme.get().shadowColor(), Theme.get().outlineAlpha());
    }

    public static GlassStyle bevelKnobStyle(final int knobLayer) {
        return new GlassStyle()
            .layer(knobLayer)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .tint(Theme.get().knobBody(), 0f)
            .bevel(0f)
            .refractionReach(0.30f)
            .refractionThickness(2f)
            .refractionFactor(1.6f)
            .refractionDispersion(10f)
            .fresnelRange(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .lensFold(1f)
            .refractionShift(0f)
            .edgeDarken(0f)
            .brightEdge(LiquidGlassConstants.KNOB_BRIGHT_EDGE)
            .sideDarken(0x000000, 0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .standardShadowDirectional(Theme.get().outlineWidth(), Theme.get().shadowColor(), Theme.get().outlineAlpha());
    }

    public static GlassStyle toggleBevelKnobStyle(final int knobLayer) {
        return bevelKnobStyle(knobLayer);
    }

    public static GlassStyle grayTrackStyle() {
        return new GlassStyle()
            .layer(ControlLayers.BASE)
            .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
            .tint(Theme.get().trackFill(), Theme.get().trackFillAlpha())
            .refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .edgeDarken(0f)
            .brightEdge(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    public static GlassStyle accentFillStyle() {
        return grayTrackStyle()
            .layer(ControlLayers.FILL)
            .tint(ACCENT_COLOR(), 1.25f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    public float position01() {
        return this.posSpring.value;
    }

    float springValue() {
        return this.posSpring.value;
    }

    float springVelocity() {
        return this.posSpring.velocity;
    }

    void seedSpring(final float value, final float velocity) {
        this.posSpring.value = value;
        this.posSpring.velocity = velocity;
    }

    void settle() {
        this.materialAmount = 0f;
        this.sizeSpring.value = 0f;
        this.sizeSpring.velocity = 0f;
        this.stretchSpring.value = 0f;
        this.stretchSpring.velocity = 0f;
    }

    public void extract(final WLGraphics2D graphics, float dt, final float target01, final boolean pressed, final boolean dragging, final float travelMin, final float travelMax, final float centerY, final float restH, final float pressH, final float alpha) {
        if (!Float.isFinite(dt) || dt < 0f) {
            dt = 0f;
        }
        this.posSpring.step(target01, dragging ? DRAG_STIFFNESS : SETTLE_STIFFNESS, dragging ? DRAG_DAMPING : SETTLE_DAMPING, dt);
        if (!Float.isFinite(this.posSpring.value)) {
            this.posSpring.value = target01;
            this.posSpring.velocity = 0f;
        }
        final float slideGlass = Mth.clamp(Math.abs(this.posSpring.value - target01) / MELT_BLEND_SPAN, 0f, 1f);
        final float materialTarget = UiStyle.minecraft() ? 0f : pressed ? 1f : slideGlass;
        this.materialAmount = MathUtils.approach(this.materialAmount, materialTarget, materialTarget > this.materialAmount ? MATERIAL_IN_SPEED : MATERIAL_OUT_SPEED, dt);
        if (pressed) {
            this.sizeSpring.step(this.materialAmount, SIZE_STIFFNESS, SIZE_DAMPING, dt);
        } else {
            this.sizeSpring.value = MathUtils.approach(this.sizeSpring.value, this.materialAmount, SIZE_FOLLOW_SPEED, dt);
            this.sizeSpring.velocity = 0f;
        }
        if (!Float.isFinite(this.sizeSpring.value)) {
            this.sizeSpring.value = this.materialAmount;
            this.sizeSpring.velocity = 0f;
        }
        final float sizeAmount = Math.max(0f, Math.min(1.25f, this.sizeSpring.value));
        final float travelPx = travelMax - travelMin;
        final float speedPx = Math.abs(this.posSpring.velocity) * travelPx;
        final float stretchTarget = Math.min(MAX_KNOB_STRETCH * this.uiScale, speedPx * STRETCH_PER_VELOCITY);
        this.stretchSpring.step(stretchTarget, STRETCH_STIFFNESS, STRETCH_DAMPING, dt);
        if (!Float.isFinite(this.stretchSpring.value)) {
            this.stretchSpring.value = 0f;
            this.stretchSpring.velocity = 0f;
        } else {
            this.stretchSpring.value = Math.min(this.stretchSpring.value, MAX_KNOB_STRETCH * 1.5f * this.uiScale);
        }
        final float stretch = Math.max(0f, this.stretchSpring.value);
        final float baseH = restH + (pressH - restH) * sizeAmount;
        final float ratio = this.restWidthRatio + (this.widthRatio - this.restWidthRatio) * Math.min(1f, sizeAmount);
        final float baseW = baseH * ratio;
        final float knobW = baseW + stretch;
        final float knobH = baseH * Math.max(MIN_HEIGHT_SCALE, baseW / knobW);
        final float cx = travelMin + travelPx * Mth.clamp(this.posSpring.value, 0f, 1f);
        GlassStyle knob = GlassStyle.lerp(this.restStyle, this.glassStyle, this.materialAmount);
        knob = dimmed(knob, this.availFade);
        knob = knob.fadeKey(this.fadeKey).scaled(this.uiScale).opacity(knob.getOpacity() * alpha);
        graphics.glass()
            .dimensions(cx - knobW / 2f, centerY - knobH / 2f, knobW, knobH)
            .cornerRadius(knobH / 2f)
            .style(knob)
            .extract();
    }
}
