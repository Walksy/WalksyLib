package main.walksy.lib.core.utils;

import main.walksy.lib.core.gui.style.UiStyle;

public final class MathUtils {
    private MathUtils() {
    }

    public static float approach(final float current, final float target, final float speed, final float dt) {
        if (UiStyle.minecraft()) {
            return target;
        }
        if (dt <= 0f || !Float.isFinite(dt)) {
            return current;
        }
        final float k = 1f - (float) Math.exp(-speed * dt);
        return current + (target - current) * k;
    }
}
