package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NumericalOption<T extends Number> extends OptionBuilder<T, NumericalOption<T>> {

    private T min;
    private T max;
    private T increment;

    public NumericalOption(final String name, final Supplier<T> getter, final T defaultOption, final Consumer<T> setter) {
        super(name, getter, defaultOption, setter);
    }

    public static <T extends Number> NumericalOption<T> createBuilder(final String name, final Supplier<T> getter, final T defaultValue, final Consumer<T> setter) {
        return new NumericalOption<>(name, getter, defaultValue, setter);
    }

    public NumericalOption<T> values(final T min, final T max, final T increment) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        return this;
    }

    @Override
    public Option<T> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, (Class<T>) this.getter.get().getClass(), this.min, this.max, this.increment, this.defaultValue, null, this.onChange);
    }
}
