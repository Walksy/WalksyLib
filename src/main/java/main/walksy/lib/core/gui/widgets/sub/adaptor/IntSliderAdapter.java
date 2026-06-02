package main.walksy.lib.core.gui.widgets.sub.adaptor;

public class IntSliderAdapter implements SliderAdapter<Integer> {
    private final int min, max, def;

    public IntSliderAdapter(final int min, final int max, final int def) {
        this.min = min;
        this.max = max;
        this.def = def;
    }

    @Override
    public float toSliderPosition(final Integer value) {
        return (float) (value - this.min) / (this.max - this.min);
    }

    @Override
    public Integer fromSliderPosition(final float sliderPos) {
        return this.min + Math.round(sliderPos * (this.max - this.min));
    }

    @Override
    public Integer clamp(final Integer value) {
        return Math.max(this.min, Math.min(this.max, value));
    }

    @Override
    public String format(final Integer value) {
        return Integer.toString(value);
    }

    @Override
    public Integer defaultValue() {
        return this.def;
    }

    @Override
    public Integer getMin() {
        return this.min;
    }

    @Override
    public Integer getMax() {
        return this.max;
    }
}
