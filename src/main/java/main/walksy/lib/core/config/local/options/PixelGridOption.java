package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.PixelGrid;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PixelGridOption extends OptionBuilder<PixelGrid, PixelGridOption> {

    public PixelGridOption(final String name, final Supplier<PixelGrid> getter, final PixelGrid defaultValue, final Consumer<PixelGrid> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static PixelGridOption createBuilder(final String name, final Supplier<PixelGrid> getter, final PixelGrid defaultValue, final Consumer<PixelGrid> setter) {
        return new PixelGridOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<PixelGrid> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, PixelGrid.class, this.defaultValue, this.onChange);
    }
}
