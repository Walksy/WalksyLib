package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumOption<E extends Enum<E>> extends OptionBuilder<E, EnumOption<E>> {

    private final Class<E> enumClass;

    public EnumOption(String name, Supplier<E> getter, E defaultValue, Consumer<E> setter, Class<E> enumClass) {
        super(name, getter, defaultValue, setter);
        this.enumClass = enumClass;
    }

    public static <E extends Enum<E>> EnumOption<E> createBuilder(String name, Supplier<E> getter, E defaultValue, Consumer<E> setter, Class<E> enumClass) {
        return new EnumOption<>(name, getter, defaultValue, setter, enumClass);
    }

    @Override
    public Option<E> build() {
        return new Option<>(name, description, getter, setter, availability, enumClass, defaultValue, onChange);
    }
}
