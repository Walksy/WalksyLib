package main.walksy.lib.core.render;

import com.terraformersmc.modmenu.config.ModMenuConfig;
import com.terraformersmc.modmenu.gui.widget.ModListWidget;
import com.terraformersmc.modmenu.util.mod.Mod;
import main.walksy.lib.core.gui.liquid.LiquidRect;
import main.walksy.lib.core.render.glass.api.GlassStyle;
import main.walksy.lib.core.render.glass.api.LiquidGlassConstants;

import java.awt.Color;

public final class ModListEntryContentExtractor {
    public static final GlassStyle STYLE = new GlassStyle()
        .layer(0)
        .blurRadius(LiquidGlassConstants.BLUR_CLEAR)
        .tint(0xFFFFFF, 0.05f)
        .refractionStrength(2f)
        .refractionThickness(9f)
        .refractionFactor(3f)
        .refractionDispersion(3f)
        .edgeDarken(Color.WHITE.getRGB(), 2f)
        .shadow(22f, 0.16f, 0f, 4f)
        .shadowColor(0x000000, 0.5f)
        .nativePixels();
    private static final int FULL_ICON_SIZE = 32;
    private static final int COMPACT_ICON_SIZE = 19;
    private static final int ICON_PADDING = 4;
    public static final float CORNER_RADIUS = 6f;
    private float opacity = 0f;
    private boolean settled = false;

    public ModListEntryContentExtractor(final Mod mod, final ModListWidget list) {
    }

    private static int iconSize() {
        return ModMenuConfig.COMPACT_LIST.getValue() ? COMPACT_ICON_SIZE : FULL_ICON_SIZE;
    }

    public static LiquidRect highlightRect(final int left, final int contentTop, final int right, final int bottom) {
        final int x = left - ICON_PADDING;
        final int y = contentTop - ICON_PADDING;
        final int collapsedSize = iconSize() + ICON_PADDING * 2;
        final int w = Math.max(collapsedSize, right - x);
        final int h = Math.max(collapsedSize, bottom - y);
        return new LiquidRect(x, y + 2, w, h - 4, CORNER_RADIUS);
    }

    public void extract(final WLGraphics2D drawContext, final int left, final int contentTop, final int right, final int bottom, final boolean hovered, final boolean selected, final float delta) {
        final float target = (selected || hovered) ? 1f : 0f;
        if (!this.settled) {
            this.settled = true;
            this.opacity = target;
        } else {
            final double dt = Math.min(0.05, WLRenderSystem.frameDeltaSeconds());
            this.opacity = smoothToward(this.opacity, target, dt, 0.07F);
        }
        if (this.opacity <= 0.001f) {
            return;
        }
        final LiquidRect r = highlightRect(left, contentTop, right, bottom);
        drawContext.glass()
            .dimensions(Math.round(r.x()), Math.round(r.y()), Math.round(r.w()), Math.round(r.h()))
            .cornerRadius(r.cornerRadius())
            .style(STYLE.copy().opacity(this.opacity))
            .extract();
    }

    private static float smoothToward(final float current, final float target, final double dt, final float tau) {
        if (tau <= 1e-5f) {
            return target;
        }
        final float a = (float) (1.0 - Math.exp(-Math.max(0.0, dt) / tau));
        final float v = current + (target - current) * a;
        return Math.abs(v - target) < 1e-4f ? target : v;
    }
}
