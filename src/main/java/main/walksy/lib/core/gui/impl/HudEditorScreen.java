package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.renderer.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

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

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "Currently Editing: " + this.hudOption.getName(),
                this.width / 2,
                10,
                -1
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                "(ESC to leave)",
                this.width / 2,
                20,
                Color.GRAY.getRGB()
        );

        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Point pos = pixelGridAnimation.getAbsolutePosition();
            int x = pos.x;
            int y = pos.y;

            if (this.client.world == null) {
                context.fill(0, 0, width, height, Color.BLACK.getRGB());
                context.drawGuiTexture(
                        RenderLayer::getCrosshair,
                        CROSSHAIR_TEXTURE,
                        (context.getScaledWindowWidth() - 15) / 2,
                        (context.getScaledWindowHeight() - 15) / 2,
                        15,
                        15
                );
            }

            pixelGridAnimation.render(context, false);

            if (!dragging) {
                context.getMatrices().push();
                float size = pixelGridAnimation.getSize();
                context.getMatrices().scale(size, size, size);
                Renderer2D.renderGridOutline(
                        context,
                        pixelGridAnimation.getCurrentFrame(),
                        (int) (x / size),
                        (int) (y / size),
                        1,
                        0
                );
                context.getMatrices().pop();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Point pos = pixelGridAnimation.getAbsolutePosition();
            float size = pixelGridAnimation.getSize();
            int gridWidth = (int) (pixelGridAnimation.getCurrentFrame().getWidth() * size);
            int gridHeight = (int) (pixelGridAnimation.getCurrentFrame().getHeight() * size);

            if (mouseX >= pos.x && mouseX <= pos.x + gridWidth
                    && mouseY >= pos.y && mouseY <= pos.y + gridHeight) {
                dragging = true;
                dragOffsetX = (int) (mouseX - pos.x);
                dragOffsetY = (int) (mouseY - pos.y);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            int screenWidth = this.client.getWindow().getScaledWidth();
            int screenHeight = this.client.getWindow().getScaledHeight();

            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            int centerX = (screenWidth / 2) - 8;
            int centerY = (screenHeight / 2) - 8;
            int offsetX = newX - centerX;
            int offsetY = newY - centerY;

            pixelGridAnimation.setOffset(offsetX, offsetY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;

            if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
                int screenWidth = this.client.getWindow().getScaledWidth();
                int screenHeight = this.client.getWindow().getScaledHeight();

                int newX = (int) mouseX - dragOffsetX;
                int newY = (int) mouseY - dragOffsetY;

                int centerX = (screenWidth / 2) - 8;
                int centerY = (screenHeight / 2) - 8;

                int offsetX = newX - centerX;
                int offsetY = newY - centerY;

                pixelGridAnimation.setOffset(offsetX, offsetY);
                hudOption.setValue(pixelGridAnimation);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            int moveAmount = Screen.hasShiftDown() ? 5 : 1;

            switch (keyCode) {
                case GLFW.GLFW_KEY_UP -> pixelGridAnimation.addOffset(0, -moveAmount);
                case GLFW.GLFW_KEY_DOWN -> pixelGridAnimation.addOffset(0, moveAmount);
                case GLFW.GLFW_KEY_LEFT -> pixelGridAnimation.addOffset(-moveAmount, 0);
                case GLFW.GLFW_KEY_RIGHT -> pixelGridAnimation.addOffset(moveAmount, 0);
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }

            hudOption.setValue(pixelGridAnimation);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public void close() {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            hudOption.setValue(pixelGridAnimation);
        }
        super.close();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client.world == null) {
        }
    }
}
