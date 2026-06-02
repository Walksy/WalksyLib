package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonOption extends OptionBuilder<Runnable, ButtonOption> {

    public ButtonOption(final String name, final Supplier<Runnable> getter, final Runnable defaultValue, final Consumer<Runnable> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static ButtonOption createBuilder(final String name, final Runnable action) {
        return new ButtonOption(name, () -> action, action, null);
    }

    @Override
    public Option<Runnable> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, Runnable.class, this.defaultValue, this.onChange);
    }
}
