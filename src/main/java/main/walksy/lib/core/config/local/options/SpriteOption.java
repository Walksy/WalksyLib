package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.utils.IdentifierWrapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpriteOption extends OptionBuilder<IdentifierWrapper, SpriteOption> {

    public SpriteOption(String name, Supplier<IdentifierWrapper> getter, IdentifierWrapper defaultValue, Consumer<IdentifierWrapper> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static SpriteOption createBuilder(String name, Supplier<IdentifierWrapper> getter, IdentifierWrapper defaultValue, Consumer<IdentifierWrapper> setter) {
        return new SpriteOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<IdentifierWrapper> build() {
        return new Option<>(name, description, getter, setter, availability, IdentifierWrapper.class, defaultValue);
    }
}
