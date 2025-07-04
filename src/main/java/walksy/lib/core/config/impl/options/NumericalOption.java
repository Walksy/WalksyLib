package walksy.lib.core.config.impl.options;

import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class NumericalOption<T extends Number> extends OptionBuilder<T, NumericalOption<T>> {

    private T min;
    private T max;
    private T increment;

    public NumericalOption(String name, Supplier<T> getter, Consumer<T> setter) {
        super(name, getter, setter);
    }

    public static <T extends Number> NumericalOption<T> createBuilder(String name, Supplier<T> getter, Consumer<T> setter) {
        return new NumericalOption<>(name, getter, setter);
    }

    public NumericalOption<T> properties(T min, T max, T increment) {
        this.min = min;
        this.max = max;
        this.increment = increment;
        return this;
    }

    @Override
    public Option<T> build() {
        return new Option<>(name, getter, setter, (Class<T>) getter.get().getClass(), description, min, max, increment);
    }
}
