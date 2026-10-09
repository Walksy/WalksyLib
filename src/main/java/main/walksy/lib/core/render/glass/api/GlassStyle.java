package main.walksy.lib.core.render.glass.api;

import main.walksy.lib.core.utils.ColorUtils;
import main.walksy.lib.core.render.DevicePixels;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.gui.style.Theme;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fc;

public final class GlassStyle {
    public static final GlassStyle INACTIVE = new GlassStyle().tint(Theme.get().flatSurface(), 0.3f);
    private static final float INACTIVE_FILL_ALPHA = 0.3f;
    private boolean hasTint;
    private int tintColor;
    private float tintAlpha;
    private boolean hasSmoothing;
    private float smoothingFactor;
    private boolean hasBlurRadius;
    private int blurRadius;
    private boolean blurFalloff;
    private boolean hasLayer;
    private int layer;
    private boolean hasFadeKey;
    private long fadeKey;
    private boolean hasShadowExpand;
    private float shadowExpand;
    private boolean hasShadowFactor;
    private float shadowFactor;
    private boolean hasShadowOffset;
    private float shadowOffsetX;
    private float shadowOffsetY;
    private boolean hasShadowColor;
    private int shadowColor;
    private float shadowColorAlpha;
    private boolean hasRefStrength;
    private float refStrength;
    private boolean hasRefThickness;
    private float refThickness;
    private boolean hasRefFactor;
    private float refFactor;
    private boolean hasRefDispersion;
    private float refDispersion;
    private boolean hasRefFresnelRange;
    private float refFresnelRange;
    private boolean hasRefFresnelHardness;
    private float refFresnelHardness;
    private boolean hasRefFresnelFactor;
    private float refFresnelFactor;
    private boolean hasGlareRange;
    private float glareRange;
    private boolean hasGlareHardness;
    private float glareHardness;
    private boolean hasGlareConvergence;
    private float glareConvergence;
    private boolean hasGlareOppositeFactor;
    private float glareOppositeFactor;
    private boolean hasGlareFactor;
    private float glareFactor;
    private boolean hasGlareAngleRad;
    private float glareAngleRad;
    private boolean hasEdgeDarken;
    private float edgeDarken;
    private int edgeDarkenColor = 0x000000;
    private boolean hasBrightEdge;
    private float brightEdgeStrength = 1f;
    private boolean hasRimLight;
    private float rimLightIntensity;
    private int rimLightColor = 0xFFFFFF;
    private float opacity = 1f;
    private float refractionReach = 0f;
    private float bevelHeight = 0f;
    private float refractionShift = 0f;
    private float innerShadow = 0f;
    private float lensFold = 0f;
    private boolean bakesText = true;
    private boolean softLens;
    private float textScale = 1f;
    private float textRefract = 1f;
    private float toneFloor = 0f;
    private float toneCeiling = 0f;
    private float chroma = 1f;
    private boolean scaleApplied;
    private static final float HOVER_DARKEN_MIX = 0.5f;
    private static final float HOVER_DARKEN_ALPHA = 0.02f;
    private static final float HOVER_REFERENCE_ALPHA = 0.175f;
    private static final float TINT_ALPHA_CEILING = 1.25f;

    public static GlassStyle create() {
        return new GlassStyle();
    }

