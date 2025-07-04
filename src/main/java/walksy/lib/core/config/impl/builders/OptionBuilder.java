package walksy.lib.core.config.impl.builders;

import walksy.lib.core.config.impl.Option;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class OptionBuilder<T, SELF extends OptionBuilder<T, SELF>> {

    protected final String name;
    protected final Supplier<T> getter;
    protected final Consumer<T> setter;
    protected String description;

    public OptionBuilder(String name, Supplier<T> getter, Consumer<T> setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
    }

    @SuppressWarnings("unchecked")
    public SELF description(String description) {
        this.description = description;
        return (SELF) this;
    }

    public abstract Option<T> build();
}
