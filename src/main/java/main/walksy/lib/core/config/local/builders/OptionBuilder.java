package main.walksy.lib.core.config.local.builders;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.OptionDescription;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class OptionBuilder<T, SELF extends OptionBuilder<T, SELF>> {

    protected final String name;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    protected final T defaultValue;
    protected Runnable onChange;
    protected OptionDescription description;
    protected Supplier<Boolean> availability = () -> true;
    protected String availabilityHelp = "";

    public OptionBuilder(String name, Supplier<T> getter, T defaultValue, Consumer<T> setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    @SuppressWarnings("unchecked")
    public SELF description(OptionDescription description) {
        this.description = description;
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF onChange(Runnable onChange) {
        this.onChange = onChange;
        return (SELF) this;
    }

    @SuppressWarnings("unchecked")
    public SELF availability(Supplier<Boolean> condition, String availabilityHelper) {
        this.availability = condition;
        this.availabilityHelp = availabilityHelper;
        return (SELF) this;
    }

    public abstract Option<T> build();
}
