package main.walksy.lib.core.render.glass.runtime;

import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;
import net.minecraft.util.Mth;

public final class LiquidGlassAnim {
    private boolean init;
    private float tintAlpha;
    private float smoothing;
    private float blurRadiusF;
    private float shadowExpand;
    private float shadowFactor;
    private float shadowOffsetX;
    private float shadowOffsetY;
    private float refStrength;
    private float refThickness;
    private float refFactor;
    private float refDispersion;
    private float refFresnelRange;
    private float refFresnelHardness;
    private float refFresnelFactor;
    private float glareRange;
    private float glareHardness;
    private float glareConvergence;
    private float glareOppositeFactor;
    private float glareFactor;
    private float glareAngleRad;
    private float debugStep;
    private float pixelatedGridSize;
    private float hoverScalePx;
    private float focusScalePx;
    private float focusBorderWidthPx;
    private float focusBorderIntensity;
    private float focusBorderSpeed;

    public LiquidGlassAnim() {
    }

    public void update(final double dtSeconds) {
        final float tau = 0.15f;
        final float a = alpha(dtSeconds, tau);
        if (!this.init) {
            this.tintAlpha = LiquidGlassConstants.DEFAULT_TINT_ALPHA;
            this.smoothing = LiquidGlassConstants.DEFAULT_SMOOTHING;
            this.blurRadiusF = LiquidGlassConstants.DEFAULT_BLUR_RADIUS;
            this.shadowExpand = LiquidGlassConstants.DEFAULT_SHADOW_EXPAND;
            this.shadowFactor = LiquidGlassConstants.DEFAULT_SHADOW_FACTOR;
            this.shadowOffsetX = LiquidGlassConstants.DEFAULT_SHADOW_OFFSET_X;
            this.shadowOffsetY = LiquidGlassConstants.DEFAULT_SHADOW_OFFSET_Y;
            this.refStrength = LiquidGlassConstants.DEFAULT_REF_STRENGTH;
            this.refThickness = LiquidGlassConstants.DEFAULT_REF_THICKNESS;
            this.refFactor = LiquidGlassConstants.DEFAULT_REF_FACTOR;
            this.refDispersion = LiquidGlassConstants.DEFAULT_REF_DISPERSION;
            this.refFresnelRange = LiquidGlassConstants.DEFAULT_REF_FRESNEL_RANGE;
            this.refFresnelHardness = LiquidGlassConstants.DEFAULT_REF_FRESNEL_HARDNESS;
            this.refFresnelFactor = LiquidGlassConstants.DEFAULT_REF_FRESNEL_FACTOR;
            this.glareRange = LiquidGlassConstants.DEFAULT_GLARE_RANGE;
            this.glareHardness = LiquidGlassConstants.DEFAULT_GLARE_HARDNESS;
            this.glareConvergence = LiquidGlassConstants.DEFAULT_GLARE_CONVERGENCE;
            this.glareOppositeFactor = LiquidGlassConstants.DEFAULT_GLARE_OPPOSITE_FACTOR;
            this.glareFactor = LiquidGlassConstants.DEFAULT_GLARE_FACTOR;
            this.glareAngleRad = LiquidGlassConstants.DEFAULT_GLARE_ANGLE_RAD;
            this.debugStep = LiquidGlassConstants.DEBUG_STEP;
            this.pixelatedGridSize = LiquidGlassConstants.PIXELATED_GRID_SIZE;
            this.hoverScalePx = LiquidGlassConstants.HOVER_SCALE_PX;
            this.focusScalePx = LiquidGlassConstants.FOCUS_SCALE_PX;
            this.focusBorderWidthPx = LiquidGlassConstants.FOCUS_BORDER_WIDTH_PX;
            this.focusBorderIntensity = LiquidGlassConstants.FOCUS_BORDER_INTENSITY;
            this.focusBorderSpeed = LiquidGlassConstants.FOCUS_BORDER_SPEED;
            this.init = true;
            return;
        }
        this.tintAlpha = Mth.lerp(a, this.tintAlpha, LiquidGlassConstants.DEFAULT_TINT_ALPHA);
        this.smoothing = Mth.lerp(a, this.smoothing, LiquidGlassConstants.DEFAULT_SMOOTHING);
        this.blurRadiusF = Mth.lerp(a, this.blurRadiusF, LiquidGlassConstants.DEFAULT_BLUR_RADIUS);
        this.shadowExpand = Mth.lerp(a, this.shadowExpand, LiquidGlassConstants.DEFAULT_SHADOW_EXPAND);
        this.shadowFactor = Mth.lerp(a, this.shadowFactor, LiquidGlassConstants.DEFAULT_SHADOW_FACTOR);
        this.shadowOffsetX = Mth.lerp(a, this.shadowOffsetX, LiquidGlassConstants.DEFAULT_SHADOW_OFFSET_X);
        this.shadowOffsetY = Mth.lerp(a, this.shadowOffsetY, LiquidGlassConstants.DEFAULT_SHADOW_OFFSET_Y);
        this.refStrength = Mth.lerp(a, this.refStrength, LiquidGlassConstants.DEFAULT_REF_STRENGTH);
        this.refThickness = Mth.lerp(a, this.refThickness, LiquidGlassConstants.DEFAULT_REF_THICKNESS);
        this.refFactor = Mth.lerp(a, this.refFactor, LiquidGlassConstants.DEFAULT_REF_FACTOR);
        this.refDispersion = Mth.lerp(a, this.refDispersion, LiquidGlassConstants.DEFAULT_REF_DISPERSION);
        this.refFresnelRange = Mth.lerp(a, this.refFresnelRange, LiquidGlassConstants.DEFAULT_REF_FRESNEL_RANGE);
        this.refFresnelHardness = Mth.lerp(a, this.refFresnelHardness, LiquidGlassConstants.DEFAULT_REF_FRESNEL_HARDNESS);
        this.refFresnelFactor = Mth.lerp(a, this.refFresnelFactor, LiquidGlassConstants.DEFAULT_REF_FRESNEL_FACTOR);
        this.glareRange = Mth.lerp(a, this.glareRange, LiquidGlassConstants.DEFAULT_GLARE_RANGE);
        this.glareHardness = Mth.lerp(a, this.glareHardness, LiquidGlassConstants.DEFAULT_GLARE_HARDNESS);
        this.glareConvergence = Mth.lerp(a, this.glareConvergence, LiquidGlassConstants.DEFAULT_GLARE_CONVERGENCE);
        this.glareOppositeFactor = Mth.lerp(a, this.glareOppositeFactor, LiquidGlassConstants.DEFAULT_GLARE_OPPOSITE_FACTOR);
        this.glareFactor = Mth.lerp(a, this.glareFactor, LiquidGlassConstants.DEFAULT_GLARE_FACTOR);
        this.glareAngleRad = Mth.lerp(a, this.glareAngleRad, LiquidGlassConstants.DEFAULT_GLARE_ANGLE_RAD);
        this.debugStep = Mth.lerp(a, this.debugStep, LiquidGlassConstants.DEBUG_STEP);
        this.pixelatedGridSize = Mth.lerp(a, this.pixelatedGridSize, LiquidGlassConstants.PIXELATED_GRID_SIZE);
        this.hoverScalePx = Mth.lerp(a, this.hoverScalePx, LiquidGlassConstants.HOVER_SCALE_PX);
        this.focusScalePx = Mth.lerp(a, this.focusScalePx, LiquidGlassConstants.FOCUS_SCALE_PX);
        this.focusBorderWidthPx = Mth.lerp(a, this.focusBorderWidthPx, LiquidGlassConstants.FOCUS_BORDER_WIDTH_PX);
        this.focusBorderIntensity = Mth.lerp(a, this.focusBorderIntensity, LiquidGlassConstants.FOCUS_BORDER_INTENSITY);
        this.focusBorderSpeed = Mth.lerp(a, this.focusBorderSpeed, LiquidGlassConstants.FOCUS_BORDER_SPEED);
    }

