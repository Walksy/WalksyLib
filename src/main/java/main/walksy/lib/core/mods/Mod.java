package main.walksy.lib.core.mods;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.api.config.Config;
import main.walksy.lib.core.mods.remote.ModSuggestion;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

public final class Mod {
    private final ModContainer container;
    private final Supplier<Config> config;
    private final String displayName;
    private final ModSuggestion suggestion;
    private final Identifier iconIdentifier;
    private final Path iconPath;
    private Identifier modIcon;

    public Mod(final ModContainer container, final Config config) {
        this(container, config, null);
    }

    public Mod(final ModContainer container, final Config config, final String displayName) {
        this(container, () -> config, displayName);
    }

    public static Mod ofDynamicConfig(final ModContainer container, final Supplier<Config> config, final String displayName) {
        return new Mod(container, config, displayName);
    }

    public Mod(final ModSuggestion suggestion) {
        this.container = null;
        this.config = null;
        this.displayName = suggestion.title();
        this.suggestion = suggestion;
        this.iconIdentifier = null;
        this.iconPath = null;
    }

    private Mod(final ModContainer container, final Supplier<Config> config, final String displayName) {
        this.container = container;
        this.config = config;
        this.displayName = displayName;
        this.suggestion = null;
        this.iconIdentifier = Identifier.fromNamespaceAndPath("walksylib", container.getMetadata().getId() + "_icon");
        this.iconPath = container.getMetadata().getIconPath(32).flatMap(container::findPath).orElse(null);
    }

    public String getDisplayName() {
        if (this.displayName != null) {
            return this.displayName;
        }
        return this.container == null ? "" : this.container.getMetadata().getName();
    }

    public boolean isLibraryPage() {
        return this.suggestion == null && this.displayName != null;
    }

    public boolean isSuggestion() {
        return this.suggestion != null;
    }

    public ModSuggestion getSuggestion() {
        return this.suggestion;
    }

    public ModContainer getContainer() {
        return this.container;
    }

    public Config getConfig() {
        return this.config == null ? null : this.config.get();
    }

    public boolean hasConfig() {
        return this.getConfig() != null;
    }

    public Identifier getModIcon() {
        if (this.suggestion != null) {
            return WalksyLib.getInstance().getSuggestedMods().icon(this.suggestion);
        }
        if (this.modIcon != null) {
            return this.modIcon;
        }
        if (this.iconPath != null) {
            try (final InputStream inputStream = Files.newInputStream(this.iconPath)) {
                final NativeImage image = NativeImage.read(Objects.requireNonNull(inputStream));
                final DynamicTexture tex = new DynamicTexture(this.iconIdentifier::toString, image);
                Minecraft.getInstance().getTextureManager().register(this.iconIdentifier, tex);
                this.modIcon = this.iconIdentifier;
            } catch (final IOException e) {
                WalksyLib.getLogger().err("Failed to load icon from mod jar: " + this.iconPath + " " + e);
                this.modIcon = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
            }
        } else {
            this.modIcon = Identifier.withDefaultNamespace("textures/misc/unknown_pack.png");
        }
        return this.modIcon;
    }
}
