package main.walksy.lib.core.gui.widget.options.control;

import main.walksy.lib.core.gui.style.UiStyle;
import main.walksy.lib.core.render.WLGraphics2D;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.MathUtils;
import net.minecraft.util.Mth;

public final class LiquidKnob2D {
    private static final float AUTO_STIFFNESS = 3600f;
    private static final float AUTO_DAMPING = 2f * (float) Math.sqrt(AUTO_STIFFNESS);
    private static final float LENS_THICKNESS_FRACTION = 0.3f;
    private static final float LENS_REACH = 0.6f;
    private final LiquidKnob.Spring1D posSpringX;
    private final LiquidKnob.Spring1D posSpringY;
    private final LiquidKnob.Spring1D sizeSpring = new LiquidKnob.Spring1D(0f);
    private float materialAmount;
    private final float uiScale;
    private final long fadeKey;
    private final GlassStyle restStyle;
    private final GlassStyle glassStyle;
    private final float restDiameter;
    private final float pressDiameter;

    public LiquidKnob2D(final float initialX01, final float initialY01, final float uiScale, final long fadeKey, final int layer, final float restDiameter, final float pressDiameter) {
        this.posSpringX = new LiquidKnob.Spring1D(initialX01);
        this.posSpringY = new LiquidKnob.Spring1D(initialY01);
        this.uiScale = uiScale;
        this.fadeKey = fadeKey;
        this.restStyle = LiquidKnob.restKnobStyle(layer);
        this.glassStyle = LiquidKnob.glassKnobStyle(layer);
        this.restDiameter = restDiameter;
        this.pressDiameter = pressDiameter;
    }

    public LiquidKnob2D withoutInnerShadow() {
        this.glassStyle.innerShadow(0f);
        return this;
    }

    float positionX01() {
        return this.posSpringX.value;
    }

    float positionY01() {
        return this.posSpringY.value;
    }

    float diameter() {
        return this.pressDiameter * 1.25f * this.uiScale;
    }

    public void extract(final WLGraphics2D graphics, float dt, final float targetX01, final float targetY01, final boolean dragging, final boolean auto, final float travelMinX, final float travelMaxX, final float travelMinY, final float travelMaxY, final float alpha) {
        if (!Float.isFinite(dt) || dt < 0f) {
            dt = 0f;
        }
        final boolean live = dragging || auto;
        final float stiffness = dragging ? LiquidKnob.DRAG_STIFFNESS : auto ? AUTO_STIFFNESS : LiquidKnob.SETTLE_STIFFNESS;
        final float damping = dragging ? LiquidKnob.DRAG_DAMPING : auto ? AUTO_DAMPING : LiquidKnob.SETTLE_DAMPING;
        this.posSpringX.step(targetX01, stiffness, damping, dt);
        this.posSpringY.step(targetY01, stiffness, damping, dt);
        if (!Float.isFinite(this.posSpringX.value)) {
            this.posSpringX.value = targetX01;
            this.posSpringX.velocity = 0f;
        }
        if (!Float.isFinite(this.posSpringY.value)) {
            this.posSpringY.value = targetY01;
            this.posSpringY.velocity = 0f;
        }
        final float dx = this.posSpringX.value - targetX01;
        final float dy = this.posSpringY.value - targetY01;
        final float dist = (float) Math.sqrt(dx * dx + dy * dy);
        final float slideGlass = Mth.clamp(dist / LiquidKnob.MELT_BLEND_SPAN, 0f, 1f);
        final float materialTarget = UiStyle.minecraft() ? 0f : live ? 1f : slideGlass;
        this.materialAmount = MathUtils.approach(this.materialAmount, materialTarget, materialTarget > this.materialAmount ? LiquidKnob.MATERIAL_IN_SPEED : LiquidKnob.MATERIAL_OUT_SPEED, dt);
        if (live) {
            this.sizeSpring.step(this.materialAmount, LiquidKnob.SIZE_STIFFNESS, LiquidKnob.SIZE_DAMPING, dt);
        } else {
            this.sizeSpring.value = MathUtils.approach(this.sizeSpring.value, this.materialAmount, LiquidKnob.SIZE_FOLLOW_SPEED, dt);
            this.sizeSpring.velocity = 0f;
        }
        if (!Float.isFinite(this.sizeSpring.value)) {
            this.sizeSpring.value = this.materialAmount;
            this.sizeSpring.velocity = 0f;
        }
        final float sizeAmount = Math.max(0f, Math.min(1.25f, this.sizeSpring.value));
        final float baseD = this.restDiameter + (this.pressDiameter - this.restDiameter) * sizeAmount;
        final float d = baseD * this.uiScale;
        final float cx = travelMinX + (travelMaxX - travelMinX) * Mth.clamp(this.posSpringX.value, 0f, 1f);
        final float cy = travelMinY + (travelMaxY - travelMinY) * Mth.clamp(this.posSpringY.value, 0f, 1f);
        GlassStyle knob = GlassStyle.lerp(this.restStyle, this.glassStyle, this.materialAmount);
        final float reachEase = this.materialAmount <= LiquidKnob.REACH_GATE_START ? 0f : (this.materialAmount - LiquidKnob.REACH_GATE_START) / (1f - LiquidKnob.REACH_GATE_START);
        knob.refractionReach(LENS_REACH * reachEase);
        knob.refractionThickness(reachEase > 0f ? baseD * LENS_THICKNESS_FRACTION : 0f);
        knob.refractionFactor(1f);
        knob = knob.fadeKey(this.fadeKey).scaled(this.uiScale).opacity(knob.getOpacity() * alpha);
        graphics.glass().dimensions(cx - d / 2f, cy - d / 2f, d, d).cornerRadius(d / 2f).style(knob).extract();
    }
}
