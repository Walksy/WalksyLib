package main.walksy.lib.api.config.option.builder;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class NumberOptionBuilder<T extends Number> extends OptionBuilder<T, NumberOptionBuilder<T>> {
    private T min;
    private T max;
    private T increment;

    public NumberOptionBuilder(final String name, final Supplier<T> getter, final T defaultValue, final Consumer<T> setter) {
        super(name, getter, Objects.requireNonNull(defaultValue, "number option default value"), setter);
    }

    public NumberOptionBuilder<T> range(final T min, final T max, final T increment) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        return this;
    }

    @Override
    public T min() {
        return this.min;
    }

    @Override
    public T max() {
        return this.max;
    }

    @Override
    public T increment() {
        return this.increment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> valueType() {
        return (Class<T>) this.defaultValue().getClass();
    }
}
