package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorOption extends OptionBuilder<Color, ColorOption> {

    public ColorOption(String name, Supplier<Color> getter, Color defaultValue, Consumer<Color> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static ColorOption createBuilder(String name, Supplier<Color> getter, Color defaultValue, Consumer<Color> setter) {
        return new ColorOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<Color> build() {
        return new Option<>(name, description, getter, setter, Color.class, defaultValue);
    }
}
