package main.walksy.lib.core.utils;

public class Scroller {
    public boolean active = true;
    private double value;
    private final double step;
    private double min = Double.NEGATIVE_INFINITY;
    private double max = Double.POSITIVE_INFINITY;

    public Scroller(double startValue, double step) {
        this.value = startValue;
        this.step = step;
    }

    public void onScroll(double amount) {
        if (active) {
            value -= amount * step;
            if (value < min) value = min;
            if (value > max) value = max;
        }
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public void setBounds(double min, double max) {
        this.min = min;
        this.max = max;
        if (active) {
            this.value = clamp(this.value, min, max);
        }
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}
