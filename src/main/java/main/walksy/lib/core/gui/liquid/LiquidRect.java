package main.walksy.lib.core.gui.liquid;

public record LiquidRect(float x, float y, float w, float h, float cornerRadius) {

    public float centerX() {
        return this.x + 0.5f * this.w;
    }

    public float centerY() {
        return this.y + 0.5f * this.h;
    }

    public float clampedRadius() {
        return Math.min(this.cornerRadius, 0.5f * Math.min(this.w, this.h));
    }

    public static LiquidRect of(final int x, final int y, final int w, final int h, final float cornerRadius) {
        return new LiquidRect(x, y, w, h, cornerRadius);
    }

    public static LiquidRect lerp(final LiquidRect a, final LiquidRect b, final float t) {
        return new LiquidRect(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.w + (b.w - a.w) * t, a.h + (b.h - a.h) * t, a.cornerRadius + (b.cornerRadius - a.cornerRadius) * t);
    }

    public static LiquidRect seed(final LiquidRect panel, final float size) {
        final float s = Math.max(2f, size);
        return new LiquidRect(panel.centerX() - 0.5f * s, panel.centerY() - 0.5f * s, s, s, 0.5f * s);
    }

    public static LiquidRect circle(final float cx, final float cy, final float d) {
        return new LiquidRect(cx - 0.5f * d, cy - 0.5f * d, d, d, 0.5f * d);
    }
}
