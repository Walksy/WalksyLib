package main.walksy.lib.core.gui.widgets;

import main.walksy.lib.core.config.local.Option;
import main.walksy.lib.core.config.local.options.groups.OptionGroup;
import main.walksy.lib.core.gui.impl.WalksyLibConfigScreen;
import main.walksy.lib.core.gui.Graphics;
import main.walksy.lib.core.utils.MainColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class EnumOptionWidget<E extends Enum<E>> extends OptionWidget {

    private final Option<E> option;
    private int maxWidth;

    public EnumOptionWidget(final OptionGroup parent, final WalksyLibConfigScreen screen, final int x, final int y, final int width, final int height, final Option<E> option) {
        super(parent, screen, option, x, y, width, height, option.getName());
        this.option = option;
        this.recalc();
    }

    @Override
    public void draw(final GuiGraphicsExtractor context, final int mouseX, final int mouseY, final float delta) {
        final int rectX = this.getWidth() - 30 - this.maxWidth;
        final int rectY = this.getY() + 3;
        final int rectWidth = this.maxWidth + 38;
        final int rectHeight = this.getHeight() - 6;

        final boolean hovered = this.isHoveringEnum(mouseX, mouseY);

        new Graphics(context).fillRoundedRectOutline(rectX - 1, rectY - 1, rectWidth + 2, rectHeight + 2, 2, 1,
                hovered ? MainColors.OUTLINE_BLACK.getRGB() : new Color(0, 0, 0, 100).getRGB());

        new Graphics(context).fillRoundedRectOutline(rectX, rectY, rectWidth, rectHeight, 2, 1,
                hovered ? MainColors.OUTLINE_WHITE_HOVERED.getRGB() : MainColors.OUTLINE_WHITE.getRGB());

        final Font textRenderer = Minecraft.getInstance().font;
        final int textPos = (int) (rectY + (rectHeight / 2f) - textRenderer.lineHeight / 2f) + 1;

        context.centeredText(
                textRenderer,
                this.option.getValue().name(),
                (int) (rectX + rectWidth / 2f),
                textPos,
                hovered ? -1 : Color.LIGHT_GRAY.getRGB()
        );
    }

    @Override
    public void onMouseClick(final MouseButtonEvent click, final boolean doubled) {
        super.onMouseClick(click, doubled);

        if (this.isHoveringEnum(click.x(), click.y())) {
            final E[] constants = this.option.getValue().getDeclaringClass().getEnumConstants();
            final int currentIndex = this.option.getValue().ordinal();
            final int nextIndex;

            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                nextIndex = (currentIndex - 1 + constants.length) % constants.length;
            } else {
                nextIndex = (currentIndex + 1) % constants.length;
            }

            this.option.setValue(constants[nextIndex]);
        }
    }

    @Override
    public void onWidgetUpdate() {}

    @Override
    public boolean isHovered() {
        return false;
    }

    void recalc() {
        int longestWidth = 0;
        for (final E constant : this.option.getValue().getDeclaringClass().getEnumConstants()) {
            final int width = this.screen.getFont().width(constant.name());
            if (width > longestWidth) longestWidth = width;
        }
        this.maxWidth = longestWidth;
    }

    private boolean isHoveringEnum(final double mouseX, final double mouseY) {
        final int rectX = this.getWidth() - 30 - this.maxWidth;
        final int rectY = this.getY() + 3;
        final int rectWidth = this.maxWidth + 38;
        final int rectHeight = this.getHeight() - 6;

        return (mouseX >= rectX && mouseX <= rectX + rectWidth &&
                mouseY >= rectY && mouseY <= rectY + rectHeight);
    }
}
