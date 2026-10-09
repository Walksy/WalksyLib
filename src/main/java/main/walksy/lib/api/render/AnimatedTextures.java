package main.walksy.lib.api.render;

import main.walksy.lib.core.render.texture.AnimatedTexture;
import main.walksy.lib.core.render.texture.AnimatedTextureRegistry;

import net.minecraft.resources.Identifier;

public final class AnimatedTextures {
    private AnimatedTextures() {
    }

    public static Identifier currentFrame(final Identifier id) {
        if (id == null) {
            return null;
        }
        final AnimatedTexture animated = AnimatedTextureRegistry.get(id);
        return animated != null ? animated.currentFrame() : id;
    }

    public static boolean isAnimated(final Identifier id) {
        final AnimatedTexture animated = AnimatedTextureRegistry.get(id);
        return animated != null && animated.isAnimated();
    }

    public static boolean isRegistered(final Identifier id) {
        return AnimatedTextureRegistry.get(id) != null;
    }
}
