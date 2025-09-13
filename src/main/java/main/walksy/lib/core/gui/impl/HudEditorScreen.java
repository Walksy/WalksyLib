package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.WalksyLib;
import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.awt.*;

public class HudEditorScreen extends BaseScreen {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");

    private final Option<?> hudOption;
    private boolean dragging = false;
    private int dragOffsetX = 0, dragOffsetY = 0;

    public HudEditorScreen(Screen parent, Option<?> option) {
        super("HudEditor", parent);
        this.hudOption = option;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, "Currently Editing: " + this.hudOption.getName(), this.width / 2, 10, -1);
        context.drawCenteredTextWithShadow(this.textRenderer, "(ESC to leave)", this.width / 2, 20, Color.GRAY.getRGB());
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Point pos = pixelGridAnimation.getAbsolutePosition();
            int x = pos.x;
            int y = pos.y;

            context.fill(0, 0, width, height, Color.BLACK.getRGB());

            if (this.client.world == null)
            {
                context.drawGuiTexture(RenderLayer::getCrosshair, CROSSHAIR_TEXTURE, (context.getScaledWindowWidth() - 15) / 2, (context.getScaledWindowHeight() - 15) / 2, 15, 15);
            }

            pixelGridAnimation.render(context);
            if (!dragging) {
                WalksyLib.getInstance().get2DRenderer().renderGridOutline(
                        context,
                        pixelGridAnimation.getCurrentFrame(),
                        x,
                        y,
                        1,
                        0
                );
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Point pos = pixelGridAnimation.getAbsolutePosition();
            int gridWidth = pixelGridAnimation.getCurrentFrame().getWidth();
            int gridHeight = pixelGridAnimation.getCurrentFrame().getHeight();

            if (mouseX >= pos.x && mouseX <= pos.x + gridWidth
                    && mouseY >= pos.y && mouseY <= pos.y + gridHeight) {
                dragging = true;
                dragOffsetX = (int) mouseX - pos.x;
                dragOffsetY = (int) mouseY - pos.y;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            pixelGridAnimation.setPosition(newX, newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;

            if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;
                pixelGridAnimation.setPosition(newX, newY);

                hudOption.setValue(pixelGridAnimation);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Point pos = pixelGridAnimation.getAbsolutePosition();
            pixelGridAnimation.setPosition(pos.x, pos.y);
            hudOption.setValue(pixelGridAnimation);
        }
        super.close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client.world == null) {
            //this.renderPanoramaBackground(context, delta);
        }
    }
}
