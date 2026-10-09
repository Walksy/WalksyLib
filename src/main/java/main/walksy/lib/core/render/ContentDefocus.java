package main.walksy.lib.core.render;

public final class ContentDefocus {
    private static final int RING_TAPS = 12;
    private static final float[] RING_DX = new float[RING_TAPS];
    private static final float[] RING_DY = new float[RING_TAPS];
    private static final float INNER_WEIGHT = 0.080f;
    private static final float OUTER_WEIGHT = 0.055f;
    private static final float RING_WEIGHT_SUM = 6f * INNER_WEIGHT + 6f * OUTER_WEIGHT;
    private static final float MIN_ACTIVE = 0.25f;
    private static final float CONCENTRATION_RANGE = 2.5f;
    private static final float[] STACK = new float[8];
    private int depth;
    private float radius;

    static {
        for (int i = 0; i < 6; i++) {
            final double inner = Math.toRadians(45.0 + i * 60.0);
            RING_DX[i] = (float) (Math.cos(inner) * 0.55);
            RING_DY[i] = (float) (Math.sin(inner) * 0.55);
            final double outer = Math.toRadians(15.0 + i * 60.0);
            RING_DX[6 + i] = (float) Math.cos(outer);
            RING_DY[6 + i] = (float) Math.sin(outer);
        }
    }

    public void push(final float radiusPx) {
        if (this.depth < STACK.length) {
            STACK[this.depth] = this.radius;
        }
        this.depth++;
        this.radius = Math.max(0f, radiusPx);
    }

    public void pop() {
        this.depth = Math.max(0, this.depth - 1);
        this.radius = this.depth < STACK.length ? STACK[this.depth] : 0f;
    }

    public boolean isActive() {
        return this.radius > MIN_ACTIVE;
    }

    public int tapCount() {
        return RING_TAPS + 1;
    }

    public float tapDx(final int i) {
        return i == 0 ? 0f : RING_DX[i - 1] * this.radius;
    }

    public float tapDy(final int i) {
        return i == 0 ? 0f : RING_DY[i - 1] * this.radius;
    }

    public int tapColor(final int color, final int i) {
        final float t = Math.min(1f, this.radius / CONCENTRATION_RANGE);
        final float w = i == 0 ? 1f - RING_WEIGHT_SUM * t : i <= 6 ? INNER_WEIGHT * t : OUTER_WEIGHT * t;
        final int a = Math.round(((color >>> 24) & 0xFF) * w);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
