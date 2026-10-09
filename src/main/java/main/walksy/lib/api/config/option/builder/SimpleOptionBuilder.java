package main.walksy.lib.api.config.option.builder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SimpleOptionBuilder<T> extends OptionBuilder<T, SimpleOptionBuilder<T>> {
    private final Class<T> valueType;
    private boolean container;

    public SimpleOptionBuilder(final String name, final Supplier<T> getter, final T defaultValue, final Consumer<T> setter, final Class<T> valueType) {
        super(name, getter, defaultValue, setter);
        this.valueType = valueType;
    }

    public SimpleOptionBuilder<T> container() {
        this.container = true;
        return this;
    }

    @Override
    public boolean isContainer() {
        return this.container;
    }

    @Override
    public Class<T> valueType() {
        return this.valueType;
    }
}
