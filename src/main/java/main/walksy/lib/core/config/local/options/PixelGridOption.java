package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.PixelGrid;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PixelGridOption extends OptionBuilder<PixelGrid, PixelGridOption> {

    public PixelGridOption(String name, Supplier<PixelGrid> getter, PixelGrid defaultValue, Consumer<PixelGrid> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static PixelGridOption createBuilder(String name, Supplier<PixelGrid> getter, PixelGrid defaultValue, Consumer<PixelGrid> setter) {
        return new PixelGridOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<PixelGrid> build() {
        return new Option<>(name, description, getter, setter, availability, PixelGrid.class, defaultValue, onChange);
    }
}
