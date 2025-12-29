package main.walksy.lib.core.mods;

import main.walksy.lib.core.config.impl.LocalConfig;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;

public class Mod {
    private final ModContainer container;
    private final Identifier modIcon;
    private final LocalConfig config;

    public Mod(ModContainer container, LocalConfig config)
    {
        this.container = container;
        this.config = config;
        /**
         * Credit to uku for this code:
         * https://github.com/uku3lig/ukulib/blob/de3c36f921f3dba6401601eb05912337d2c602ee/src/main/java/net/uku3lig/ukulib/config/impl/EntrypointList.java#L67
         */
        Identifier identifier = Identifier.of("walksylib", this.getContainer().getMetadata().getId() + "_icon");
        final int ICON_SIZE = 32;
        this.modIcon = this.getContainer().getMetadata().getIconPath(ICON_SIZE)
                .flatMap(this.getContainer()::findPath)
                .flatMap(path -> {
                    try (InputStream inputStream = Files.newInputStream(path)) {
                        NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));

                        return Optional.of(new NativeImageBackedTexture(image));
                    } catch (IOException e) {
                        WalksyLibLogger.err("Failed to load icon from mod jar: " + " " + path + " " + e);
                        return Optional.empty();
                    }
                })
                .map(tex -> {
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, tex);
                    return identifier;
                })
                .orElse(Identifier.ofVanilla("textures/misc/unknown_pack.png"));
    }

    public ModContainer getContainer()
    {
        return this.container;
    }

    public LocalConfig getConfig() {
        return this.config;
    }

    public Identifier getModIcon()
    {
        return this.modIcon;
    }
}
