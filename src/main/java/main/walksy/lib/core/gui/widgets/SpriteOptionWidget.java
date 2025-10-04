package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.TextureDropPopUp;
import main.walksy.lib.core.utils.IdentifierWrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class SpriteOptionWidget extends OptionWidget {

    private final Option<IdentifierWrapper> option;
    private final ButtonWidget editTextureButton;
    private NativeImage image;
    private int visibleX, visibleY, visibleWidth, visibleHeight;

    public SpriteOptionWidget(OptionGroup parent, WalksyLibConfigScreen screen, int x, int y, int width, int height, Option<IdentifierWrapper> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.editTextureButton = new ButtonWidget(getWidth() - 100, getY() + 3, 70, 14, false, "Edit Texture", () -> this.screen.popUp = new TextureDropPopUp(screen, "Texture Editor: " + option.getName(), pass ->
        {
            this.option.setValue(new IdentifierWrapper(pass.identifier(), pass.fileName()));
            this.reCalc();
        }));
        this.reCalc();
    }


    @Override
    public void draw(DrawContext context, int mouseX, int mouseY, float delta) {
        this.editTextureButton.render(context, mouseX, mouseY, delta);
        if (image == null) return;

        int padding = 6;
        float scaleX = (float) ((getX() * 2.2 - padding) / (float) visibleWidth); //this will definitely cause some issues in the future
        float scaleY = Math.min(
                (getWidth() - padding) / (float) visibleWidth,
                (getHeight() - padding) / (float) visibleHeight
        );

        float drawX = getX() + getWidth() - visibleWidth * scaleX - padding;
        float drawY = getY() + (getHeight() - visibleHeight * scaleY) / 2f;

        context.getMatrices().push();
        context.getMatrices().scale(scaleX, scaleY, 1f);

        context.drawTexture(
                RenderLayer::getGuiTextured,
                option.getValue().getIdentifier(),
                (int)(drawX / scaleX),
                (int)(drawY / scaleY),
                visibleX, visibleY,
                visibleWidth, visibleHeight,
                image.getWidth(), image.getHeight()
        );

        context.getMatrices().pop();

        if (isHoveringImage(mouseX, mouseY))
        {
            this.setTooltip(Tooltip.of(Text.of(this.option.getValue().getIdentifier().getNamespace() + ": " + this.option.getValue().getIdentifier().getPath())));
        } else {
            this.setTooltip(null);
        }
    }

    @Override
    public void onMouseClick(double mouseX, double mouseY, int button) {
        super.onMouseClick(mouseX, mouseY, button);
        this.editTextureButton.onClick(mouseX, mouseY);
    }

    @Override
    public void onWidgetUpdate() {
        this.editTextureButton.setPosition(getWidth() - 100, getY() + 3);
    }

    @Override
    protected void handleResetButtonClick() {
        super.handleResetButtonClick();
        this.reCalc();
    }

    @Override
    public boolean isHovered() {
        return false;
    }

    public boolean isHoveringImage(double mouseX, double mouseY) {
        if (image == null) return false;

        int padding = 6;
        float scale = Math.min(
                (getWidth() - padding) / (float) visibleWidth,
                (getHeight() - padding) / (float) visibleHeight
        );

        float drawX = getX() + getWidth() - visibleWidth * scale - padding;
        float drawY = getY() + (getHeight() - visibleHeight * scale) / 2f;

        return mouseX >= drawX && mouseX <= drawX + visibleWidth * scale &&
                mouseY >= drawY && mouseY <= drawY + visibleHeight * scale;
    }


    public void reCalc() {
        Identifier id = option.getValue().getIdentifier();
        AtomicReference<NativeImage> tempImage = new AtomicReference<>();

        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        if (textureManager.getTexture(id) instanceof NativeImageBackedTexture nativeTexture) {
            tempImage.set(nativeTexture.getImage());
        } else {
            ResourceManager manager = MinecraftClient.getInstance().getResourceManager();
            manager.getResource(id).ifPresent(resource -> {
                try (InputStream stream = resource.getInputStream()) {
                    tempImage.set(NativeImage.read(stream));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        this.image = tempImage.get();

        if (image != null) {
            int minX = image.getWidth();
            int minY = image.getHeight();
            int maxX = 0;
            int maxY = 0;

            for (int y1 = 0; y1 < image.getHeight(); y1++) {
                for (int x1 = 0; x1 < image.getWidth(); x1++) {
                    int alpha = image.getColorArgb(x1, y1) >>> 24;
                    if (alpha != 0) {
                        if (x1 < minX) minX = x1;
                        if (y1 < minY) minY = y1;
                        if (x1 > maxX) maxX = x1;
                        if (y1 > maxY) maxY = y1;
                    }
                }
            }

            if (minX <= maxX && minY <= maxY) {
                this.visibleX = minX;
                this.visibleY = minY;
                this.visibleWidth = maxX - minX + 1;
                this.visibleHeight = maxY - minY + 1;
            } else {
                this.visibleX = 0;
                this.visibleY = 0;
                this.visibleWidth = 1;
                this.visibleHeight = 1;
            }
        } else {
            this.visibleX = 0;
            this.visibleY = 0;
            this.visibleWidth = 1;
            this.visibleHeight = 1;
        }
    }

}
