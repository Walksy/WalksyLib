package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanOption extends OptionBuilder<Boolean, BooleanOption> {

    private Warning warning;

    public BooleanOption(String name, Supplier<Boolean> getter, boolean defaultValue, Consumer<Boolean> setter) {
        super(name, getter, defaultValue, setter);
    }

    public BooleanOption addWarning(Warning warning) {
        this.warning = warning;
        return this;
    }

    public static BooleanOption createBuilder(String name, Supplier<Boolean> getter, boolean defaultValue, Consumer<Boolean> setter) {
        return new BooleanOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<Boolean> build() {
        return new Option<>(name, description, getter, setter, availability, Boolean.class, defaultValue, warning, onChange);
    }

    public static class Warning
    {
        public String title;
        public String message;
        public Runnable onYes, onNo;

        public Warning(String title, String message, Runnable onYes, Runnable onNo)
        {
            this.title = title;
            this.message = message;
            this.onYes = onYes;
            this.onNo = onNo;
        }
    }
}
