package main.walksy.lib.core.config.local.options;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.builders.LocalConfigBuilder;
import main.walksy.lib.core.config.local.builders.OptionBuilder;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PixelGridAnimationOption extends OptionBuilder<PixelGridAnimation, PixelGridAnimationOption> {

    private Supplier<Point> positionSupplier = () -> new Point(-1, -1);

    public PixelGridAnimationOption(String name, Supplier<PixelGridAnimation> getter, PixelGridAnimation defaultValue, Consumer<PixelGridAnimation> setter) {
        super(name, getter, defaultValue, setter);
    }

    public static PixelGridAnimationOption createBuilder(String name, Supplier<PixelGridAnimation> getter, PixelGridAnimation defaultValue, Consumer<PixelGridAnimation> setter) {
        return new PixelGridAnimationOption(name, getter, defaultValue, setter);
    }

    public PixelGridAnimationOption position(Supplier<Point> supplier) {
        this.positionSupplier = supplier;
        return this;
    }

    @Override
    public Option<PixelGridAnimation> build() {
        return new Option<>(name, description, getter, setter, PixelGridAnimation.class, null, null, null, defaultValue, null, positionSupplier);
    }
}
