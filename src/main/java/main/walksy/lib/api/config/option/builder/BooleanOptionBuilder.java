package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.option.Warning;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BooleanOptionBuilder extends OptionBuilder<Boolean, BooleanOptionBuilder> {
    private Warning warning;

    public BooleanOptionBuilder(final String name, final Supplier<Boolean> getter, final boolean defaultValue, final Consumer<Boolean> setter) {
        super(name, getter, defaultValue, setter);
    }

    public BooleanOptionBuilder warning(final Warning warning) {
        this.warning = warning;
        return this;
    }

    @Override
    public Warning warning() {
        return this.warning;
    }

    @Override
    public Class<Boolean> valueType() {
        return Boolean.class;
    }
}
