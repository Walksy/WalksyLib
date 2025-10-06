package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumOption extends OptionBuilder<Enum<?>, EnumOption> {

    public EnumOption(String name, Supplier<Enum<?>> getter, Enum<?> defaultValue, Consumer<Enum<?>> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static EnumOption createBuilder(String name, Enum<?> action) {
        return new EnumOption(name, () -> action, action, null);
    }

    @Override
    public Option<Enum<?>> build() {
        Class<Enum<?>> clazz = (Class<Enum<?>>) (Class<?>) Enum.class;
        return new Option<>(name, description, getter, setter, availability, clazz, defaultValue, onChange);
    }
}
