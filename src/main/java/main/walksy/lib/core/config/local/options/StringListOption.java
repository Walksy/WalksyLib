package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringListOption extends OptionBuilder<List<String>, StringListOption> {

    public StringListOption(String name, Supplier<List<String>> getter, List<String> defaultValue, Consumer<List<String>> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static StringListOption createBuilder(String name, Supplier<List<String>> getter, List<String> defaultValue, Consumer<List<String>> setter) {
        return new StringListOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<List<String>> build() {
        Class<List<String>> clazz = (Class<List<String>>) (Class<?>) List.class;
        return new Option<>(name, description, getter, setter, availability, clazz, defaultValue);
    }
}
