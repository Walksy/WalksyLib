package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class PixelGridAnimationOption extends OptionBuilder<PixelGridAnimation, PixelGridAnimationOption> {

    public PixelGridAnimationOption(final String name, final Supplier<PixelGridAnimation> getter, final PixelGridAnimation defaultValue, final Consumer<PixelGridAnimation> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static PixelGridAnimationOption createBuilder(final String name, final Supplier<PixelGridAnimation> getter, final PixelGridAnimation defaultValue, final Consumer<PixelGridAnimation> setter) {
        return new PixelGridAnimationOption(name, getter, defaultValue, setter);
    }

    @Override
    public Option<PixelGridAnimation> build() {
        return new Option<>(this.name, this.description, this.getter, this.setter, this.availability, this.availabilityHelp, PixelGridAnimation.class, null, null, null, this.defaultValue, null, this.onChange);
    }
}
