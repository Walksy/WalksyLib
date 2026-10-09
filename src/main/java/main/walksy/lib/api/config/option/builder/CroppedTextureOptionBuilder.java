package main.walksy.lib.api.config.option.builder;

import main.walksy.lib.api.config.value.CroppedTexture;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CroppedTextureOptionBuilder extends OptionBuilder<CroppedTexture, CroppedTextureOptionBuilder> {
    private float aspect;

    public CroppedTextureOptionBuilder(final String name, final Supplier<CroppedTexture> getter, final CroppedTexture defaultValue, final Consumer<CroppedTexture> setter) {
        super(name, getter, defaultValue, setter);
    }

    public CroppedTextureOptionBuilder aspect(final float width, final float height) {
        if (width <= 0f || height <= 0f) {
            throw new IllegalArgumentException("Crop aspect must be positive, got " + width + ":" + height);
        }
        this.aspect = width / height;
        return this;
    }

    @Override
    public float cropAspect() {
        return this.aspect;
    }

    @Override
    public Class<CroppedTexture> valueType() {
        return CroppedTexture.class;
    }
}
