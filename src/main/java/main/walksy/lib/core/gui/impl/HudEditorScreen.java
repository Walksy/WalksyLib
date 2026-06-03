package main.walksy.lib.core.gui.impl;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.type.PixelGrid;
import main.walksy.lib.core.config.local.options.type.PixelGridAnimation;
import main.walksy.lib.core.gui.Graphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class HudEditorScreen extends BaseScreen {

    private static final Identifier CROSSHAIR_TEXTURE = Identifier.withDefaultNamespace("hud/crosshair");
    private static final double MOVE_AMOUNT = 0.5;

    private final Option<?> hudOption;
    private boolean dragging = false;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;

    public HudEditorScreen(final Screen parent, final Option<?> option) {
        super("HudEditor", parent);
        this.hudOption = option;
    }

    @Override
    protected void extract(final Graphics graphics, final int mouseX, final int mouseY) {
        final GuiGraphicsExtractor extractor = graphics.extractor();
        extractor.centeredText(this.font, "Currently Editing: " + this.hudOption.getName(), this.width / 2, 10, -1);
        extractor.centeredText(this.font, "(ESC to leave)", this.width / 2, 20, Color.GRAY.getRGB());

        if (!(this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return;
        }

        if (this.minecraft.level == null) {
            extractor.fill(0, 0, this.width, this.height, Color.BLACK.getRGB());
            extractor.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    CROSSHAIR_TEXTURE,
                    (extractor.guiWidth() - 15) / 2,
                    (extractor.guiHeight() - 15) / 2,
                    15,
                    15
            );
        }
        pixelGridAnimation.render(extractor, false);
        if (!this.dragging) {
            final Vec2 pos = pixelGridAnimation.getAbsolutePosition();
            final float size = pixelGridAnimation.getSize();
            extractor.pose().pushMatrix();
            extractor.pose().scale(size, size);
            graphics.renderGridOutline(
                    pixelGridAnimation.getCurrentFrame(),
                    (int) Math.round(pos.x / size),
                    (int) Math.round(pos.y / size),
                    1,
                    0
            );
            extractor.pose().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent click, final boolean doubled) {
        if (!(this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseClicked(click, doubled);
        }
        final double mouseX = click.x();
        final double mouseY = click.y();
        final AnimBounds bounds = this.computeBounds(pixelGridAnimation);
        final double rawX = bounds.baseX() + pixelGridAnimation.getOffsetX();
        final double rawY = bounds.baseY() + pixelGridAnimation.getOffsetY();
        if (mouseX >= rawX && mouseX <= rawX + bounds.renderedW() && mouseY >= rawY && mouseY <= rawY + bounds.renderedH()) {
            this.dragging = true;
            this.dragOffsetX = mouseX - rawX;
            this.dragOffsetY = mouseY - rawY;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent click, final double oX, final double oY) {
        if (!this.dragging || !(this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseDragged(click, oX, oY);
        }
        this.applyOffset(pixelGridAnimation, click.x(), click.y());
        return true;
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent click) {
        if (!this.dragging) {
            return super.mouseReleased(click);
        }
        this.dragging = false;
        if (!(this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.mouseReleased(click);
        }
        this.applyOffset(pixelGridAnimation, click.x(), click.y());
        this.hudOption.setValue(pixelGridAnimation);
        return true;
    }

    @Override
    public boolean keyPressed(final KeyEvent input) {
        if (!(this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation)) {
            return super.keyPressed(input);
        }
        switch (input.key()) {
            case GLFW.GLFW_KEY_UP    -> pixelGridAnimation.addOffset(0, -MOVE_AMOUNT);
            case GLFW.GLFW_KEY_DOWN  -> pixelGridAnimation.addOffset(0, MOVE_AMOUNT);
            case GLFW.GLFW_KEY_LEFT  -> pixelGridAnimation.addOffset(-MOVE_AMOUNT, 0);
            case GLFW.GLFW_KEY_RIGHT -> pixelGridAnimation.addOffset(MOVE_AMOUNT, 0);
            default -> {
                return super.keyPressed(input);
            }
        }
        this.hudOption.setValue(pixelGridAnimation);
        return true;
    }

    @Override
    public void onClose() {
        if (this.hudOption.getValue() instanceof PixelGridAnimation pixelGridAnimation) {
            this.hudOption.setValue(pixelGridAnimation);
        }
        super.onClose();
    }

    @Override
    protected void extractBlurredBackground(final GuiGraphicsExtractor graphics) {}

    private void applyOffset(final PixelGridAnimation anim, final double mouseX, final double mouseY) {
        final AnimBounds bounds = this.computeBounds(anim);
        final double offsetX = Math.round((mouseX - this.dragOffsetX - bounds.baseX()) * 2.0) / 2.0;
        final double offsetY = Math.round((mouseY - this.dragOffsetY - bounds.baseY()) * 2.0) / 2.0;
        anim.setOffset(offsetX, offsetY);
    }

    private AnimBounds computeBounds(final PixelGridAnimation anim) {
        final PixelGrid frame = anim.getCurrentFrame();
        final float size = anim.getSize();
        final int renderedW = Math.round((frame == null ? 0 : frame.getWidth()) * size);
        final int renderedH = Math.round((frame == null ? 0 : frame.getHeight()) * size);
        final int baseX = (this.minecraft.getWindow().getGuiScaledWidth() - renderedW) / 2;
        final int baseY = (this.minecraft.getWindow().getGuiScaledHeight() - renderedH) / 2;
        return new AnimBounds(baseX, baseY, renderedW, renderedH);
    }

    private record AnimBounds(int baseX, int baseY, int renderedW, int renderedH) {}
}
