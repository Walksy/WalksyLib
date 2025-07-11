package main.walksy.lib.core.config.impl.builders;

import main.walksy.lib.core.config.impl.Option;
import main.walksy.lib.core.config.impl.OptionDescription;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class OptionBuilder<T, SELF extends OptionBuilder<T, SELF>> {

    protected final String name;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    protected OptionDescription description;

    public OptionBuilder(String name, Supplier<T> getter, Consumer<T> setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("unchecked")
    public SELF description(OptionDescription description) {
        this.description = description;
        return (SELF) this;
    }

    public abstract Option<T> build();
}
