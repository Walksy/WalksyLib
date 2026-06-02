package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class DoubleSliderAdapter implements SliderAdapter<Double> {
    private final double min, max, def;

    public DoubleSliderAdapter(final double min, final double max, final double def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(final Double value) {
        return (float) ((value - this.min) / (this.max - this.min));
    }

    @Override
    public Double fromSliderPosition(final float sliderPos) {
        return this.min + sliderPos * (this.max - this.min);
    }

    @Override
    public Double clamp(final Double value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    @Override
    public String format(final Double value) {
        return String.format("%.1f", value);
    }

    @Override
    public Double defaultValue() {
        return this.def;
    }

    @Override
    public Double getMin() {
        return this.min;
    }

    @Override
    public Double getMax() {
        return this.max;
    }
}
