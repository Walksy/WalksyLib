package main.walksy.lib.core.config.local;

import main.walksy.lib.core.config.local.options.BooleanOption;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.*;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Option<T> {
    private final String name;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Class<T> type;
    private final T min;
    private final T max;
    private final T increment;
    private OptionDescription description;
    private final T defaultValue;

    //Screen
    public T screenInstanceValue = null;

    //Boolean Option
    private BooleanOption.Warning warning;


    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type, T defaultValue, BooleanOption.Warning warning) {
        this(name, description, getter, setter, type, null, null, null, defaultValue, warning, null);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type, T defaultValue) {
        this(name, description, getter, setter, type, null, null, null, defaultValue, null, null);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter,
                  Class<T> type, T min, T max, T increment, T defaultValue, BooleanOption.Warning warning, Point point) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
        this.min = min;
        this.max = max;
        this.increment = increment;
        if (getter.get() instanceof PixelGridAnimation) {
            this.defaultValue = (T) ((PixelGridAnimation)defaultValue).copy();
        } else if (getter.get() instanceof WalksyLibColor) {
            this.defaultValue = (T) ((WalksyLibColor)defaultValue).copy();
        } else {
            this.defaultValue = defaultValue;
        }
        this.warning = warning;
        if (point != null) {
            this.definePosition(point);
        }
    }

    public String getName() { return name; }
    private Supplier<T> getGetter() { return getter; }
    private Consumer<T> getSetter() { return setter; }
    public Class<T> getType() { return type; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public T getIncrement() { return increment; }

    public OptionDescription getDescription() { return description; }

    public T getValue()
    {
        return getter.get();
    }

    public T getDefaultValue()
    {
        return defaultValue;
    }

    public boolean screenInstanceCheck()
    {
        return Objects.equals(this.screenInstanceValue, this.getValue());
    }

    public void setScreenInstance()
    {
        if (this.getValue() instanceof PixelGridAnimation animation) {
            this.screenInstanceValue = (T) animation.copy();
        } else if (this.getValue() instanceof WalksyLibColor color) {
            this.screenInstanceValue = (T) color.copy();
        } else {
            this.screenInstanceValue = this.getter.get();
        }
    }

    public boolean has2DPosition() {
        if (this.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            return true; //
        }
        return false;
    }

    public void definePosition(Point point) {
        if (this.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            pixelGridAnimation.setPosition(point.x, point.y);
        }
    }

    public void undo()
    {
        this.setter.accept(this.screenInstanceValue);
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if (type.isInstance(value)) {
            setter.accept((T) value);
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass().getName());
        }
    }

    public boolean canReset()
    {
        return this.getValue() != this.defaultValue;
    }

    public void reset()
    {
        if (this.getValue() instanceof PixelGridAnimation) {
            this.setter.accept((T) ((PixelGridAnimation) this.defaultValue).copy());
        } else if (this.getValue() instanceof WalksyLibColor c) {
            this.setter.accept((T) ((WalksyLibColor) this.defaultValue).copy());
        } else {
            this.setter.accept(this.defaultValue);
        }
    }



    public Option<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    public void tick() {
        if (this.getValue() instanceof WalksyLibColor color)
        {
            color.handleRainbow();
            color.handlePulse();
        } else if (this.getValue() instanceof PixelGridAnimation animation)
        {
            animation.tick();
        }
    }

    public boolean hasChanged() {
        T value = getValue();
        T defaultValue = getDefaultValue();

        if (value == null || defaultValue == null) {
            return value != defaultValue;
        }

        return !Objects.equals(value, defaultValue);
    }


    @SuppressWarnings("unchecked")
    public OptionWidget createWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height) {
        if (type == Boolean.class) {
            return new BooleanWidget(parent, screen, x, y, width, height, (Option<Boolean>) this, this.warning);
        } else if (type == Integer.class) {
            return new NumericalWidget<Integer>(parent, screen, x, y, width, height, (Option<Integer>) this);
        } else if (type == Double.class) {
            return new NumericalWidget<Double>(parent, screen, x, y, width, height, (Option<Double>) this);
        } else if (type == Float.class) {
            return new NumericalWidget<Float>(parent, screen, x, y, width, height, (Option<Float>) this);
        } else if (type == java.awt.Color.class || type == WalksyLibColor.class) {
            return new ColorWidget(parent, screen, x, y, width, height, (Option<WalksyLibColor>) this);
        } else if (type == PixelGridAnimation.class) {
            return new PixelGridAnimationWidget(parent, screen, x, y, width, height, (Option<PixelGridAnimation>) this);
        } else {
            throw new UnsupportedOperationException("Unsupported option type: " + type);
        }
    }
}
