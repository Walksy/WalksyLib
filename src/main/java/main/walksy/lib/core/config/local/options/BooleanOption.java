package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanOption extends OptionBuilder<Boolean, BooleanOption> {

    private Warning warning;

    public BooleanOption(final String name, final Supplier<Boolean> getter, final boolean defaultValue, final Consumer<Boolean> setter) {
        super(name, getter, defaultValue, setter);
    }

    public BooleanOption addWarning(final Warning warning) {
        this.warning = warning;
        return this;
    }

    public static BooleanOption createBuilder(final String name, final Supplier<Boolean> getter, final boolean defaultValue, final Consumer<Boolean> setter) {
        return new BooleanOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<Boolean> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, Boolean.class, this.defaultValue, this.warning, this.onChange);
    }

    public record Warning(String title, String message, Runnable onYes, Runnable onNo) {
    }
}
