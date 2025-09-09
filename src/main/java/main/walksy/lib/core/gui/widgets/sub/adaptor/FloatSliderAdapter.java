package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class FloatSliderAdapter implements SliderAdapter<Float> {
    private final float min, max, def;

    public FloatSliderAdapter(float min, float max, float def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(Float value) {
        return (value - min) / (max - min);
    }

    @Override
    public Float fromSliderPosition(float sliderPos) {
        return min + sliderPos * (max - min);
    }

    @Override
    public Float clamp(Float value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String format(Float value) {
        return String.format("%.1f", value);
    }

    @Override
    public Float defaultValue() {
        return def;
    }

    @Override
    public Float getMin() {
        return min;
    }

    @Override
    public Float getMax() {
        return max;
    }
}

