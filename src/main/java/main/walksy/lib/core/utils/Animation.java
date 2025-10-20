package main.walksy.lib.core.utils;

import net.minecraft.util.math.MathHelper;

public class Animation {
    private float currentValue;
    private float targetValue;
    private float speed;

    public Animation(float initialValue, float speed) {
        this.currentValue = initialValue;
        this.targetValue = initialValue;
        this.speed = speed;
    }

    public void update(float delta, Runnable onAnimating) {
        float t = 1.0f - (float) Math.exp(-speed * delta);
        float newValue = MathHelper.lerp(t, currentValue, targetValue);

        if (Math.abs(newValue - targetValue) > 0.001f) {
            if (onAnimating != null) onAnimating.run();
        }

        currentValue = newValue;
    }

    public void update(float delta) {
        update(delta, null);
    }

    public void setTargetValue(float targetValue) {
        this.targetValue = targetValue;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public void jumpTo(float value) {
        this.currentValue = value;
        this.targetValue = value;
    }

    public boolean isAnimating() {
        return this.currentValue != this.targetValue;
    }

    public float getTargetValue() {
        return targetValue;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
