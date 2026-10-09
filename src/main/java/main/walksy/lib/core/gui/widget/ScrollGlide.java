package main.walksy.lib.core.gui.widget;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.render.DevicePixels;

public final class ScrollGlide {
    private static final float DURATION_PER_RATE = 3f;
    private boolean active;
    private float from;
    private float to;
    private float duration;
    private float elapsed;
    private float lastOutput;

    public float step(final float current, final float target, final float authoredRate, final float dtSeconds) {
        final float rate = WalksyLib.getInstance().getAppearancePage().scrollGlideSpeed(authoredRate);
        if (rate <= 0f || current == target) {
            return this.settle(target);
        }
        if (!this.active || target != this.to || current != this.lastOutput) {
            final float carried = this.active && current == this.lastOutput ? this.velocity() : 0f;
            this.begin(current, target, carried, DURATION_PER_RATE / rate);
        }
        if (dtSeconds > 0f && Float.isFinite(dtSeconds)) {
            this.elapsed += dtSeconds;
        }
        if (this.elapsed >= this.duration) {
            return this.settle(target);
        }
        final float u = this.elapsed / this.duration;
        this.lastOutput = this.from + (this.to - this.from) * u * (2f - u);
        return this.lastOutput;
    }

    private void begin(final float current, final float target, final float carried, final float baseDuration) {
        final float distance = target - current;
        final boolean sameWay = carried != 0f && Math.signum(carried) == Math.signum(distance);
        final float fresh = 2f * distance / baseDuration;
        this.duration = sameWay && Math.abs(carried) > Math.abs(fresh) ? 2f * distance / carried : baseDuration;
        this.from = current;
        this.to = target;
        this.elapsed = 0f;
        this.lastOutput = current;
        this.active = true;
    }

    private float velocity() {
        if (!this.active || this.duration <= 0f) {
            return 0f;
        }
        final float u = Math.min(1f, this.elapsed / this.duration);
        return 2f * (this.to - this.from) / this.duration * (1f - u);
    }

    private float settle(final float target) {
        this.active = false;
        this.lastOutput = target;
        return target;
    }

    public static float shown(final float scrollY) {
        return DevicePixels.snapY(scrollY);
    }
}
