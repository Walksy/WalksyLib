package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.renderer.Renderer2D;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class HudEditorScreen extends BaseScreen {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.withDefaultNamespace("hud/crosshair");
    private final Option<?> hudOption;
    private boolean dragging = false;

    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    public HudEditorScreen(Screen parent, Option<?> option) {
        super("HudEditor", parent);
        this.hudOption = option;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float a) {
        super.extractRenderState(context, mouseX, mouseY, a);
        context.centeredText(
                this.font,
                "Currently Editing: " + this.hudOption.getName(),
                this.width / 2,
                10,
                -1
        );
        context.centeredText(
                this.font,
                "(ESC to leave)",
                this.width / 2,
                20,
                Color.GRAY.getRGB()
        );

        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            Vec2 pos = pixelGridAnimation.getAbsolutePosition();
            double x = pos.x;
            double y = pos.y;

            if (this.minecraft.level == null) {
                context.fill(0, 0, width, height, Color.BLACK.getRGB());
                context.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        CROSSHAIR_TEXTURE,
                        (context.guiWidth() - 15) / 2,
                        (context.guiHeight() - 15) / 2,
                        15,
                        15
                );
            }

            pixelGridAnimation.render(context, false);

            if (!dragging) {
                context.pose().pushMatrix();
                float size = pixelGridAnimation.getSize();
                context.pose().scale(size, size);
                Renderer2D.renderGridOutline(
                        context,
                        pixelGridAnimation.getCurrentFrame(),
                        (int) Math.round(x / size),
                        (int) Math.round(y / size),
                        1,
                        0
                );

                context.pose().popMatrix();
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (!(hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseClicked(click, doubled);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.minecraft.getWindow().getGuiScaledWidth() - renderedW) / 2;
        int baseY = (this.minecraft.getWindow().getGuiScaledHeight() - renderedH) / 2;

        double rawX = baseX + pixelGridAnimation.getOffsetX();
        double rawY = baseY + pixelGridAnimation.getOffsetY();

        if (mouseX >= rawX && mouseX <= rawX + renderedW
                && mouseY >= rawY && mouseY <= rawY + renderedH) {
            dragging = true;

            dragOffsetX = mouseX - rawX;
            dragOffsetY = mouseY - rawY;
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double oX, double oY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (!(dragging && hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseDragged(click, oX, oY);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.minecraft.getWindow().getGuiScaledWidth() - renderedW) / 2;
        int baseY = (this.minecraft.getWindow().getGuiScaledHeight() - renderedH) / 2;

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
    public boolean mouseReleased(MouseButtonEvent click) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (!dragging) {
            return super.mouseReleased(click);
        }

        dragging = false;

        if (!(hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseReleased(click);
        }

        PixelGrid frame = pixelGridAnimation.getCurrentFrame();
        int frameW = frame == null ? 0 : frame.getWidth();
        int frameH = frame == null ? 0 : frame.getHeight();
        float size = pixelGridAnimation.getSize();

        int renderedW = Math.round(frameW * size);
        int renderedH = Math.round(frameH * size);

        int baseX = (this.minecraft.getWindow().getGuiScaledWidth() - renderedW) / 2;
        int baseY = (this.minecraft.getWindow().getGuiScaledHeight() - renderedH) / 2;

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
    public boolean keyPressed(KeyEvent input) {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            double moveAmount = 0.5;

            switch (input.key()) {
                case GLFW.GLFW_KEY_UP -> pixelGridAnimation.addOffset(0, -moveAmount);
                case GLFW.GLFW_KEY_DOWN -> pixelGridAnimation.addOffset(0, moveAmount);
                case GLFW.GLFW_KEY_LEFT -> pixelGridAnimation.addOffset(-moveAmount, 0);
                case GLFW.GLFW_KEY_RIGHT -> pixelGridAnimation.addOffset(moveAmount, 0);
                default -> {
                    return super.keyPressed(input);
                }
            }

            hudOption.setValue(pixelGridAnimation);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            hudOption.setValue(pixelGridAnimation);
        }
        super.onClose();
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {

    }

    @Override
    protected void renderBackgroundLayer(GuiGraphicsExtractor context, float delta) {
        if (this.minecraft.level == null) {
        }
    }
}
