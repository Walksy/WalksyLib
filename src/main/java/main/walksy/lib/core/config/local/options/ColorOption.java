package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorOption extends OptionBuilder<WalksyLibColor, ColorOption> {

    public ColorOption(String name, Supplier<WalksyLibColor> getter, WalksyLibColor defaultValue, Consumer<WalksyLibColor> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static ColorOption createBuilder(String name, Supplier<WalksyLibColor> getter, WalksyLibColor defaultValue, Consumer<WalksyLibColor> setter) {
        return new ColorOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<WalksyLibColor> build() {
        return new Option<>(name, description, getter, setter, availability, WalksyLibColor.class, defaultValue);
    }
}
