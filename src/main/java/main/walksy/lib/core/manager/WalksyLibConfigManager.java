package main.walksy.lib.core.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.config.impl.ModConfig;
import main.walksy.lib.core.config.local.Category;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.config.local.options.type.WalksyLibColor;
import main.walksy.lib.core.config.serialization.OptionConverter;
import main.walksy.lib.core.config.serialization.SerializableCategory;
import main.walksy.lib.core.config.serialization.SerializableGroup;
import main.walksy.lib.core.config.serialization.SerializableOption;
import main.walksy.lib.core.config.serialization.adapters.ColorTypeAdapter;
import main.walksy.lib.core.config.serialization.adapters.IdentifierWrapperAdapter;
import main.walksy.lib.core.config.serialization.adapters.PixelGridAdapter;
import main.walksy.lib.core.config.serialization.adapters.PixelGridAnimationAdapter;
import main.walksy.lib.core.utils.IdentifierWrapper;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
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
import java.util.ArrayList;
import java.util.List;

public class WalksyLibConfigManager {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(WalksyLibColor.class, new ColorTypeAdapter())
            .registerTypeAdapter(PixelGrid.class, new PixelGridAdapter())
            .registerTypeAdapter(PixelGridAnimation.class, new PixelGridAnimationAdapter())
            .registerTypeAdapter(IdentifierWrapper.class, new IdentifierWrapperAdapter())
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    private final ModConfig localConfig;

    public WalksyLibConfigManager(final ModConfig localConfig) {
        this.localConfig = localConfig;
    }

    public ModConfig get() {
        return this.localConfig;
    }

    public void cleanCache() {
        final Path cacheDir = getCachedImageDir();
        if (!Files.exists(cacheDir)) return;

        final List<String> usedFiles = new ArrayList<>();

        for (final Category category : this.localConfig.categories()) {
            for (final Option<?> option : category.options()) {
                final Object value = option.getValue();
                if (value instanceof IdentifierWrapper wrapper) {
                    final String fileName = wrapper.getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        usedFiles.add(fileName);
                    }
                }
            }

            for (final OptionGroup group : category.optionGroups()) {
                for (final Option<?> option : group.getOptions()) {
                    final Object value = option.getValue();
                    if (value instanceof IdentifierWrapper wrapper) {
                        final String fileName = wrapper.getFileName();
                        if (fileName != null && !fileName.isEmpty()) {
                            usedFiles.add(fileName);
                        }
                    }
                }
            }
        }

        try {
            Files.list(cacheDir).forEach(path -> {
                final String fileName = path.getFileName().toString();
                if (!usedFiles.contains(fileName)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        WalksyLibLogger.err("Failed to delete unused cached image: " + fileName);
                    }
                }
            });
        } catch (IOException e) {
            WalksyLibLogger.err("Failed to list cache directory: " + e.getMessage());
        }
    }


    public static SerializableCategory serializeCategory(final Category category) {
        final SerializableCategory serialized = new SerializableCategory();
        serialized.name = category.name();
        serialized.options = new ArrayList<>();
        serialized.groups = new ArrayList<>();

        for (final Option<?> option : category.options()) {
            if (option.getType() == Runnable.class) continue;
            serialized.options.add(OptionConverter.fromOption(option));
        }

        for (final OptionGroup group : category.optionGroups()) {
            final SerializableGroup serializedGroup = new SerializableGroup();
            serializedGroup.name = group.getName();
            serializedGroup.expanded = group.isExpanded();
            serializedGroup.options = new ArrayList<>();

            for (final Option<?> option : group.getOptions()) {
                if (option.getType() == Runnable.class) continue;
                serializedGroup.options.add(OptionConverter.fromOption(option));
            }

            serialized.groups.add(serializedGroup);
        }

        return serialized;
    }


    public static void applyCategoryValues(final Category category, final SerializableCategory serializedCategory) {
        for (final Option<?> option : category.options()) {
            serializedCategory.options.stream()
                    .filter(serialized -> serialized.name.equals(option.getName()))
                    .findFirst()
                    .ifPresent(serialized -> applyOptionValues(option, serialized));

            final Object value = option.getValue();

            if (value instanceof IdentifierWrapper wrapper) {
                final String fileName = wrapper.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    final Identifier identifier = WalksyLibConfigManager.loadTextureFromCache(fileName);
                    if (identifier != null) {
                        wrapper.setIdentifier(identifier);
                    }
                }
            }
        }

        for (final OptionGroup group : category.optionGroups()) {
            serializedCategory.groups.stream()
                    .filter(serialized -> serialized.name.equals(group.getName()))
                    .findFirst()
                    .ifPresent(serializedGroup -> {
                        group.setExpanded(serializedGroup.expanded);

                        for (final Option<?> option : group.getOptions()) {
                            serializedGroup.options.stream()
                                    .filter(serialized -> serialized.name.equals(option.getName()))
                                    .findFirst()
                                    .ifPresent(serialized -> applyOptionValues(option, serialized));
                        }
                    });

            group.getOptions().forEach(option -> {
                final Object value = option.getValue();

                if (value instanceof IdentifierWrapper wrapper) {
                    final String fileName = wrapper.getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        final Identifier identifier = WalksyLibConfigManager.loadTextureFromCache(fileName);
                        if (identifier != null) {
                            wrapper.setIdentifier(identifier);
                        }
                    }
                }
            });
        }
    }

    public static void applyOptionValues(final Option<?> option, final SerializableOption serialized) {
        try {
            OptionConverter.setOptionValue(option, serialized.value);
        } catch (Exception e) {
            WalksyLibLogger.err("Failed to set value for option '" + option.getName() + "': " + e.getMessage());
        }
    }

    public static Path getCachedImageDir() {
        final Path configDir = FabricLoader.getInstance().getConfigDir();
        final Path destDir = configDir.resolve("WalksyLib").resolve("CachedImages");

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            WalksyLibLogger.err("Failed to create cached image directory: " + e.getMessage());
        }

        return destDir;
    }

    public static Identifier loadTextureFromCache(final String fileName) {
        final Path imagePath = getCachedImageDir().resolve(fileName);
        if (!Files.exists(imagePath)) return null;

        try {
            final byte[] fileBytes = Files.readAllBytes(imagePath);
            if (fileBytes.length == 0) return null;

            final NativeImage image = loadFlexibleImage(fileBytes);
            if (image == null) {
                WalksyLibLogger.err("Failed to decode cached image data for: " + fileName);
                return null;
            }

            final DynamicTexture texture = new DynamicTexture(() -> fileName, image);
            String name = fileName;
            final int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) name = name.substring(0, dotIndex);

            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
            final String dynamicId = "dropped/" + name;
            final Identifier textureId = Identifier.fromNamespaceAndPath("walksylib", dynamicId);

            Minecraft.getInstance().getTextureManager().register(textureId, texture);
            return textureId;
        } catch (Exception e) {
            WalksyLibLogger.err(e.getMessage());
            return null;
        }
    }

    public static NativeImage loadFlexibleImage(final byte[] imageBytes) {
        try {
            return NativeImage.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            try {
                final BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (bufferedImage == null) return null;

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                final byte[] pngBytes = baos.toByteArray();

                return NativeImage.read(new ByteArrayInputStream(pngBytes));
            } catch (Exception ex) {
                return null;
            }
        }
    }

}
