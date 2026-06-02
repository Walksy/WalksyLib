package main.walksy.lib.core.gui.popup.impl;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.callback.WalksyLibDropCallback;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public class TextureDropPopUp extends PopUp {

    private final ButtonWidget doneButton;
    private Identifier selectedTexture;
    private String fileName;

    public record Pass(Identifier identifier, String fileName) {}

    public TextureDropPopUp(final WalksyLibConfigScreen parent, final String subText, final Consumer<Pass> onDone) {
        super(parent, subText, 280, 320);
        this.doneButton = new ButtonWidget(
                this.x + this.width - 51,
                this.y + this.height - 21,
                40,
                16,
                false,
                "Done",
                () -> {
                    if (this.selectedTexture != null && onDone != null) {
                        onDone.accept(new Pass(this.selectedTexture, this.fileName));
                    }
                    parent.popUp.close();
                });

        WalksyLibDropCallback.register(this::onFileDropped);
    }

    @Override
    public void render(final GuiGraphicsExtractor context, final double mouseX, final double mouseY, final float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.centeredText(this.parent.getFont(), this.subText, this.x + this.width / 2, this.y + 10, -1);
        context.horizontalLine(this.x + 2, this.x + this.width - 3, this.y + 23, MainColors.OUTLINE_WHITE.getRGB());

        if (this.selectedTexture != null) {
            final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            final DynamicTexture nativeTexture = (DynamicTexture) textureManager.getTexture(this.selectedTexture);

            if (nativeTexture != null) {
                final NativeImage image = nativeTexture.getPixels();
                if (image != null) {
                    final int imgW = image.getWidth();
                    final int imgH = image.getHeight();

                    final int maxWidth = this.width - 40;
                    final int maxHeight = this.height - 100;

                    final float scale = Math.min((float) maxWidth / imgW, (float) maxHeight / imgH);

                    final int scaledWidth = Math.round(imgW * scale);
                    final int scaledHeight = Math.round(imgH * scale);

                    final int drawX = this.x + this.width / 2 - scaledWidth / 2;
                    final int drawY = this.y + this.height / 2 - scaledHeight / 2;

                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            this.selectedTexture,
                            drawX,
                            drawY,
                            0f,
                            0f,
                            scaledWidth,
                            scaledHeight,
                            scaledWidth,
                            scaledHeight
                    );
                }
            }
        } else {
            context.centeredText(this.parent.getFont(), "Drop a .png image file", this.x + this.width / 2, this.y + this.height / 2, CommonColors.GRAY);
        }

        this.doneButton.extractRenderState(context, (int) mouseX, (int) mouseY, delta);
    }

    @Override
    public void onClick(final MouseButtonEvent click, final boolean doubled) {
        this.doneButton.onClick(click, doubled);
    }

    private void onFileDropped(final String filePath) {
        final File file = new File(filePath);
        WalksyLibLogger.info("File dropped: " + filePath);
        if (!file.exists() || !file.isFile()) return;

        String name = file.getName();
        final String trueName = name;
        final String lowerName = name.toLowerCase();

        if (!lowerName.matches(".*\\.(png|jpg|jpeg|bmp|webp|gif)$")) {
            WalksyLibLogger.err("Unsupported file extension: " + name);
            return;
        }

        try {
            if (file.length() == 0) {
                WalksyLibLogger.err("Dropped file is 0 bytes. It might still be downloading!");
                return;
            }

            final byte[] fileBytes = Files.readAllBytes(file.toPath());

            final int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }
            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");

            final NativeImage image = this.loadFlexibleImage(fileBytes);
            if (image == null) {
                WalksyLibLogger.err("Failed to decode image data for: " + trueName);
                return;
            }

            final DynamicTexture texture = new DynamicTexture(() -> filePath, image);
            final String dynamicId = "dropped/" + name;
            final Identifier textureId = Identifier.fromNamespaceAndPath("walksylib", dynamicId);

            Minecraft.getInstance().getTextureManager().release(textureId);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);

            final Path destDir = WalksyLibConfigManager.getCachedImageDir();
            final Path destPath = destDir.resolve(trueName);
            Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            this.selectedTexture = textureId;
            this.fileName = trueName;

        } catch (Exception e) {
            WalksyLibLogger.err("Failed to process dropped file: " + filePath + " " + e.getMessage());
        }
    }

    private NativeImage loadFlexibleImage(final byte[] imageBytes) {
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

    @Override
    public void layout(final int requestedWidth, final int requestedHeight) {
        super.layout(requestedWidth, requestedHeight);
        if (this.doneButton != null) {
            this.doneButton.setPosition(this.x + this.width - 51, this.y + this.height - 21);
        }
    }

    @Override
    protected void onClose() {
        WalksyLibDropCallback.unregister();
    }
}
