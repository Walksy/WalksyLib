package main.walksy.lib.core.utils;

import net.minecraft.util.Mth;

public class Animation {
    private float currentValue;
    private float targetValue;
    private float speed;

    public Animation(final float initialValue, final float speed) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.speed = speed;
    }

    public void update(final float delta, final Runnable onAnimating) {
        final float t = 1.0f - (float) Math.exp(-this.speed * delta);
        final float newValue = Mth.lerp(t, this.currentValue, this.targetValue);

        if (Math.abs(newValue - this.targetValue) > 0.001f) {
            if (onAnimating != null) {
                onAnimating.run();
            }
        }

        this.currentValue = newValue;
    }

    public void update(final float delta) {
        this.update(delta, null);
    }

    public void setTargetValue(final float targetValue) {
        this.targetValue = targetValue;
    }

    public float getCurrentValue() {
        return this.currentValue;
    }

    public void jumpTo(final float value) {
        this.currentValue = value;
        this.targetValue = value;
    }

    public boolean isAnimating() {
        return Math.round(this.currentValue) != Math.round(this.targetValue);
    }

    public float getTargetValue() {
        return this.targetValue;
    }

    public void setSpeed(final float speed) {
        this.speed = speed;
    }
}