    public static GlassStyle lerp(final GlassStyle a, final GlassStyle b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        final GlassStyle s = new GlassStyle();
        s.scaleApplied = a.scaleApplied && b.scaleApplied;
        s.tint(lerpColor(a.getTintColor(), b.getTintColor(), t), Mth.lerp(t, a.getTintAlpha(), b.getTintAlpha()));
        s.smoothing(Mth.lerp(t, a.getSmoothing(), b.getSmoothing()));
        s.blurRadius(Math.round(Mth.lerp(t, a.getBlurRadius(), b.getBlurRadius())));
        s.blurFalloff(t < 0.5f ? a.hasBlurFalloff() : b.hasBlurFalloff());
        s.layer(t < 0.5f ? a.getLayer() : b.getLayer());
        final float ea = a.getShadowExpand();
        final float eb = b.getShadowExpand();
        final float fa = a.getShadowFactor();
        final float fb = b.getShadowFactor();
        final boolean modeSwap = (ea < 0f) != (eb < 0f) || (fa < 0f) != (fb < 0f);
        final float expand = (ea < 0f) == (eb < 0f) ? Mth.lerp(t, ea, eb) : (t < 0.5f ? ea : eb);
        final float factor = (fa < 0f) == (fb < 0f) ? Mth.lerp(t, fa, fb) : (t < 0.5f ? fa : fb);
        s.shadow(expand, factor, Mth.lerp(t, a.getShadowOffsetX(), b.getShadowOffsetX()), Mth.lerp(t, a.getShadowOffsetY(), b.getShadowOffsetY()));
        float shadowAlpha = Mth.lerp(t, a.getShadowColorAlpha(), b.getShadowColorAlpha());
        if (modeSwap) {
            shadowAlpha *= Math.abs(2f * t - 1f);
        }
        s.shadowColor(lerpColor(a.getShadowColor(), b.getShadowColor(), t), shadowAlpha);
        s.refractionStrength(Mth.lerp(t, a.getRefStrength(), b.getRefStrength()));
        s.refractionThickness(Mth.lerp(t, a.getRefThickness(), b.getRefThickness()));
        s.refractionFactor(Mth.lerp(t, a.getRefFactor(), b.getRefFactor()));
        s.refractionDispersion(Mth.lerp(t, a.getRefDispersion(), b.getRefDispersion()));
        s.fresnelRange(Mth.lerp(t, a.getRefFresnelRange(), b.getRefFresnelRange()));
        s.fresnelHardness(Mth.lerp(t, a.getRefFresnelHardness(), b.getRefFresnelHardness()));
        s.fresnelFactor(Mth.lerp(t, a.getRefFresnelFactor(), b.getRefFresnelFactor()));
        s.glareRange(Mth.lerp(t, a.getGlareRange(), b.getGlareRange()));
        s.glareHardness(Mth.lerp(t, a.getGlareHardness(), b.getGlareHardness()));
        s.glareConvergence(Mth.lerp(t, a.getGlareConvergence(), b.getGlareConvergence()));
        s.glareOppositeFactor(Mth.lerp(t, a.getGlareOppositeFactor(), b.getGlareOppositeFactor()));
        s.glareFactor(Mth.lerp(t, a.getGlareFactor(), b.getGlareFactor()));
        s.glareAngleRad(Mth.lerp(t, a.getGlareAngleRad(), b.getGlareAngleRad()));
        s.hasEdgeDarken = true;
        s.edgeDarkenColor = lerpColor(a.getEdgeDarkenColor(), b.getEdgeDarkenColor(), t);
        s.edgeDarken = Mth.lerp(t, a.getEdgeDarken(), b.getEdgeDarken());
        s.brightEdge(Mth.lerp(t, a.getBrightEdge(), b.getBrightEdge()));
        s.rimLight(lerpColor(a.getRimLightColor(), b.getRimLightColor(), t), Mth.lerp(t, a.getRimLightIntensity(), b.getRimLightIntensity()));
        s.opacity(Mth.lerp(t, a.getOpacity(), b.getOpacity()));
        s.refractionReach(Mth.lerp(t, a.getRefractionReach(), b.getRefractionReach()));
        s.bevel(Mth.lerp(t, a.getBevelHeight(), b.getBevelHeight()));
        s.refractionShift(Mth.lerp(t, a.getRefractionShift(), b.getRefractionShift()));
        s.innerShadow(Mth.lerp(t, a.getInnerShadow(), b.getInnerShadow()));
        s.lensFold(Mth.lerp(t, a.getLensFold(), b.getLensFold()));
        s.bakesText(t < 0.5f ? a.getBakesText() : b.getBakesText());
        s.softLens(t < 0.5f ? a.isSoftLens() : b.isSoftLens());
        s.textScale(Mth.lerp(t, a.getTextScale(), b.getTextScale()));
        s.textRefract(Mth.lerp(t, a.getTextRefract(), b.getTextRefract()));
        s.toneRange(Mth.lerp(t, a.getToneFloor(), b.getToneFloor()), Mth.lerp(t, a.getToneCeiling(), b.getToneCeiling()));
        s.chroma(Mth.lerp(t, a.getChroma(), b.getChroma()));
        return s;
    }

