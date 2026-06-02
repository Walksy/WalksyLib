package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class FloatSliderAdapter implements SliderAdapter<Float> {
    private final float min, max, def;

    public FloatSliderAdapter(final float min, final float max, final float def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(final Float value) {
        return (value - this.min) / (this.max - this.min);
    }

    @Override
    public Float fromSliderPosition(final float sliderPos) {
        return this.min + sliderPos * (this.max - this.min);
    }

    @Override
    public Float clamp(final Float value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    @Override
    public String format(final Float value) {
        return String.format("%.1f", value);
    }

    @Override
    public Float defaultValue() {
        return this.def;
    }

    @Override
    public Float getMin() {
        return this.min;
    }

    @Override
    public Float getMax() {
        return this.max;
    }
}
