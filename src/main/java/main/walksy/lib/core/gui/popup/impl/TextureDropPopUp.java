package main.walksy.lib.core.gui.popup.impl;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.callback.WindowDropCallback;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
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

    public TextureDropPopUp(WalksyLibConfigScreen parent, String subText, Consumer<Pass> onDone) {
        super(parent, subText, 280, 320);
        this.doneButton = new ButtonWidget(
                x + width - 51,
                y + height - 21,
                40,
                16,
                false,
                "Done",
                () -> {
                    if (this.selectedTexture != null && onDone != null) {
                        onDone.accept(new Pass(this.selectedTexture, fileName));
                    }
                    parent.popUp.close();
                });

        WindowDropCallback.register(this::onFileDropped);
    }

    @Override
    public void render(GuiGraphicsExtractor context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.centeredText(parent.getFont(), subText, x + width / 2, y + 10, -1);
        context.horizontalLine(x + 2, x + width - 3, y + 23, MainColors.OUTLINE_WHITE.getRGB());

        if (selectedTexture != null) {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            DynamicTexture nativeTexture = (DynamicTexture) textureManager.getTexture(selectedTexture);

            if (nativeTexture != null) {
                NativeImage image = nativeTexture.getPixels();
                if (image != null) {
                    int imgW = image.getWidth();
                    int imgH = image.getHeight();

                    int maxWidth = width - 40;
                    int maxHeight = height - 100;

                    float scale = Math.min((float) maxWidth / imgW, (float) maxHeight / imgH);

                    int scaledWidth = Math.round(imgW * scale);
                    int scaledHeight = Math.round(imgH * scale);

                    int drawX = x + width / 2 - scaledWidth / 2;
                    int drawY = y + height / 2 - scaledHeight / 2;

                    context.blit(
                            RenderPipelines.GUI_TEXTURED,
                            selectedTexture,
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
            context.centeredText(parent.getFont(), "Drop a .png image file", x + width / 2, y + height / 2, CommonColors.GRAY);
        }

        this.doneButton.extractRenderState(context, (int) mouseX, (int) mouseY, delta);
    }



    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        this.doneButton.onClick(click, doubled);
    }


    private void onFileDropped(String filePath) {
        File file = new File(filePath);
        System.out.println("File dropped: " + filePath);
        if (!file.exists() || !file.isFile()) return;

        String name = file.getName();
        String trueName = name;
        String lowerName = name.toLowerCase();

        if (!lowerName.matches(".*\\.(png|jpg|jpeg|bmp|webp|gif)$")) {
            System.err.println("Unsupported file extension: " + name);
            return;
        }

        try {
            if (file.length() == 0) {
                System.err.println("Dropped file is 0 bytes. It might still be downloading!");
                return;
            }

            byte[] fileBytes = Files.readAllBytes(file.toPath());

            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }
            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");

            NativeImage image = loadFlexibleImage(fileBytes);
            if (image == null) {
                System.err.println("Failed to decode image data for: " + trueName);
                return;
            }

            DynamicTexture texture = new DynamicTexture(() -> filePath, image);
            String dynamicId = "dropped/" + name;
            Identifier textureId = Identifier.fromNamespaceAndPath("walksylib", dynamicId);

            Minecraft.getInstance().getTextureManager().release(textureId);
            Minecraft.getInstance().getTextureManager().register(textureId, texture);

            Path destDir = WalksyLibConfigManager.getCachedImageDir();
            Path destPath = destDir.resolve(trueName);
            Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

            this.selectedTexture = textureId;
            this.fileName = trueName;

        } catch (Exception e) {
            System.err.println("Failed to process dropped file: " + filePath);
            e.printStackTrace();
        }
    }

    private NativeImage loadFlexibleImage(byte[] imageBytes) {
        try {
            return NativeImage.read(new ByteArrayInputStream(imageBytes));
        } catch (Exception e) {
            try {
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
                if (bufferedImage == null) return null;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] pngBytes = baos.toByteArray();

                return NativeImage.read(new ByteArrayInputStream(pngBytes));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Override
    public void layout(int requestedWidth, int requestedHeight) {
        super.layout(requestedWidth, requestedHeight);
        if (this.doneButton != null) {
            this.doneButton.setPosition(x + width - 51, y + height - 21);
        }
    }

    @Override
    protected void onClose() {
        WindowDropCallback.unregister();
    }
}
