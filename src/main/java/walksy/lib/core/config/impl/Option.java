package walksy.lib.core.config.impl;

import walksy.lib.core.gui.widgets.BooleanWidget;
import walksy.lib.core.gui.widgets.OptionWidget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class Option<T> {
    private final String name;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Class<T> type;
    private final String description;
    private final T min;
    private final T max;
    private final T increment;

    public Option(String name, Supplier<T> getter, Consumer<T> setter, Class<T> type) {
        this(name, getter, setter, type, null, null, null, null);
    }

    public Option(String name, Supplier<T> getter, Consumer<T> setter, Class<T> type, String description) {
        this(name, getter, setter, type, description, null, null, null);
    }

    public Option(String name, Supplier<T> getter, Consumer<T> setter, Class<T> type, String description, T min, T max, T increment) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
        this.type = type;
        this.description = description;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }


    public String getName() { return name; }
    public Supplier<T> getGetter() { return getter; }
    public Consumer<T> getSetter() { return setter; }
    public Class<T> getType() { return type; }
    public String getDescription() { return description; }
    public T getMin() { return min; }
    public T getMax() { return max; }
    public T getIncrement() { return increment; }

    public OptionWidget createWidget(int x, int y, int width, int height) {
        if (type == Boolean.class) {
            return new BooleanWidget(x, y, width, height, (Option<Boolean>) this);
        } else if (type == Integer.class) {
            // return new IntegerWidget(x, y, width, height, (Option<Integer>) this);
        } else {
            throw new UnsupportedOperationException("Unsupported option type: " + type);
        }
        return null;
    }
}
