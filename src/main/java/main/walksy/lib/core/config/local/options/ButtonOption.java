package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ButtonOption extends OptionBuilder<Runnable, ButtonOption> {

    public ButtonOption(String name, Supplier<Runnable> getter, Runnable defaultValue, Consumer<Runnable> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static ButtonOption createBuilder(String name, Runnable action) {
        return new ButtonOption(name, () -> action, action, null);
    }

    @Override
    public Option<Runnable> build() {
        return new Option<>(name, description, getter, setter, availability, Runnable.class, defaultValue, onChange);
    }
}
