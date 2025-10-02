package main.walksy.lib.core.mods;

import com.terraformersmc.modmenu.gui.widget.entries.ModListEntry;
import main.walksy.lib.core.WalksyLib;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class Mod {
    private final ModContainer container;
    private final UnaryOperator<Screen> configScreen;
    private final Identifier modIcon;

    public Mod(ModContainer container, UnaryOperator<Screen> configScreen)
    {
        this.container = container;
        this.configScreen = configScreen;

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
                        WalksyLib.getLogger().err("Failed to load icon from mod jar: " + " " + path + " " + e);
                        return Optional.empty();
                    }
                })
                .map(tex -> {
                    MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, tex);
                    return identifier;
                })
                .orElse(ModListEntry.UNKNOWN_ICON /*-> From modmenu */);
    }

    public ModContainer getContainer()
    {
        return this.container;
    }

    public Screen getConfigScreen(Screen parent)
    {
        return this.configScreen.apply(parent);
    }

    public Identifier getModIcon()
    {
        return this.modIcon;
    }
}
