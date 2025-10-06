package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringOption extends OptionBuilder<String, StringOption> {

    public StringOption(String name, Supplier<String> getter, String defaultValue, Consumer<String> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static StringOption createBuilder(String name, Supplier<String> getter, String defaultValue, Consumer<String> setter) {
        return new StringOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<String> build() {
        return new Option<>(name, description, getter, setter, availability, String.class, defaultValue, onChange);
    }
}
