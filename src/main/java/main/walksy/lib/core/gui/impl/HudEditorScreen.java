package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.renderer.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class HudEditorScreen extends BaseScreen {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");
    private final Option<?> hudOption;
    private boolean dragging = false;

    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

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
            Vec2f pos = pixelGridAnimation.getAbsolutePosition();
            float x = pos.x;
            float y = pos.y;

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
                        Math.round(x / size),
                        Math.round(y / size),
                        1,
                        0
                );
                context.getMatrices().pop();
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!(hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.client.getWindow().getScaledWidth() - renderedW) / 2;
        int baseY = (this.client.getWindow().getScaledHeight() - renderedH) / 2;

        double rawX = baseX + pixelGridAnimation.getOffsetX();
        double rawY = baseY + pixelGridAnimation.getOffsetY();

        if (mouseX >= rawX && mouseX <= rawX + renderedW
                && mouseY >= rawY && mouseY <= rawY + renderedH) {
            dragging = true;

            dragOffsetX = mouseX - rawX;
            dragOffsetY = mouseY - rawY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (!(dragging && hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.client.getWindow().getScaledWidth() - renderedW) / 2;
        int baseY = (this.client.getWindow().getScaledHeight() - renderedH) / 2;

        double newTopLeftX = mouseX - dragOffsetX;
        double newTopLeftY = mouseY - dragOffsetY;

        double offsetX = newTopLeftX - baseX;
        double offsetY = newTopLeftY - baseY;

        offsetX = Math.round(offsetX * 2.0) / 2.0;
        offsetY = Math.round(offsetY * 2.0) / 2.0;

        pixelGridAnimation.setOffset(offsetX, offsetY);
        return true;
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        dragging = false;

        if (!(hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.client.getWindow().getScaledWidth() - renderedW) / 2;
        int baseY = (this.client.getWindow().getScaledHeight() - renderedH) / 2;

        double newTopLeftX = mouseX - dragOffsetX;
        double newTopLeftY = mouseY - dragOffsetY;

        double offsetX = newTopLeftX - baseX;
        double offsetY = newTopLeftY - baseY;

        offsetX = Math.round(offsetX * 2.0) / 2.0;
        offsetY = Math.round(offsetY * 2.0) / 2.0;

        pixelGridAnimation.setOffset(offsetX, offsetY);
        hudOption.setValue(pixelGridAnimation);

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {

            double moveAmount = Screen.hasShiftDown() ? 2.5 : 0.5;

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
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}
}
