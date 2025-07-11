package main.walksy.lib.core.config.impl;

import main.walksy.lib.core.config.impl.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.widgets.BooleanWidget;
import main.walksy.lib.core.gui.widgets.OptionWidget;

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

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type) {
        this(name, description, getter, setter, type, null, null, null);
    }

    public Option(String name, OptionDescription description, Supplier<T> getter, Consumer<T> setter, Class<T> type, T min, T max, T increment) {
        this.name = name;
        this.description = description;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public String getName() { return name; }
    public Supplier<T> getGetter() { return getter; }
    public Consumer<T> getSetter() { return setter; }
    public Class<T> getType() { return type; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public T getIncrement() { return increment; }

    public OptionDescription getDescription() { return description; }

    public T getValue()
    {
        return getter.get();
    }

    public void setValue(T value) {
        setter.accept(value);
    }

    public Option<T> description(OptionDescription description) {
        this.description = description;
        return this;
    }

    public OptionWidget createWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height) {
        if (type == Boolean.class) {
            return new BooleanWidget(parent, screen, x, y, width, height, (Option<Boolean>) this);
        } else if (type == Integer.class) {
            // return new IntegerWidget(x, y, width, height, (Option<Integer>) this);
        } else {
            throw new UnsupportedOperationException("Unsupported option type: " + type);
        }
        return null;
    }
}
