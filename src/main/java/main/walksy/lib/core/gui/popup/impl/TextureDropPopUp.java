package main.walksy.lib.core.gui.popup.impl;

import main.walksy.lib.core.callback.WindowDropCallback;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.PopUp;
import main.walksy.lib.core.gui.widgets.ButtonWidget;
import main.walksy.lib.core.manager.WalksyLibConfigManager;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

        WindowDropCallback.register(MinecraftClient.getInstance().getWindow().getHandle(), this::onFileDropped);
    }

    @Override
    public void render(DrawContext context, double mouseX, double mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(parent.getTextRenderer(), subText, x + width / 2, y + 10, -1);
        context.drawHorizontalLine(x + 2, x + width - 3, y + 23, MainColors.OUTLINE_WHITE.getRGB());

        if (selectedTexture != null) {
            TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
            NativeImageBackedTexture nativeTexture = (NativeImageBackedTexture) textureManager.getTexture(selectedTexture);

            if (nativeTexture != null) {
                NativeImage image = nativeTexture.getImage();
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

                    context.drawTexture(
                            RenderLayer::getGuiTextured,
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
            context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Drop a .png image file", x + width / 2, y + height / 2, 0xAAAAAA);
        }

        this.doneButton.render(context, (int) mouseX, (int) mouseY, delta);
    }



    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.doneButton.onClick(mouseX, mouseY);
    }


    private void onFileDropped(String filePath) { //TODO TEST
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) return;

        String name = file.getName();
        String trueName = name;
        if (!name.toLowerCase().endsWith(".png")) return;

        try {
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                name = name.substring(0, dotIndex);
            }

            name = name.toLowerCase().replaceAll("[^a-z0-9._-]", "_");

            try (InputStream stream = new FileInputStream(file)) {
                NativeImage image = NativeImage.read(stream);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);

                String dynamicId = "dropped/" + name;
                Identifier textureId = Identifier.of("walksylib", dynamicId);
                MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);

                Path destDir = WalksyLibConfigManager.getCachedImageDir();
                Path destPath = destDir.resolve(trueName);
                Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                this.selectedTexture = textureId;
                this.fileName = trueName;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
