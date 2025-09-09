package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class IntSliderAdapter implements SliderAdapter<Integer> {
    private final int min, max, def;

    public IntSliderAdapter(int min, int max, int def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(Integer value) {
        return (float)(value - min) / (max - min);
    }

    @Override
    public Integer fromSliderPosition(float sliderPos) {
        return min + Math.round(sliderPos * (max - min));
    }

    @Override
    public Integer clamp(Integer value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String format(Integer value) {
        return Integer.toString(value);
    }

    @Override
    public Integer defaultValue() {
        return def;
    }

    @Override
    public Integer getMin() {
        return min;
    }

    @Override
    public Integer getMax() {
        return max;
    }
}

