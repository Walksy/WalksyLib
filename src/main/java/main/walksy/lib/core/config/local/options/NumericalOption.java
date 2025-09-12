package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NumericalOption<T extends Number> extends OptionBuilder<T, NumericalOption<T>> {

    private T min;
    private T max;
    private T increment;

    public NumericalOption(String name, Supplier<T> getter, T defaultOption, Consumer<T> setter) {
        super(name, getter, defaultOption, setter);
    }

    public static <T extends Number> NumericalOption<T> createBuilder(String name, Supplier<T> getter, T defaultValue, Consumer<T> setter) {
        return new NumericalOption<>(name, getter, defaultValue, setter);
    }

    public NumericalOption<T> values(T min, T max, T increment) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        return this;
    }

    @Override
    public Option<T> build() {
        return new Option<>(name, description, getter, setter, (Class<T>) getter.get().getClass(), min, max, increment, defaultValue, null, () -> new Point(-1, -1));
    }
}
