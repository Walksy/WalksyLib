package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.value.WLColor;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ColorOptionBuilder extends OptionBuilder<WLColor, ColorOptionBuilder> {
    private boolean alphaEditable = true;
    private boolean chromaEditable = true;

    public ColorOptionBuilder(final String name, final Supplier<WLColor> getter, final WLColor defaultValue, final Consumer<WLColor> setter) {
        super(name, getter, defaultValue, setter);
    }

    public ColorOptionBuilder disableAlpha() {
        this.alphaEditable = false;
        return this;
    }

    public ColorOptionBuilder disableChroma() {
        this.chromaEditable = false;
        return this;
    }

    @Override
    public boolean alphaEditable() {
        return this.alphaEditable;
    }

    @Override
    public boolean chromaEditable() {
        return this.chromaEditable;
    }

    @Override
    public Class<WLColor> valueType() {
        return WLColor.class;
    }
}
