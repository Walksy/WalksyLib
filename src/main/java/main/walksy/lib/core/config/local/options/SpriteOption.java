package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.utils.IdentifierWrapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpriteOption extends OptionBuilder<IdentifierWrapper, SpriteOption> {

    public SpriteOption(final String name, final Supplier<IdentifierWrapper> getter, final IdentifierWrapper defaultValue, final Consumer<IdentifierWrapper> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static SpriteOption createBuilder(final String name, final Supplier<IdentifierWrapper> getter, final IdentifierWrapper defaultValue, final Consumer<IdentifierWrapper> setter) {
        return new SpriteOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<IdentifierWrapper> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, IdentifierWrapper.class, this.defaultValue, this.onChange);
    }
}
