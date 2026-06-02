package main.walksy.lib.core.gui.widgets;

import com.mojang.blaze3d.platform.NativeImage;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.popup.impl.TextureDropPopUp;
import main.walksy.lib.core.utils.IdentifierWrapper;
import main.walksy.lib.core.utils.log.WalksyLibLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

public class SpriteOptionWidget extends OptionWidget {

    private final Option<IdentifierWrapper> option;
    private final ButtonWidget editTextureButton;
    private NativeImage image;
    private int visibleX, visibleY, visibleWidth, visibleHeight;

    public SpriteOptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<IdentifierWrapper> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.editTextureButton = new ButtonWidget(this.getWidth() - 100, this.getY() + 3, 70, 14, false, "Edit Texture", () -> this.screen.popUp = new TextureDropPopUp(screen, "Texture Editor: " + option.getName(), pass ->
        {
            this.option.setValue(new IdentifierWrapper(pass.identifier(), pass.fileName()));
            this.reCalc();
        }));
        this.reCalc();
    }


    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        this.editTextureButton.extractRenderState(context, mouseX, mouseY, delta);
        if (this.image == null) return;

        final int padding = 6;
        final float scaleX = (float) ((this.getX() * 2.2 - padding) / (float) this.visibleWidth);
        final float scaleY = Math.min(
                (this.getWidth() - padding) / (float) this.visibleWidth,
                (this.getHeight() - padding) / (float) this.visibleHeight
        );

        final float drawX = this.getX() + this.getWidth() - this.visibleWidth * scaleX - padding;
        final float drawY = this.getY() + (this.getHeight() - this.visibleHeight * scaleY) / 2f;

        context.pose().pushMatrix();
        context.pose().scale(scaleX, scaleY);

        context.blit(
                RenderPipelines.GUI_TEXTURED,
                this.option.getValue().getIdentifier(),
                (int) (drawX / scaleX),
                (int) (drawY / scaleY),
                this.visibleX, this.visibleY,
                this.visibleWidth, this.visibleHeight,
                this.image.getWidth(), this.image.getHeight()
        );

        context.pose().popMatrix();

        if (this.isHoveringImage(mouseX, mouseY)) {
            this.setTooltip(Tooltip.create(Component.literal(this.option.getValue().getIdentifier().getNamespace() + ": " + this.option.getValue().getIdentifier().getPath())));
        } else {
            this.setTooltip(null);
        }
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        super.onMouseClick(click, doubled);
        this.editTextureButton.onClick(click, doubled);
    }

    @Override
    public void onWidgetUpdate() {
        this.editTextureButton.setPosition(this.getWidth() - 100, this.getY() + 3);
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

    public boolean isHoveringImage(final double mouseX, final double mouseY) {
        if (this.image == null) return false;

        final int padding = 6;
        final float scale = Math.min(
                (this.getWidth() - padding) / (float) this.visibleWidth,
                (this.getHeight() - padding) / (float) this.visibleHeight
        );

        final float drawX = this.getX() + this.getWidth() - this.visibleWidth * scale - padding;
        final float drawY = this.getY() + (this.getHeight() - this.visibleHeight * scale) / 2f;

        return mouseX >= drawX && mouseX <= drawX + this.visibleWidth * scale &&
                mouseY >= drawY && mouseY <= drawY + this.visibleHeight * scale;
    }


    public void reCalc() {
        final Identifier id = this.option.getValue().getIdentifier();
        final AtomicReference<NativeImage> tempImage = new AtomicReference<>();

        final TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        if (textureManager.getTexture(id) instanceof DynamicTexture nativeTexture) {
            tempImage.set(nativeTexture.getPixels());
        } else {
            final ResourceManager manager = Minecraft.getInstance().getResourceManager();
            manager.getResource(id).ifPresent(resource -> {
                try (final InputStream stream = resource.open()) {
                    tempImage.set(NativeImage.read(stream));
                } catch (IOException e) {
                    WalksyLibLogger.err(e.getMessage());
                }
            });
        }

        this.image = tempImage.get();

        if (this.image != null) {
            int minX = this.image.getWidth();
            int minY = this.image.getHeight();
            int maxX = 0;
            int maxY = 0;

            for (int y1 = 0; y1 < this.image.getHeight(); y1++) {
                for (int x1 = 0; x1 < this.image.getWidth(); x1++) {
                    final int alpha = this.image.getPixel(x1, y1) >>> 24;
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
