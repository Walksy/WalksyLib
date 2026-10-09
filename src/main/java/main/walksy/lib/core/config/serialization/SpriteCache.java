package main.walksy.lib.core.config.serialization;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.WalksyLibConstants;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.api.config.value.CroppedTexture;
import main.walksy.lib.core.utils.IdentifierWrapper;
import main.walksy.lib.api.config.PathUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class SpriteCache {
    private static final String DROPPED_PREFIX = "dropped/";

    private SpriteCache() {
    }

    public static Path getDirectory() {
        final Path destDir = FabricLoader.getInstance().getConfigDir().resolve(PathUtils.LIBRARY_DIR).resolve("CachedImages");
        try {
            Files.createDirectories(destDir);
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to create cached image directory: " + e.getMessage());
        }
        return destDir;
    }

    public static void sweepUnused() {
        final Path cacheDir = getDirectory();
        if (!Files.exists(cacheDir)) {
            return;
        }
        final Set<String> used = referencedFiles();
        try (final Stream<Path> files = Files.list(cacheDir)) {
            for (final Path path : files.toList()) {
                final String fileName = path.getFileName().toString();
                if (used.contains(fileName) || !Files.isRegularFile(path)) {
                    continue;
                }
                try {
                    Files.delete(path);
                } catch (final IOException e) {
                    WalksyLib.getLogger().err("Failed to delete unused cached image: " + fileName);
                }
            }
        } catch (final IOException e) {
            WalksyLib.getLogger().err("Failed to list cache directory: " + e.getMessage());
        }
    }

    private static Set<String> referencedFiles() {
        final Set<String> used = new HashSet<>();
        final WalksyLib lib = WalksyLib.getInstanceOrNull();
        if (lib == null) {
            return used;
        }
        for (final var mod : lib.getModEntryPointList().getMods()) {
            if (mod.hasConfig()) {
                collectReferences(mod.getConfig(), used);
            }
        }
        collectReferences(lib.getGlobalTeams().config(), used);
        collectReferences(lib.getAppearancePage().config(), used);
        collectReferences(lib.getKeyBinds().config(), used);
        return used;
    }

    private static void collectReferences(final Config config, final Set<String> used) {
        config.options().forEach(option -> {
            final Object value = option.getValue();
            if (value instanceof IdentifierWrapper wrapper) {
                final String fileName = wrapper.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    used.add(fileName);
                }
            } else if (value instanceof Identifier id) {
                addDropped(id, used);
            } else if (value instanceof CroppedTexture cropped && cropped.texture() != null) {
                addDropped(cropped.texture(), used);
            }
        });
    }

    private static void addDropped(final Identifier id, final Set<String> used) {
        if (id.getNamespace().equals(WalksyLibConstants.NAMESPACE) && id.getPath().startsWith(DROPPED_PREFIX)) {
            used.add(id.getPath().substring(DROPPED_PREFIX.length()));
        }
    }


    public static Identifier loadTexture(final String fileName) {
        final Path imagePath = getDirectory().resolve(fileName);
        if (!Files.exists(imagePath)) {
            return null;
        }
        try {
            final byte[] fileBytes = Files.readAllBytes(imagePath);
            if (fileBytes.length == 0) {
                return null;
            }
            final NativeImage image = readImage(fileBytes);
            if (image == null) {
                WalksyLib.getLogger().err("Failed to decode cached image data for: " + fileName);
                return null;
            }
            final DynamicTexture texture = new DynamicTexture(() -> fileName, image);
            String name = fileName;
            final int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }
            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
            final Identifier textureId = Identifier.fromNamespaceAndPath("walksylib", "dropped/" + name);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            return textureId;
        } catch (final Exception e) {
            WalksyLib.getLogger().err(e.getMessage());
            return null;
        }
    }

    public static NativeImage readImage(final byte[] imageBytes) {
        try {
            return NativeImage.read(new ByteArrayInputStream(imageBytes));
        } catch (final Exception e) {
            try {
                final BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (bufferedImage == null) {
                    return null;
                }
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                return NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
            } catch (final Exception ex) {
                return null;
            }
        }
    }
}
