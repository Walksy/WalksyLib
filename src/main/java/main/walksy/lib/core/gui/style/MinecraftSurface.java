package main.walksy.lib.core.gui.style;

import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.utils.ColorUtils;
import net.minecraft.util.Mth;

public final class MinecraftSurface {
    private static final float HIGHLIGHT_MIX = 0.35f;
    private static final float HIGHLIGHT_ALPHA = 0.12f;
    private static final float TINT_CEILING = 1.25f;

    private MinecraftSurface() {
    }

    public static GlassStyle of(final GlassStyle source, final boolean highlighted) {
        final GlassStyle s = blend(source, 1f);
        if (highlighted) {
            s.tint(ColorUtils.lerpColor(source.getTintColor(), 0xFFFFFF, HIGHLIGHT_MIX), Math.min(TINT_CEILING, source.getTintAlpha() + HIGHLIGHT_ALPHA));
        }
        return s;
    }

    public static GlassStyle blend(final GlassStyle source, final float amount) {
        final float t = Mth.clamp(amount, 0f, 1f);
        final float keep = 1f - t;
        return source.copy()
            .refractionStrength(source.getRefStrength() * keep)
            .refractionThickness(source.getRefThickness() * keep)
            .refractionFactor(Mth.lerp(t, source.getRefFactor(), 1f))
            .refractionDispersion(source.getRefDispersion() * keep)
            .refractionReach(source.getRefractionReach() * keep)
            .refractionShift(source.getRefractionShift() * keep)
            .bevel(source.getBevelHeight() * keep)
            .lensFold(source.getLensFold() * keep)
            .fresnelRange(source.getRefFresnelRange() * keep)
            .fresnelHardness(source.getRefFresnelHardness() * keep)
            .fresnelFactor(source.getRefFresnelFactor() * keep)
            .glareRange(source.getGlareRange() * keep)
            .glareHardness(source.getGlareHardness() * keep)
            .glareFactor(source.getGlareFactor() * keep)
            .edgeDarken(source.getEdgeDarken() * keep)
            .brightEdge(source.getBrightEdge() * keep)
            .innerShadow(source.getInnerShadow() * keep)
            .rimLight(source.getRimLightColor(), source.getRimLightIntensity() * keep)
            .smoothing(source.getSmoothing() * keep);
    }
}
