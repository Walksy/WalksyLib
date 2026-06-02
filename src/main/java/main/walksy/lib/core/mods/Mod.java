package main.walksy.lib.core.mods;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.config.impl.ModConfig;
import main.walksy.lib.core.gui.impl.BaseScreen;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class Mod {
    private final ModContainer container;
    private final Identifier modIcon;
    private final ModConfig config;
    private final Function<Screen, BaseScreen> overridableScreenFactory;
    private final String[] conflictedButtonTitles;

    public Mod(final ModContainer container, final ModConfig config, final Function<Screen, BaseScreen> overridableScreenFactory, final String[] conflictedButtonTitles) {
        this.container = container;
        this.config = config;
        this.overridableScreenFactory = overridableScreenFactory;
        this.conflictedButtonTitles = conflictedButtonTitles;
        /**
         * Credit to uku for this code:
         * https://github.com/uku3lig/ukulib/blob/de3c36f921f3dba6401601eb05912337d2c602ee/src/main/java/net/uku3lig/ukulib/config/impl/EntrypointList.java#L67
         */
        final Identifier identifier = Identifier.fromNamespaceAndPath("walksylib", this.getContainer().getMetadata().getId() + "_icon");
        final int ICON_SIZE = 32;
        this.modIcon = this.getContainer().getMetadata().getIconPath(ICON_SIZE)
                .flatMap(this.getContainer()::findPath)
                .flatMap(path -> {
                    try (final InputStream inputStream = Files.newInputStream(path)) {
                        final NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));

                    return Optional.of(new DynamicTexture(identifier::toString, image));
                    } catch (IOException e) {
                        WalksyLibLogger.err("Failed to load icon from mod jar: " + " " + path + " " + e);
                        return Optional.empty();
                    }
                })
                .map(tex -> {
                    Minecraft.getInstance().getTextureManager().register(identifier, tex);
                    return identifier;
                })
                .orElse(Identifier.withDefaultNamespace("textures/misc/unknown_pack.png"));
    }

    public ModContainer getContainer() {
        return this.container;
    }

    public ModConfig getConfig() {
        return this.config;
    }

    public BaseScreen getOverridableConfigScreen(final Screen parent) {
        return this.overridableScreenFactory.apply(parent);
    }

    public String[] getConflictedButtonTitles() {
        return this.conflictedButtonTitles;
    }

    public boolean hasConfig() {
        return this.config != null;
    }

    public Identifier getModIcon() {
        return this.modIcon;
    }
}
