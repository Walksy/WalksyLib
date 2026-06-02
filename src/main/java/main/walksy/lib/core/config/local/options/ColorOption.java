package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorOption extends OptionBuilder<WalksyLibColor, ColorOption> {

    public ColorOption(final String name, final Supplier<WalksyLibColor> getter, final WalksyLibColor defaultValue, final Consumer<WalksyLibColor> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static ColorOption createBuilder(final String name, final Supplier<WalksyLibColor> getter, final WalksyLibColor defaultValue, final Consumer<WalksyLibColor> setter) {
        return new ColorOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<WalksyLibColor> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, WalksyLibColor.class, this.defaultValue, this.onChange);
    }
}
