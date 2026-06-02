package main.walksy.lib.core.utils;

public class Scroller {
    public boolean active = true;
    private double value;
    private final double step;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    public Scroller(final double startValue, final double step) {
        this.value = startValue;
        this.step = step;
    }

    public void onScroll(final double amount) {
        if (this.active) {
            this.value -= amount * this.step;
            if (this.value < this.min) this.value = this.min;
            if (this.value > this.max) this.value = this.max;
        }
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(final double value) {
        this.value = Math.max(this.min, Math.min(this.max, value));
    }

    public void setBounds(final double min, final double max) {
        this.min = min;
        this.max = max;
        if (this.active) {
            this.value = this.clamp(this.value, min, max);
        }
    }

    private double clamp(final double val, final double min, final double max) {
        return Math.max(min, Math.min(max, val));
    }
}
