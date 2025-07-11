package walksy.lib.core.config.impl.options;

import walksy.lib.core.config.impl.Option;
import walksy.lib.core.config.impl.builders.OptionBuilder;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanOption extends OptionBuilder<Boolean, BooleanOption> {

    public BooleanOption(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        super(name, getter, setter);
    }

    public static BooleanOption createBuilder(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new BooleanOption(name, getter, setter);
    }

    @Override
    public Option<Boolean> build() {
        return new Option<>(name, description, getter, setter, Boolean.class);
    }
}
