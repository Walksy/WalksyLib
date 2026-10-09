package main.walksy.lib.api.render;

import main.walksy.lib.core.render.texture.AnimatedTexture;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.core.config.serialization.SpriteCache;
import main.walksy.lib.core.render.WLRenderSystem;
import main.walksy.lib.core.render.gpu.WLTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedTextures {
    public static final String PATH_PREFIX = "dropped/";
    public static final String[] IMAGE_FILTER_PATTERNS = {"*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"};
    private static final Set<Identifier> LOADING = ConcurrentHashMap.newKeySet();

    private CachedTextures() {
    }

    public static boolean isCacheBacked(@Nullable final Identifier id) {
        return id != null && id.getNamespace().equals(WalksyLibConstants.NAMESPACE) && id.getPath().startsWith(PATH_PREFIX);
    }

    public static boolean isReady(@Nullable final Identifier id) {
        return id != null && (!isCacheBacked(id) || AnimatedTextures.isRegistered(id));
    }

    public static void ensureLoaded(@Nullable final Identifier id) {
        if (isCacheBacked(id) && !AnimatedTextures.isRegistered(id)) {
            load(id, false, null);
        }
    }

    public static void load(final Identifier id, final boolean forceReload, @Nullable final Runnable onComplete) {
        if (!forceReload && AnimatedTextures.isRegistered(id)) {
            runIfPresent(onComplete);
            return;
        }
        if (!forceReload && onComplete == null && !LOADING.add(id)) {
            return;
        }
        final Path file = SpriteCache.getDirectory().resolve(id.getPath().substring(PATH_PREFIX.length()));
        if (!Files.isRegularFile(file)) {
            WalksyLib.getLogger().err("Cached texture file missing: " + file.getFileName());
            LOADING.remove(id);
            runIfPresent(onComplete);
            return;
        }
        try {
            AnimatedTexture.decodeAsync(Files.readAllBytes(file), id, () -> {
                LOADING.remove(id);
                runIfPresent(onComplete);
            });
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to read cached image " + file.getFileName() + ": " + e.getMessage());
            LOADING.remove(id);
            runIfPresent(onComplete);
        }
    }

    public static Identifier importFile(final Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) {
            throw new IOException("Image file is missing or empty: " + file);
        }
        final String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        final Path dest = SpriteCache.getDirectory().resolve(fileName);
        if (!file.toAbsolutePath().normalize().equals(dest.toAbsolutePath().normalize())) {
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return Identifier.fromNamespaceAndPath(WalksyLibConstants.NAMESPACE, PATH_PREFIX + fileName);
    }

    public static int @Nullable [] size(@Nullable final Identifier id) {
        if (!isReady(id)) {
            return null;
        }
        final WLTexture texture = WLRenderSystem.texture(AnimatedTextures.currentFrame(id));
        if (texture == null) {
            return null;
        }
        return new int[]{Math.max(1, texture.width()), Math.max(1, texture.height())};
    }

    private static void runIfPresent(@Nullable final Runnable task) {
        if (task != null) {
            task.run();
        }
    }
}