    private static int lerpColor(final int a, final int b, final float t) {
        return ColorUtils.lerpColor(a, b, t) & 0x00FFFFFF;
    }

    public GlassStyle flat() {
        return this.refractionStrength(0f)
            .refractionThickness(0f)
            .refractionFactor(1f)
            .refractionDispersion(0f)
            .refractionReach(0f)
            .refractionShift(0f)
            .bevel(0f)
            .lensFold(0f)
            .fresnelRange(0f)
            .fresnelHardness(0f)
            .fresnelFactor(0f)
            .glareRange(0f)
            .glareHardness(0f)
            .glareFactor(0f)
            .edgeDarken(0f)
            .brightEdge(0f)
            .innerShadow(0f)
            .rimLight(Theme.get().rimLightColor(), 0f)
            .shadow(0f, 0f, 0f, 0f)
            .shadowColor(Theme.get().shadowColor(), 0f);
    }

    public static GlassStyle inactive(final GlassStyle source) {
        if (source == null) {
            return INACTIVE.copy();
        }
        final GlassStyle flat = source.copy().flat();
        if (flat.getTintAlpha() <= 0.004f) {
            flat.tint(Theme.get().flatSurface(), INACTIVE_FILL_ALPHA);
        }
        return flat;
    }

    public GlassStyle copy() {
        final GlassStyle s = new GlassStyle();
        s.hasTint = this.hasTint; s.tintColor = this.tintColor; s.tintAlpha = this.tintAlpha;
        s.hasSmoothing = this.hasSmoothing; s.smoothingFactor = this.smoothingFactor;
        s.hasBlurRadius = this.hasBlurRadius; s.blurRadius = this.blurRadius;
        s.blurFalloff = this.blurFalloff;
        s.hasLayer = this.hasLayer; s.layer = this.layer;
        s.hasFadeKey = this.hasFadeKey; s.fadeKey = this.fadeKey;
        s.hasShadowExpand = this.hasShadowExpand; s.shadowExpand = this.shadowExpand;
        s.hasShadowFactor = this.hasShadowFactor; s.shadowFactor = this.shadowFactor;
        s.hasShadowOffset = this.hasShadowOffset; s.shadowOffsetX = this.shadowOffsetX; s.shadowOffsetY = this.shadowOffsetY;
        s.hasShadowColor = this.hasShadowColor; s.shadowColor = this.shadowColor; s.shadowColorAlpha = this.shadowColorAlpha;
        s.hasRefStrength = this.hasRefStrength; s.refStrength = this.refStrength;
        s.hasRefThickness = this.hasRefThickness; s.refThickness = this.refThickness;
        s.hasRefFactor = this.hasRefFactor; s.refFactor = this.refFactor;
        s.hasRefDispersion = this.hasRefDispersion; s.refDispersion = this.refDispersion;
        s.hasRefFresnelRange = this.hasRefFresnelRange; s.refFresnelRange = this.refFresnelRange;
        s.hasRefFresnelHardness = this.hasRefFresnelHardness; s.refFresnelHardness = this.refFresnelHardness;
        s.hasRefFresnelFactor = this.hasRefFresnelFactor; s.refFresnelFactor = this.refFresnelFactor;
        s.hasGlareRange = this.hasGlareRange; s.glareRange = this.glareRange;
        s.hasGlareHardness = this.hasGlareHardness; s.glareHardness = this.glareHardness;
        s.hasGlareConvergence = this.hasGlareConvergence; s.glareConvergence = this.glareConvergence;
        s.hasGlareOppositeFactor = this.hasGlareOppositeFactor; s.glareOppositeFactor = this.glareOppositeFactor;
        s.hasGlareFactor = this.hasGlareFactor; s.glareFactor = this.glareFactor;
        s.hasGlareAngleRad = this.hasGlareAngleRad; s.glareAngleRad = this.glareAngleRad;
        s.hasEdgeDarken = this.hasEdgeDarken; s.edgeDarken = this.edgeDarken; s.edgeDarkenColor = this.edgeDarkenColor;
        s.hasBrightEdge = this.hasBrightEdge; s.brightEdgeStrength = this.brightEdgeStrength;
        s.hasRimLight = this.hasRimLight; s.rimLightIntensity = this.rimLightIntensity; s.rimLightColor = this.rimLightColor;
        s.scaleApplied = this.scaleApplied;
        s.opacity = this.opacity;
        s.refractionReach = this.refractionReach;
        s.bevelHeight = this.bevelHeight;
        s.refractionShift = this.refractionShift;
        s.innerShadow = this.innerShadow;
        s.lensFold = this.lensFold;
        s.bakesText = this.bakesText;
        s.softLens = this.softLens;
        s.textScale = this.textScale;
        s.textRefract = this.textRefract;
        s.toneFloor = this.toneFloor;
        s.toneCeiling = this.toneCeiling;
        s.chroma = this.chroma;
        return s;
    }

