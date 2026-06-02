package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringOption extends OptionBuilder<String, StringOption> {

    public StringOption(final String name, final Supplier<String> getter, final String defaultValue, final Consumer<String> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static StringOption createBuilder(final String name, final Supplier<String> getter, final String defaultValue, final Consumer<String> setter) {
        return new StringOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<String> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, String.class, this.defaultValue, this.onChange);
    }
}
