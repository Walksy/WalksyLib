package main.walksy.lib.core.gui.liquid;

import main.walksy.lib.core.gui.animation.ScreenAnimation;

public final class LiquidProfile {
    public float restSmoothing = 0.003f;
    public float meltSmoothing = 0.040f;
    public int droplets = 2;
    public float dropletSpread = 0.55f;
    public float dropletSize = 0.34f;
    public float wobble = 0.10f;
    public int wobbleCycles = 2;
    public float bridge = 0.42f;
    public float seedSize = 16f;
    public float roundnessSwell = 0f;
    public float anchorResidue = 0f;
    public ScreenAnimation.Easing easing = ScreenAnimation.Easing.EASE_OUT_BACK;

    public static LiquidProfile smooth() {
        final LiquidProfile p = new LiquidProfile();
        p.droplets = 0;
        p.wobble = 0f;
        p.meltSmoothing = 0.030f;
        p.bridge = 0.50f;
        p.easing = ScreenAnimation.Easing.EASE_OUT_CUBIC;
        return p;
    }

}
