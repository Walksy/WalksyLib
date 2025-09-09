package main.walksy.lib.core.gui.widgets.sub.adaptor;

public interface SliderAdapter<T> {
    float toSliderPosition(T value);
    T fromSliderPosition(float sliderPos);
    T clamp(T value);
    String format(T value);
    T defaultValue();
    T getMin();
    T getMax();
}
