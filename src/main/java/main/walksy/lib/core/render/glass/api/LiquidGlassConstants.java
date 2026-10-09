package main.walksy.lib.core.render.glass.api;

import main.walksy.lib.core.render.glass.api.model.RimLight;
import org.joml.Vector2f;

public final class LiquidGlassConstants {
    public static final boolean PIXELATED_GRID = false;
    public static final int DEFAULT_TINT_COLOR = 0x000000;
    public static final float DEFAULT_TINT_ALPHA = 0.0f;
    public static final float DEFAULT_SMOOTHING = 0.003f;
    public static final int DEFAULT_LAYER = 10;
    public static final int DEFAULT_BLUR_RADIUS = 16;
    public static final int BACKGROUND_BLUR_RADIUS = 16;
    public static final float DEFAULT_SHADOW_EXPAND = 9.1f;
    public static final float DEFAULT_SHADOW_FACTOR = 0.208f;
    public static final float DEFAULT_SHADOW_OFFSET_X = 0.0f;
    public static final float DEFAULT_SHADOW_OFFSET_Y = 2.0f;
    public static final int   DEFAULT_SHADOW_COLOR = 0x000000;
    public static final float DEFAULT_SHADOW_COLOR_ALPHA = 1.0f;
    public static final float DEFAULT_REF_STRENGTH = 1.0f;
    public static final float DEFAULT_REF_THICKNESS = 13.15f;
    public static final float DEFAULT_REF_FACTOR = 1.5f;
    public static final float DEFAULT_REF_DISPERSION = 7.0f;
    public static final float DEFAULT_REF_FRESNEL_RANGE = 8.0f;
    public static final float DEFAULT_REF_FRESNEL_HARDNESS = 6.0f;
    public static final float DEFAULT_REF_FRESNEL_FACTOR = 20.0f;
    public static final float DEFAULT_GLARE_RANGE = 6.0f;
    public static final float DEFAULT_GLARE_HARDNESS = 9.0f;
    public static final float DEFAULT_GLARE_CONVERGENCE = 50.0f;
    public static final float DEFAULT_GLARE_OPPOSITE_FACTOR = 80.0f;
    public static final float DEFAULT_GLARE_FACTOR = 90.0f;
    public static final float DEFAULT_GLARE_ANGLE_RAD = -0.785f;
    public static final RimLight RIM_LIGHT = new RimLight(new Vector2f(-1.0f, 1.0f).normalize(), 0xFFFFFF, 0.1f);
    public static final float PIXEL_EPSILON = 2.0f;
    public static final float DEBUG_STEP = 9.0f;
    public static final float PIXELATED_GRID_SIZE = 8.0f;
    public static final float HOVER_SCALE_PX = 1.53f;
    public static final float FOCUS_SCALE_PX = 2.5f;
    public static final float FOCUS_BORDER_WIDTH_PX = 0.75f;
    public static final float FOCUS_BORDER_INTENSITY = 0.60f;
    public static final float FOCUS_BORDER_SPEED = 1.6f;
    public static final float REFRACTION_STRENGTH_NONE = 0.0f;
    public static final float REFRACTION_STRENGTH_SUBTLE = 0.5f;
    public static final float REFRACTION_STRENGTH_DEFAULT = 1.0f;
    public static final float REFRACTION_STRENGTH_STRONG = 2.0f;
    public static final float REFRACTION_STRENGTH_MAX = 3.0f;
    public static final float REFRACTION_THICKNESS_THIN = 8.0f;
    public static final float REFRACTION_THICKNESS_DEFAULT = 13.15f;
    public static final float REFRACTION_THICKNESS_WIDE = 24.0f;
    public static final float REFRACTION_THICKNESS_DRAMATIC = 36.0f;
    public static final float CHROME_BRIGHT_EDGE = 0.45f;
    public static final float BRIGHT_EDGE_WIDTH_PX = 0.40f;
    public static final float KNOB_BRIGHT_EDGE = 0.45f;
    public static final int MAX_BLUR_RADIUS = 512;
    public static final float GLASS_SHADOW_ALPHA = 0.07f;
    public static final float GLASS_SHADOW_EXPAND = 1.25f;
    public static final float GLASS_SHADOW_OFFSET_Y = 3f;

    public static final float OVER_WORLD_TONE_FLOOR = 0.16f;
    public static final float OVER_WORLD_TONE_CEILING = 0.72f;

    public static final int BLUR_CLEAR = 0;
    public static final int BLUR_CRISP = 6;
    public static final int BLUR_FROSTED = 16;
    public static final int BLUR_HEAVY = 32;
    public static final int BLUR_MILKY = 80;
    public static final int BLUR_PANEL = 48;
    public static final int BLUR_CONTENT = 24;
    public static final int BLUR_POPOVER = 10;
    public static final int BLUR_DROPDOWN = 128;
    public static final int BLUR_OVERLAY = 256;

    private LiquidGlassConstants() {
    }
}
