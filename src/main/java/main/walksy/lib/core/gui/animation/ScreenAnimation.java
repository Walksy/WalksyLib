package main.walksy.lib.core.gui.animation;

import main.walksy.lib.core.gui.style.UiStyle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public final class ScreenAnimation {
    private static final float OPEN_SPEED = 1.0f;
    private static final float CLOSE_SPEED = 1.5f;
    private float progress = 0f;
    private float target = 0f;
    private float speed = 1f;
    private boolean skipNextUpdate = false;
    private final Style style;
    private final Easing easing;

    public ScreenAnimation(final Style style, final Easing easing) {
        this.style = style;
        this.easing = easing;
    }

    public ScreenAnimation(final Style style) {
        this(style, Easing.EASE_OUT_QUAD);
    }

    public void reset() {
        this.progress = 0f;
        this.target = 0f;
        this.speed = OPEN_SPEED;
        this.skipNextUpdate = true;
    }

    public void open() {
        this.target = 1f;
        this.speed = OPEN_SPEED;
    }

    public void close() {
        this.target = 0f;
        this.speed = CLOSE_SPEED;
    }

    public boolean isClosing() {
        return this.target < 0.5f;
    }

    public boolean isDoneClosing() {
        return this.isClosing() && this.progress < 0.01f;
    }

    public float value() {
        return this.ease(Mth.clamp(this.progress, 0f, 1f));
    }

    public void update(final float delta) {
        if (UiStyle.minecraft()) {
            this.skipNextUpdate = false;
            this.progress = this.target;
            return;
        }
        if (this.skipNextUpdate) {
            this.skipNextUpdate = false;
            return;
        }
        final float t = 1.0f - (float) Math.exp(-this.speed * delta);
        this.progress = this.progress + (this.target - this.progress) * t;
    }

    public PanelState compute(final int tx, final int ty, final int tw, final int th) {
        final float raw = Mth.clamp(this.progress, 0f, 1f);
        final float t = this.ease(raw);
        int x = tx;
        int y = ty;
        int w = tw;
        int h = th;
        switch (this.style) {
            case SCALE -> {
                w = Math.max(2, Math.round(tw * t));
                h = Math.max(2, Math.round(th * t));
                x = tx + (tw - w) / 2;
                y = ty + (th - h) / 2;
            }
            case SLIDE_TOP -> {
                final int offset = Math.round(th * (1f - t));
                x = tx;
                y = ty - offset;
            }
            case SLIDE_BOTTOM -> {
                final int offset = Math.round(th * (1f - t));
                x = tx;
                y = ty + offset;
            }
            case SLIDE_LEFT -> {
                final int offset = Math.round(tw * (1f - t));
                x = tx - offset;
                y = ty;
            }
            case SLIDE_RIGHT -> {
                final int offset = Math.round(tw * (1f - t));
                x = tx + offset;
                y = ty;
            }
            case FLIP -> {
                h = Math.max(2, Math.round(th * t));
                x = tx;
                y = ty + (th - h) / 2;
            }
            case WIPE_HORIZONTAL -> {
                if (raw < 0.5f) {
                    final float pt = this.ease(raw * 2f);
                    w = Math.max(2, Math.round(tw * pt));
                    h = 2;
                    x = tx + (tw - w) / 2;
                    y = ty + (th - h) / 2;
                } else {
                    final float pt = this.ease((raw - 0.5f) * 2f);
                    w = tw;
                    h = Math.max(2, Math.round(th * pt));
                    x = tx;
                    y = ty + (th - h) / 2;
                }
            }
        }
        final float cornerRadius = Math.min(14f, 0.5f * Math.min(w, h));
        return new PanelState(x, y, w, h, cornerRadius);
    }

    public record PanelState(int x, int y, int w, int h, float cornerRadius) {
        @Override
        public @NotNull String toString() {
            return "";
        }
    }

    public static ScreenAnimation scale() {
        return new ScreenAnimation(Style.SCALE, Easing.EASE_OUT_QUAD);
    }

    public static ScreenAnimation scaleBounce() {
        return new ScreenAnimation(Style.SCALE, Easing.EASE_OUT_BACK);
    }

    public static ScreenAnimation scaleElastic() {
        return new ScreenAnimation(Style.SCALE, Easing.EASE_OUT_ELASTIC);
    }

    public static ScreenAnimation slideTop() {
        return new ScreenAnimation(Style.SLIDE_TOP, Easing.EASE_OUT_CUBIC);
    }

    public static ScreenAnimation slideBottom() {
        return new ScreenAnimation(Style.SLIDE_BOTTOM, Easing.EASE_OUT_CUBIC);
    }

    public static ScreenAnimation slideLeft() {
        return new ScreenAnimation(Style.SLIDE_LEFT, Easing.EASE_OUT_CUBIC);
    }

    public static ScreenAnimation slideRight() {
        return new ScreenAnimation(Style.SLIDE_RIGHT, Easing.EASE_OUT_CUBIC);
    }

    public static ScreenAnimation flip() {
        return new ScreenAnimation(Style.FLIP, Easing.EASE_OUT_QUAD);
    }

    public static ScreenAnimation wipeHorizontal() {
        return new ScreenAnimation(Style.WIPE_HORIZONTAL, Easing.EASE_OUT_QUAD);
    }

    private float ease(final float t) {
        return ease(this.easing, t);
    }

    public static float ease(final Easing easing, final float t) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_OUT_QUAD -> 1f - (1f - t) * (1f - t);
            case EASE_OUT_CUBIC -> 1f - (float) Math.pow(1f - t, 3);
            case EASE_OUT_BACK -> easeOutBack(t);
            case EASE_OUT_ELASTIC -> easeOutElastic(t);
            case EASE_OUT_BOUNCE -> easeOutBounce(t);
        };
    }

    private static float easeOutBack(final float t) {
        final float c1 = 1.70158f;
        final float c3 = c1 + 1f;
        final float s = t - 1f;
        return 1f + c3 * s * s * s + c1 * s * s;
    }

    private static float easeOutElastic(final float t) {
        if (t == 0f || t == 1f) {
            return t;
        }
        final float c4 = (float) (2.0 * Math.PI / 3.0);
        return (float) (Math.pow(2.0, -10.0 * t) * Math.sin((t * 10.0 - 0.75) * c4) + 1.0);
    }

    private static float easeOutBounce(float t) {
        final float n1 = 7.5625f;
        final float d1 = 2.75f;
        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        } else {
            t -= 2.625f / d1;
            return n1 * t * t + 0.984375f;
        }
    }

    public enum Style {

        SCALE,

        SLIDE_TOP,

        SLIDE_BOTTOM,

        SLIDE_LEFT,

        SLIDE_RIGHT,

        FLIP,

        WIPE_HORIZONTAL
    }

    public enum Easing {
        LINEAR,
        EASE_OUT_QUAD,
        EASE_OUT_CUBIC,

        EASE_OUT_BACK,

        EASE_OUT_ELASTIC,

        EASE_OUT_BOUNCE
    }
}
