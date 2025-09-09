package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class DoubleSliderAdapter implements SliderAdapter<Double> {
    private final double min, max, def;

    public DoubleSliderAdapter(double min, double max, double def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(Double value) {
        return (float)((value - min) / (max - min));
    }

    @Override
    public Double fromSliderPosition(float sliderPos) {
        return min + sliderPos * (max - min);
    }

    @Override
    public Double clamp(Double value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String format(Double value) {
        return String.format("%.1f", value);
    }

    @Override
    public Double defaultValue() {
        return def;
    }

    @Override
    public Double getMin() {
        return min;
    }

    @Override
    public Double getMax() {
        return max;
    }
}