    public boolean isScaleResolved() {
        return this.scaleApplied;
    }

    public GlassStyle nativePixels() {
        this.scaleApplied = true;
        return this;
    }

    public GlassStyle scaled(final float k) {
        final GlassStyle s = this.copy();
        s.scaleApplied = true;
        s.refractionThickness(this.getRefThickness() * k);
        s.refractionShift(this.getRefractionShift() * k);
        s.bevel(this.getBevelHeight() * k);
        s.fresnelRange(this.getRefFresnelRange() * k);
        s.glareRange(this.getGlareRange() * k);
        final float expand = this.getShadowExpand();
        s.shadowRadius(expand < 0f ? expand : expand * k);
        s.shadowOffset(this.getShadowOffsetX() * k, this.getShadowOffsetY() * k);
        return s;
    }

    public GlassStyle hoverDarken(final float hover) {
        if (hover <= 0.001f) {
            return this;
        }
        final float k = Math.min(1f, hover);
        final float alpha = this.getTintAlpha();
        return this.copy().tint(towardBlack(this.getTintColor(), mixFor(alpha) * k), Math.min(TINT_ALPHA_CEILING, alpha + HOVER_DARKEN_ALPHA * k));
    }

    private static float mixFor(final float tintAlpha) {
        return tintAlpha <= HOVER_REFERENCE_ALPHA ? HOVER_DARKEN_MIX : HOVER_DARKEN_MIX * (HOVER_REFERENCE_ALPHA / tintAlpha);
    }

