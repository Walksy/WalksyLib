package main.walksy.lib.core.render.texture;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AnimatedTextureRegistry {
    private static final Map<Identifier, AnimatedTexture> REGISTRY = new ConcurrentHashMap<>();

    private AnimatedTextureRegistry() {
    }

    static void register(final Identifier baseId, final AnimatedTexture texture) {
        REGISTRY.put(baseId, texture);
    }

    public static @Nullable AnimatedTexture get(@Nullable final Identifier id) {
        return id == null ? null : REGISTRY.get(id);
    }
}
