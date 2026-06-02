package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringListOption extends OptionBuilder<List<String>, StringListOption> {

    public StringListOption(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static StringListOption createBuilder(final String name, final Supplier<List<String>> getter, final List<String> defaultValue, final Consumer<List<String>> setter) {
        return new StringListOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<List<String>> build() {
        final Class<List<String>> clazz = (Class<List<String>>) (Class<?>) List.class;
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, clazz, this.defaultValue, this.onChange);
    }
}