    private static int towardBlack(final int rgb, final float t) {
        final int r = Math.round(((rgb >> 16) & 0xFF) * (1f - t));
        final int g = Math.round(((rgb >> 8) & 0xFF) * (1f - t));
        final int b = Math.round((rgb & 0xFF) * (1f - t));
        return (rgb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public GlassStyle tint(final int color, final float alpha) {
        this.hasTint = true;
        this.tintColor = color;
        this.tintAlpha = alpha;
        return this;
    }

    public GlassStyle smoothing(final float factor) {
        this.hasSmoothing = true;
        this.smoothingFactor = factor;
        return this;
    }

    public GlassStyle layer(final int v) {
        this.hasLayer = true;
        this.layer = v;
        return this;
    }

    public GlassStyle fadeKey(final long key) {
        this.hasFadeKey = true;
        this.fadeKey = key;
        return this;
    }

    public GlassStyle blurRadius(final int radius) {
        this.hasBlurRadius = true;
        this.blurRadius = Math.max(0, radius);
        return this;
    }

    public GlassStyle blurFalloff(final boolean falloff) {
        this.blurFalloff = falloff;
        return this;
    }

    public GlassStyle shadow(final float expand, final float factor, final float offsetX, final float offsetY) {
        this.hasShadowExpand = true;
        this.shadowExpand = expand;
        this.hasShadowFactor = true;
        this.shadowFactor = factor;
        this.hasShadowOffset = true;
        this.shadowOffsetX = offsetX;
        this.shadowOffsetY = offsetY;
        return this;
    }

    public GlassStyle shadowRadius(final float expand) {
        this.hasShadowExpand = true;
        this.shadowExpand = expand;
        return this;
    }

    public GlassStyle shadowFactor(final float factor) {
        this.hasShadowFactor = true;
        this.shadowFactor = factor;
        return this;
    }

    public GlassStyle shadowOffset(final float offsetX, final float offsetY) {
        this.hasShadowOffset = true;
        this.shadowOffsetX = offsetX;
        this.shadowOffsetY = offsetY;
        return this;
    }

    public GlassStyle outline(final float widthPx, final int color, final float alpha) {
        return this.outlineWithShadow(widthPx, color, alpha, 0f, 0f, 0f);
    }

    public GlassStyle outlineDirectional(final float widthPx, final int flankColor, final float outlineAlpha, final float shadowPeak, final float shadowExpand, final float shadowOffsetY) {
        this.outlineWithShadow(widthPx, flankColor, outlineAlpha, shadowPeak, shadowExpand, shadowOffsetY);
        this.shadowFactor = -Math.abs(this.shadowFactor);
        return this;
    }

    public GlassStyle outlineWithShadow(final float widthPx, final int color, final float outlineAlpha, final float shadowPeak, final float shadowExpand, final float shadowOffsetY) {
        this.hasShadowExpand = true;
        this.shadowExpand = -Math.max(0.05f, widthPx);
        this.hasShadowFactor = true;
        final float a = Math.max(1e-3f, outlineAlpha);
        this.shadowFactor = shadowExpand > 0f ? shadowPeak / (a * 0.6f) : 1f;
        this.hasShadowOffset = true;
        this.shadowOffsetX = Math.max(0f, shadowExpand);
        this.shadowOffsetY = shadowOffsetY;
        return this.shadowColor(color, outlineAlpha);
    }

    public GlassStyle shadowColor(final int color, final float alpha) {
        this.hasShadowColor = true;
        this.shadowColor = color;
        this.shadowColorAlpha = alpha;
        return this;
    }

    public GlassStyle refractionStrength(final float v) {
        this.hasRefStrength = true;
        this.refStrength = Math.max(0f, v);
        return this;
    }

    public GlassStyle refractionThickness(final float v) {
        this.hasRefThickness = true;
        this.refThickness = v;
        return this;
    }

    public GlassStyle refractionFactor(final float v) {
        this.hasRefFactor = true;
        this.refFactor = v;
        return this;
    }

    public GlassStyle refractionDispersion(final float v) {
        this.hasRefDispersion = true;
        this.refDispersion = v;
        return this;
    }

    public GlassStyle fresnelRange(final float v) {
        this.hasRefFresnelRange = true;
        this.refFresnelRange = v;
        return this;
    }

    public GlassStyle fresnelHardness(final float v) {
        this.hasRefFresnelHardness = true;
        this.refFresnelHardness = v;
        return this;
    }

    public GlassStyle fresnelFactor(final float v) {
        this.hasRefFresnelFactor = true;
        this.refFresnelFactor = v;
        return this;
    }

    public GlassStyle glareRange(final float v) {
        this.hasGlareRange = true;
        this.glareRange = v;
        return this;
    }

    public GlassStyle glareHardness(final float v) {
        this.hasGlareHardness = true;
        this.glareHardness = v;
        return this;
    }

    public GlassStyle glareConvergence(final float v) {
        this.hasGlareConvergence = true;
        this.glareConvergence = v;
        return this;
    }

    public GlassStyle glareOppositeFactor(final float v) {
        this.hasGlareOppositeFactor = true;
        this.glareOppositeFactor = v;
        return this;
    }

    public GlassStyle glareFactor(final float v) {
        this.hasGlareFactor = true;
        this.glareFactor = v;
        return this;
    }

    public GlassStyle glareAngleRad(final float v) {
        this.hasGlareAngleRad = true;
        this.glareAngleRad = v;
        return this;
    }

    public GlassStyle edgeDarken(final float v) {
        this.hasEdgeDarken = true;
        this.edgeDarken = Math.max(0f, Math.min(1f, v));
        return this;
    }

    public GlassStyle edgeDarken(final int color, final float amount) {
        this.hasEdgeDarken = true;
        this.edgeDarkenColor = color;
        this.edgeDarken = Math.max(0f, Math.min(1f, amount));
        return this;
    }

    public GlassStyle sideDarken(final int color, final float amount) {
        this.hasEdgeDarken = true;
        this.edgeDarkenColor = color;
        this.edgeDarken = -Math.max(0f, Math.min(1f, amount));
        return this;
    }

    public GlassStyle brightEdge(final float v) {
        this.hasBrightEdge = true;
        this.brightEdgeStrength = Math.max(0f, Math.min(1f, v));
        return this;
    }

    public float getBrightEdge() {
        return this.hasBrightEdge ? this.brightEdgeStrength : 1f;
    }

    public GlassStyle rimLight(final int color, final float intensity) {
        this.hasRimLight = true;
        this.rimLightColor = color;
        this.rimLightIntensity = Math.max(0f, intensity);
        return this;
    }

    public GlassStyle opacity(final float v) {
        this.opacity = Math.max(0f, Math.min(1f, v));
        return this;
    }

    public float getOpacity() {
        return this.opacity;
    }

    public GlassStyle refractionReach(final float v) {
        this.refractionReach = Math.max(-1f, Math.min(1f, v));
        return this;
    }

    public float getRefractionReach() {
        return this.refractionReach;
    }

    public GlassStyle bevel(final float px) {
        this.bevelHeight = Math.max(0f, px);
        return this;
    }

    public float getBevelHeight() {
        return this.bevelHeight;
    }

    public GlassStyle refractionShift(final float px) {
        this.refractionShift = px;
        return this;
    }

    public float getRefractionShift() {
        return this.refractionShift;
    }

    public GlassStyle standardShadow() {
        return this.standardShadowDirectional(Theme.get().outlineWidth(), Theme.get().shadowColor(), Theme.get().outlineAlpha());
    }

    public GlassStyle standardShadowDirectional(final float widthPx, final int flankColor, final float outlineAlpha) {
        return this.innerShadow(Theme.get().innerShadow()).outlineDirectional(widthPx, flankColor, outlineAlpha, LiquidGlassConstants.GLASS_SHADOW_ALPHA, LiquidGlassConstants.GLASS_SHADOW_EXPAND, LiquidGlassConstants.GLASS_SHADOW_OFFSET_Y);
    }

    public GlassStyle innerShadow(final float amount) {
        this.innerShadow = Math.max(0f, Math.min(4f, amount));
        return this;
    }

    public float getInnerShadow() {
        return this.innerShadow;
    }

    public GlassStyle lensFold(final float amount) {
        this.lensFold = Math.max(0f, Math.min(1f, amount));
        return this;
    }

    public float getLensFold() {
        return this.lensFold;
    }

    public GlassStyle bakesText(final boolean value) {
        this.bakesText = value;
        return this;
    }

    public boolean getBakesText() {
        return this.bakesText;
    }

    public GlassStyle softLens(final boolean value) {
        this.softLens = value;
        return this;
    }

    public boolean isSoftLens() {
        return this.softLens;
    }

    public static float centreLensBand(final Matrix3x2fc pose, final float height, final float growPx) {
        return height / 2f * Math.abs(pose.m11()) + growPx / DevicePixels.perLogicalY();
    }

    public GlassStyle textScale(final float value) {
        this.textScale = Math.max(0.05f, Math.min(4f, value));
        return this;
    }

    public float getTextScale() {
        return this.textScale;
    }

    public GlassStyle textRefract(final float value) {
        this.textRefract = Math.max(0f, Math.min(1f, value));
        return this;
    }

    public float getTextRefract() {
        return this.textRefract;
    }

    public GlassStyle toneRange(final float lo, final float hi) {
        this.toneFloor = Math.max(0f, Math.min(1f, lo));
        this.toneCeiling = Math.max(0f, Math.min(1f, hi));
        return this;
    }

    public float getToneFloor() {
        return this.toneFloor;
    }

    public float getToneCeiling() {
        return this.toneCeiling;
    }

    public GlassStyle chroma(final float k) {
        this.chroma = Math.max(0f, Math.min(1f, k));
        return this;
    }

    public float getChroma() {
        return this.chroma;
    }

    public int getTintColor() {
        return this.hasTint ? this.tintColor : LiquidGlassConstants.DEFAULT_TINT_COLOR;
    }

    public float getTintAlpha() {
        return this.hasTint ? this.tintAlpha : WLRenderSystem.glassAnim().tintAlpha();
    }

    public float getSmoothing() {
        return this.hasSmoothing ? this.smoothingFactor : WLRenderSystem.glassAnim().smoothing();
    }

    public int getLayer() {
        return this.hasLayer ? this.layer : LiquidGlassConstants.DEFAULT_LAYER;
    }

    public boolean hasFadeKey() {
        return this.hasFadeKey;
    }

    public long getFadeKey() {
        return this.fadeKey;
    }

    public int getBlurRadius() {
        return this.hasBlurRadius ? this.blurRadius : WLRenderSystem.glassAnim().blurRadiusInt();
    }

    public boolean hasBlurFalloff() {
        return this.blurFalloff;
    }

    public float getShadowExpand() {
        return this.hasShadowExpand ? this.shadowExpand : WLRenderSystem.glassAnim().shadowExpand();
    }

    public float getShadowFactor() {
        return this.hasShadowFactor ? this.shadowFactor : WLRenderSystem.glassAnim().shadowFactor();
    }

    public float getShadowOffsetX() {
        return this.hasShadowOffset ? this.shadowOffsetX : WLRenderSystem.glassAnim().shadowOffsetX();
    }

    public float getShadowOffsetY() {
        return this.hasShadowOffset ? this.shadowOffsetY : WLRenderSystem.glassAnim().shadowOffsetY();
    }

    public int getShadowColor() {
        return this.hasShadowColor ? this.shadowColor : LiquidGlassConstants.DEFAULT_SHADOW_COLOR;
    }

    public float getShadowColorAlpha() {
        return this.hasShadowColor ? this.shadowColorAlpha : LiquidGlassConstants.DEFAULT_SHADOW_COLOR_ALPHA;
    }

    public float getRefStrength() {
        return this.hasRefStrength ? this.refStrength : WLRenderSystem.glassAnim().refStrength();
    }

    public float getRefThickness() {
        return this.hasRefThickness ? this.refThickness : WLRenderSystem.glassAnim().refThickness();
    }

    public float getRefFactor() {
        return this.hasRefFactor ? this.refFactor : WLRenderSystem.glassAnim().refFactor();
    }

    public float getRefDispersion() {
        return this.hasRefDispersion ? this.refDispersion : WLRenderSystem.glassAnim().refDispersion();
    }

    public float getRefFresnelRange() {
        return this.hasRefFresnelRange ? this.refFresnelRange : WLRenderSystem.glassAnim().refFresnelRange();
    }

    public float getRefFresnelHardness() {
        return this.hasRefFresnelHardness ? this.refFresnelHardness : WLRenderSystem.glassAnim().refFresnelHardness();
    }

    public float getRefFresnelFactor() {
        return this.hasRefFresnelFactor ? this.refFresnelFactor : WLRenderSystem.glassAnim().refFresnelFactor();
    }

    public float getGlareRange() {
        return this.hasGlareRange ? this.glareRange : WLRenderSystem.glassAnim().glareRange();
    }

    public float getGlareHardness() {
        return this.hasGlareHardness ? this.glareHardness : WLRenderSystem.glassAnim().glareHardness();
    }

    public float getGlareConvergence() {
        return this.hasGlareConvergence ? this.glareConvergence : WLRenderSystem.glassAnim().glareConvergence();
    }

    public float getGlareOppositeFactor() {
        return this.hasGlareOppositeFactor ? this.glareOppositeFactor : WLRenderSystem.glassAnim().glareOppositeFactor();
    }

    public float getGlareFactor() {
        return this.hasGlareFactor ? this.glareFactor : WLRenderSystem.glassAnim().glareFactor();
    }

    public float getGlareAngleRad() {
        return this.hasGlareAngleRad ? this.glareAngleRad : WLRenderSystem.glassAnim().glareAngleRad();
    }

    public boolean hasDirectionalGlare() {
        return this.hasGlareFactor;
    }

    public float getEdgeDarken() {
        return this.hasEdgeDarken ? this.edgeDarken : 0f;
    }

    public int getEdgeDarkenColor() {
        return this.edgeDarkenColor;
    }

    public float getRimLightIntensity() {
        return this.hasRimLight ? this.rimLightIntensity : 0f;
    }

    public int getRimLightColor() {
        return this.rimLightColor;
    }
}