    private static float alpha(final double dt, final float tau) {
        if (dt <= 0) {
            return 0;
        }
        final double al = 1.0 - Math.exp(-dt / Math.max(1e-4, tau));
        return (float) al;
    }

    public float tintAlpha() {
        return this.tintAlpha;
    }

    public float smoothing() {
        return this.smoothing;
    }

    public int blurRadiusInt() {
        return Math.max(0, Math.round(this.blurRadiusF));
    }

    public float shadowExpand() {
        return this.shadowExpand;
    }

    public float shadowFactor() {
        return this.shadowFactor;
    }

    public float shadowOffsetX() {
        return this.shadowOffsetX;
    }

    public float shadowOffsetY() {
        return this.shadowOffsetY;
    }

    public float refStrength() {
        return this.refStrength;
    }

    public float refThickness() {
        return this.refThickness;
    }

    public float refFactor() {
        return this.refFactor;
    }

    public float refDispersion() {
        return this.refDispersion;
    }

    public float refFresnelRange() {
        return this.refFresnelRange;
    }

    public float refFresnelHardness() {
        return this.refFresnelHardness;
    }

    public float refFresnelFactor() {
        return this.refFresnelFactor;
    }

    public float glareRange() {
        return this.glareRange;
    }

    public float glareHardness() {
        return this.glareHardness;
    }

    public float glareConvergence() {
        return this.glareConvergence;
    }

    public float glareOppositeFactor() {
        return this.glareOppositeFactor;
    }

    public float glareFactor() {
        return this.glareFactor;
    }

    public float glareAngleRad() {
        return this.glareAngleRad;
    }

    public float debugStep() {
        return this.debugStep;
    }

    public float pixelatedGridSize() {
        return this.pixelatedGridSize;
    }

    public float hoverScalePx() {
        return this.hoverScalePx;
    }

    public float focusScalePx() {
        return this.focusScalePx;
    }

    public float focusBorderWidthPx() {
        return this.focusBorderWidthPx;
    }

    public float focusBorderIntensity() {
        return this.focusBorderIntensity;
    }

    public float focusBorderSpeed() {
        return this.focusBorderSpeed;
    }
}
