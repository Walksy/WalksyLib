package main.walksy.lib.core.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.impl.APIConfig;
import main.walksy.lib.core.config.impl.LocalConfig;
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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
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

    private final LocalConfig localConfig;
    private final APIConfig apiConfig;

    public WalksyLibConfigManager(LocalConfig localConfig) {
        this.localConfig = localConfig;
        this.apiConfig = new APIConfig();
    }

    public LocalConfig getLocal()
    {
        return localConfig;
    }

    public APIConfig getAPI()
    {
        return apiConfig;
    }

    public void cleanCache() {
        Path cacheDir = getCachedImageDir();
        if (!Files.exists(cacheDir)) return;

        List<String> usedFiles = new ArrayList<>();

        for (Category category : this.localConfig.categories()) {
            for (Option<?> option : category.options()) {
                Object value = option.getValue();
                if (value instanceof IdentifierWrapper wrapper) {
                    String fileName = wrapper.getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        usedFiles.add(fileName);
                    }
                }
            }

            for (OptionGroup group : category.optionGroups()) {
                for (Option<?> option : group.getOptions()) {
                    Object value = option.getValue();
                    if (value instanceof IdentifierWrapper wrapper) {
                        String fileName = wrapper.getFileName();
                        if (fileName != null && !fileName.isEmpty()) {
                            usedFiles.add(fileName);
                        }
                    }
                }
            }
        }

        try {
            Files.list(cacheDir).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (!usedFiles.contains(fileName)) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        WalksyLib.getLogger().err("Failed to delete unused cached image: " + fileName);
                    }
                }
            });
        } catch (IOException e) {
            WalksyLib.getLogger().err("Failed to list cache directory: " + e.getMessage());
        }
    }


    public static SerializableCategory serializeCategory(Category category) {
        SerializableCategory serialized = new SerializableCategory();
        serialized.name = category.name();
        serialized.options = new ArrayList<>();
        serialized.groups = new ArrayList<>();

        for (Option<?> option : category.options()) {
            if (option.getType() == Runnable.class) continue;
            serialized.options.add(OptionConverter.fromOption(option));
        }

        for (OptionGroup group : category.optionGroups()) {
            SerializableGroup serializedGroup = new SerializableGroup();
            serializedGroup.name = group.getName();
            serializedGroup.expanded = group.isExpanded();
            serializedGroup.options = new ArrayList<>();

            for (Option<?> option : group.getOptions()) {
                if (option.getType() == Runnable.class) continue;
                serializedGroup.options.add(OptionConverter.fromOption(option));
            }

            serialized.groups.add(serializedGroup);
        }

        return serialized;
    }


    public static void applyCategoryValues(Category category, SerializableCategory serializedCategory) {
        for (Option<?> option : category.options()) {
            serializedCategory.options.stream()
                    .filter(serialized -> serialized.name.equals(option.getName()))
                    .findFirst()
                    .ifPresent(serialized -> applyOptionValues(option, serialized));

            Object value = option.getValue();

            if (value instanceof IdentifierWrapper wrapper) {
                String fileName = wrapper.getFileName();
                if (fileName != null && !fileName.isEmpty()) {
                    Identifier identifier = WalksyLibConfigManager.loadTextureFromCache(fileName);
                    if (identifier != null) {
                        wrapper.setIdentifier(identifier);
                    }
                }
            }
        }

        for (OptionGroup group : category.optionGroups()) {
            serializedCategory.groups.stream()
                    .filter(serialized -> serialized.name.equals(group.getName()))
                    .findFirst()
                    .ifPresent(serializedGroup -> {
                        group.setExpanded(serializedGroup.expanded);

                        for (Option<?> option : group.getOptions()) {
                            serializedGroup.options.stream()
                                    .filter(serialized -> serialized.name.equals(option.getName()))
                                    .findFirst()
                                    .ifPresent(serialized -> applyOptionValues(option, serialized));
                        }
                    });

            group.getOptions().forEach(option -> {
                Object value = option.getValue();

                if (value instanceof IdentifierWrapper wrapper) {
                    String fileName = wrapper.getFileName();
                    if (fileName != null && !fileName.isEmpty()) {
                        Identifier identifier = WalksyLibConfigManager.loadTextureFromCache(fileName);
                        if (identifier != null) {
                            wrapper.setIdentifier(identifier);
                        }
                    }
                }
            });
        }


    }

    public static void applyOptionValues(Option<?> option, SerializableOption serialized) {
        try {
            OptionConverter.setOptionValue(option, serialized.value);
        } catch (Exception e) {
            System.err.println("Failed to set value for option '" + option.getName() + "': " + e.getMessage());
            return;
        }

        /*
        option.setRainbow(serialized.rainbow);
        option.setHue(serialized.hue);
        option.setSaturation(serialized.saturation);
        option.setBrightness(serialized.brightness);
        option.setAlpha(serialized.alpha);
        option.setRainbowSpeed(serialized.rainbowSpeed);
        option.setPulseSpeed(serialized.pulseSpeed);
        option.setPulse(serialized.pulse);

         */
    }

    public static Path getCachedImageDir() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path destDir = configDir.resolve("WalksyLib").resolve("CachedImages");

        try {
            Files.createDirectories(destDir);
        } catch (IOException e) {
            System.err.println("Failed to create cached image directory: " + e.getMessage());
        }

        return destDir;
    }

    public static Identifier loadTextureFromCache(String fileName) {
        Path imagePath = getCachedImageDir().resolve(fileName);
        if (!Files.exists(imagePath)) return null;

        try (InputStream stream = Files.newInputStream(imagePath)) {
            NativeImage image = NativeImage.read(stream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

            String name = fileName;
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) name = name.substring(0, dotIndex);

            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
            String dynamicId = "dropped/" + name;
            Identifier textureId = Identifier.of("walksylib", dynamicId);

            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
            return textureId;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
