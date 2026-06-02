package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnumOption<E extends Enum<E>> extends OptionBuilder<E, EnumOption<E>> {

    private final Class<E> enumClass;

    public EnumOption(final String name, final Supplier<E> getter, final E defaultValue, final Consumer<E> setter, final Class<E> enumClass) {
        super(name, getter, defaultValue, setter);
        this.enumClass = enumClass;
    }

    public static <E extends Enum<E>> EnumOption<E> createBuilder(final String name, final Supplier<E> getter, final E defaultValue, final Consumer<E> setter, final Class<E> enumClass) {
        return new EnumOption<>(name, getter, defaultValue, setter, enumClass);
    }

    @Override
    public Option<E> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, this.enumClass, this.defaultValue, this.onChange);
    }
}
